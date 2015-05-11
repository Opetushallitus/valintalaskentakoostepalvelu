package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.seuranta.dto.*;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Service
public class LaskentaKaynnistin {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaKaynnistin.class);

    @Autowired
    private ValintalaskentaKerrallaRouteValvomo valintalaskentaValvomo;
    @Autowired
    private OhjausparametritAsyncResource ohjausparametritAsyncResource;
    @Autowired
    private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    @Autowired
    private LaskentaSeurantaAsyncResource seurantaAsyncResource;
    @Autowired
    private ValintalaskentaKerrallaRoute valintalaskentaRoute;


    public void haeJaKaynnistaLaskenta(String uuid, final Consumer<Response> callbackResponse) {
        seurantaAsyncResource.laskenta(
                uuid,
                (LaskentaDto laskenta) -> kaynnistaLaskenta(laskenta, callbackResponse),
                (Throwable t) -> {
                    LOG.error("Laskennan haku ja käyynistys epäonnistui {}:\r\n{}", t.getMessage(), Arrays.toString(t.getStackTrace()));
                    callbackResponse.accept(errorResponse("Uudelleen ajo laskennalle heitti poikkeuksen!"));
                }
        );
    }

    private Response errorResponse(final String errorMessage){
        return Response.serverError().entity(errorMessage).build();
    }

    private void kaynnistaLaskenta(LaskentaDto laskenta, final Consumer<Response> callbackResponse) {
        String hakuOid = laskenta.getHakuOid();
        if (StringUtils.isBlank(hakuOid)) {
            LOG.error("HakuOid on pakollinen");
            throw new RuntimeException("HakuOid on pakollinen");
        }

        // maskilla kaynnistettaessa luodaan aina uusi laskenta
        Maski maski = luoMaskiLaskennanPohjalta(laskenta);
        if (!maski.isMask()) { // muuten tarkistetaan onko laskenta jo olemassa
            // Kaynnissa oleva laskenta koko haulle
            final Optional<Laskenta> ajossaOlevaLaskentaHaulle = haeAjossaOlevaLaskentaHaulle(hakuOid);
            if (ajossaOlevaLaskentaHaulle.isPresent()) {
                // palautetaan seurattavaksi ajossa olevan hakukohteen seurantatunnus
                final String uuid = ajossaOlevaLaskentaHaulle.get().getUuid();
                LOG.warn("Laskenta on jo kaynnissa haulle {} joten palautetaan seurantatunnus({}) ajossa olevaan hakuun", hakuOid, uuid);
                callbackResponse.accept(redirectResponse(uuid));
                return;
            }
        }
        LOG.info("Aloitetaan laskenta haulle {}", hakuOid);
        haunHakukohteet(
                laskenta,
                maski,
                callbackResponse,
                (Throwable poikkeus) -> callbackResponse.accept(errorResponse(poikkeus.getMessage()))
        );
    }

    private void kasitteleHaunkohteetOids(
            final LaskentaDto laskenta,
            final Collection<HakukohdeJaOrganisaatio> haunHakukohteetOids,
            final Maski maski,
            final Consumer<Response> callbackResponse
    ) {
        LaskentaTyyppi tyyppi = laskenta.getTyyppi();
        String hakuOid = laskenta.getHakuOid();
        Boolean erillishaku = laskenta.isErillishaku();
        boolean valintaryhmalaskenta = LaskentaTyyppi.VALINTARYHMA.equals(tyyppi);
        Integer valinnanvaihe = laskenta.getValinnanvaihe();
        Boolean valintakoelaskenta = laskenta.getValintakoelaskenta();

        Collection<HakukohdeJaOrganisaatio> oids = maski.isMask() ? maski.maskaa(haunHakukohteetOids) : haunHakukohteetOids;
        if (oids.isEmpty()) {
            throw new RuntimeException("Hakukohdemaskauksen jalkeen haulla ei ole hakukohteita! Ei voida aloittaa laskentaa ilman hakukohteita.");
        }

        BiConsumer<Collection<HakukohdeJaOrganisaatio>, Consumer<String>> seurantaTunnus = (Collection<HakukohdeJaOrganisaatio> hakuJaHakukohteet, Consumer<String> laskennanAloitus) ->  laskennanAloitus.accept(laskenta.getUuid());
        ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid, parametrit -> {
                    seurantaTunnus.accept(
                            oids,
                            (String uuid) -> {
                                valintalaskentaRoute.suoritaValintalaskentaKerralla(
                                        parametrit,
                                        new LaskentaAloitus(
                                                uuid,
                                                hakuOid,
                                                erillishaku,
                                                maski.isMask(),
                                                valintaryhmalaskenta,
                                                valinnanvaihe,
                                                valintakoelaskenta,
                                                oids,
                                                tyyppi
                                        )
                                );
                                callbackResponse.accept(redirectResponse(uuid));
                            });
                },
                poikkeus -> {
                    LOG.error("Ohjausparametrien luku epäonnistui: {} {}", poikkeus.getMessage(), Arrays.toString(poikkeus.getStackTrace()));
                    callbackResponse.accept(errorResponse(poikkeus.getMessage()));
                });
    }

    private Optional<Laskenta> haeAjossaOlevaLaskentaHaulle(final String hakuOid) {
        return valintalaskentaValvomo.ajossaOlevatLaskennat().stream()
                .filter(l -> hakuOid.equals(l.getHakuOid()) && !l.isOsittainenLaskenta())
                .findFirst();
    }

    private void haunHakukohteet(
            final LaskentaDto laskenta,
            final Maski maski,
            final Consumer<Response> callbackResponse,
            final Consumer<Throwable> failureCallback
    ) {
        String hakuOid = laskenta.getHakuOid();
        if (StringUtils.isBlank(hakuOid)) {
            LOG.error("Yritettiin hakea hakukohteita ilman hakuOidia!");
            throw new RuntimeException("Yritettiin hakea hakukohteita ilman hakuOidia!");
        }
        valintaperusteetAsyncResource.haunHakukohteet(
                hakuOid,
                (List<HakukohdeViiteDTO> hakukohdeViitteet) -> kasitteleHakukohdeViitteet(
                        laskenta,
                        maski,
                        hakukohdeViitteet,
                        callbackResponse,
                        failureCallback
                ),
                failureCallback::accept
        );
    }

    private void kasitteleHakukohdeViitteet(
            final LaskentaDto laskenta,
            final Maski maski,
            final List<HakukohdeViiteDTO> hakukohdeViitteet,
            final Consumer<Response> callbackResponse,
            final Consumer<Throwable> failureCallback
    ) {
        String hakuOid = laskenta.getHakuOid();
        LOG.info("Tarkastellaan hakukohdeviitteita haulle {}", hakuOid);

        if (hakukohdeViitteet == null || hakukohdeViitteet.isEmpty()) {
            LOG.error("Valintaperusteet palautti tyhjat hakukohdeviitteet haulle {}!", hakuOid);
            throw new NullPointerException("Valintaperusteet palautti tyhjat hakukohdeviitteet!");
        }
        final List<HakukohdeJaOrganisaatio> haunHakukohdeOidit = hakukohdeViitteet.stream()
                .filter(Objects::nonNull)
                .filter(h -> {
                    if (h == null) {
                        LOG.error("nonNull filteri ei toimi!");
                        return false;
                    }
                    if (h.getOid() == null) {
                        LOG.error("Hakukohdeviitteen oid oli null haussa {}", hakuOid);
                        return false;
                    }
                    if (h.getTila() == null) {
                        LOG.error("Hakukohdeviitteen tila oli null hakukohteelle {}", h.getOid());
                        return false;
                    }
                    if (!"JULKAISTU".equals(h.getTila())) {
                        LOG.warn("Ohitetaan hakukohde {} koska sen tila on {}.", h.getOid(), h.getTila());
                        return false;
                    }
                    return true;
                })
                .map(u -> new HakukohdeJaOrganisaatio(u.getOid(), u.getTarjoajaOid()))
                .collect(Collectors.toList());
        if (haunHakukohdeOidit.isEmpty()) {
            LOG.error("Haulla {} ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?", hakuOid);
            failureCallback.accept(new RuntimeException("Haulla " + hakuOid + " ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?"));
        } else {
            kasitteleHaunkohteetOids(laskenta, haunHakukohdeOidit, maski, callbackResponse);
        }
    }

    private Maski luoMaskiLaskennanPohjalta(final LaskentaDto laskenta) {
        final List<HakukohdeJaOrganisaatio> hakukohdeMaski = laskenta.getHakukohteet().stream()
                .filter(h -> !HakukohdeTila.VALMIS.equals(h.getTila()))
                .map(h -> new HakukohdeJaOrganisaatio(h.getHakukohdeOid(), h.getOrganisaatioOid()))
                .collect(Collectors.toList());
        return new Maski(
                true,
                hakukohdeMaski.stream()
                        .map(HakukohdeJaOrganisaatio::getHakukohdeOid)
                        .collect(Collectors.toList())
        );
    }

    private Response redirectResponse(final String target) {
        return Response.ok(Vastaus.uudelleenOhjaus(target)).build();
    }
}
