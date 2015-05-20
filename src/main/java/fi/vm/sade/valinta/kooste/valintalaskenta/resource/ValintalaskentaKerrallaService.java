package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class ValintalaskentaKerrallaService {
    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaKerrallaService.class);

    @Autowired
    private ValintalaskentaKerrallaRouteValvomo valintalaskentaValvomo;
    @Autowired
    private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    @Autowired
    private ValintalaskentaKerrallaRoute valintalaskentaRoute;
    @Autowired
    private LaskentaSeurantaAsyncResource seurantaAsyncResource;

    public ValintalaskentaKerrallaService() {}

    public void kaynnistaLaskentaHaulle(LaskentaParams laskentaParams, Consumer<Response> callback) {
        kaynnistaLaskenta(
                laskentaParams.getHakuOid(),
                laskentaParams.getMaski(),
                (Collection<HakukohdeJaOrganisaatio> hakukohdeOids, Consumer<String> laskennanAloitus) -> luoLaskenta(
                        hakukohdeOids,
                        laskennanAloitus,
                        laskentaParams,
                        callback
                ),
                callback
        );
    }

    public void kaynnistaLaskentaUudelleen(final String uuid, final Consumer<Response> callbackResponse) {
        try {
            final Laskenta l = valintalaskentaValvomo.haeLaskenta(uuid);
            if (l != null && !l.isValmis()) {
                LOG.warn("Laskenta {} on viela ajossa, joten palautetaan linkki siihen.", uuid);
                callbackResponse.accept(redirectResponse(uuid));
            }
            seurantaAsyncResource.resetoiTilat(
                    uuid,
                    (LaskentaDto laskenta) -> kaynnistaLaskenta(
                            laskenta.getHakuOid(),
                            luoMaskiLaskennanPohjalta(laskenta),
                            (Collection<HakukohdeJaOrganisaatio> hakuJaHakukohteet, Consumer<String> laskennanAloitus) -> laskennanAloitus.accept(laskenta.getUuid()),
                            callbackResponse),
                    (Throwable t) -> {
                        LOG.error("Laskennan uudelleenajo epäonnistui. Uuid: " + uuid , t);
                        callbackResponse.accept(errorResponse("Uudelleen ajo laskennalle heitti poikkeuksen!"));
                    });
        } catch (Throwable t) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe", t);
            callbackResponse.accept(errorResponse("Odottamaton virhe laskennan kaynnistamisessa! " + t.getMessage()));
            throw t;
        }
    }

    private void kaynnistaLaskenta(
            final String hakuOid,
            final Maski maski,
            final BiConsumer<Collection<HakukohdeJaOrganisaatio>, Consumer<String>> seurantaTunnus,
            final Consumer<Response> callbackResponse) {
        if (StringUtils.isBlank(hakuOid)) {
            LOG.error("HakuOid on pakollinen");
            throw new RuntimeException("HakuOid on pakollinen");
        }
        // maskilla kaynnistettaessa luodaan aina uusi laskenta
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
                hakuOid,
                (List<HakukohdeJaOrganisaatio> haunHakukohteetOids) -> kasitteleHaunkohteetOids(
                            haunHakukohteetOids,
                            maski,
                            seurantaTunnus,
                            callbackResponse),
                (Throwable poikkeus) -> callbackResponse.accept(errorResponse(poikkeus.getMessage())));
    }

    private Optional<Laskenta> haeAjossaOlevaLaskentaHaulle(final String hakuOid) {
        return valintalaskentaValvomo
                .ajossaOlevatLaskennat()
                .stream()
                        // Tama haku ... ja koko haun laskennasta on kyse
                .filter(l -> hakuOid.equals(l.getHakuOid()) && !l.isOsittainenLaskenta())
                .findFirst();
    }

    private void haunHakukohteet(
            final String hakuOid,
            final Consumer<List<HakukohdeJaOrganisaatio>> hakukohdeJaOrganisaatioKasittelijaCallback,
            final Consumer<Throwable> failureCallback) {
        if (StringUtils.isBlank(hakuOid)) {
            LOG.error("Yritettiin hakea hakukohteita ilman hakuOidia!");
            throw new RuntimeException("Yritettiin hakea hakukohteita ilman hakuOidia!");
        }
        valintaperusteetAsyncResource.haunHakukohteet(
                hakuOid,
                (List<HakukohdeViiteDTO> hakukohdeViitteet) -> kasitteleHakukohdeViitteet(hakukohdeViitteet, hakuOid, hakukohdeJaOrganisaatioKasittelijaCallback, failureCallback),
                (Throwable poikkeus) -> failureCallback.accept(poikkeus));
    }

    private void kasitteleHakukohdeViitteet(
            final List<HakukohdeViiteDTO> hakukohdeViitteet,
            final String hakuOid, Consumer<List<HakukohdeJaOrganisaatio>> hakukohdeJaOrganisaatioKasittelijaCallback,
            final Consumer<Throwable> failureCallback) {
        LOG.info("Tarkastellaan hakukohdeviitteita haulle {}", hakuOid);

        if (hakukohdeViitteet == null || hakukohdeViitteet.isEmpty()) {
            LOG.error("Valintaperusteet palautti tyhjat hakukohdeviitteet haulle {}!", hakuOid);
            throw new NullPointerException("Valintaperusteet palautti tyhjat hakukohdeviitteet!");
        }
        final List<HakukohdeJaOrganisaatio> haunHakukohdeOids = hakukohdeViitteet.stream()
                .filter(Objects::nonNull)
                .filter(hakukohdeOid -> hakukohdeOid.getOid() != null)
                .filter(hakukohdeOid -> hakukohdeOid.getTila().equals("JULKAISTU"))
                .map(u -> new HakukohdeJaOrganisaatio(u.getOid(), u.getTarjoajaOid()))
                .collect(Collectors.toList());
        if (haunHakukohdeOids.isEmpty()) {
            LOG.error("Haulla {} ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?", hakuOid);
            failureCallback.accept(new RuntimeException("Haulla " + hakuOid + " ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?"));
        } else {
            hakukohdeJaOrganisaatioKasittelijaCallback.accept(haunHakukohdeOids);
        }
    }

    private void kasitteleHaunkohteetOids(
            final Collection<HakukohdeJaOrganisaatio> haunHakukohteetOids,
            final Maski maski,
            final BiConsumer<Collection<HakukohdeJaOrganisaatio>, Consumer<String>> seurantaTunnus,
            final Consumer<Response> callbackResponse
    ) {
        Collection<HakukohdeJaOrganisaatio> oids;
        if (maski.isMask()) {
            oids = maski.maskaa(haunHakukohteetOids);
            if (oids.isEmpty()) {
                throw new RuntimeException("Hakukohdemaskauksen jalkeen haulla ei ole hakukohteita! Ei voida aloittaa laskentaa hakukohteettomasti.");
            }
        } else {
            oids = haunHakukohteetOids;
        }
        seurantaTunnus.accept(oids, (String uuid) -> {
            valintalaskentaRoute.workAvailable();
            callbackResponse.accept(redirectResponse(uuid));
        });
    }

    private void kasitteleLaskennanAloitus(
            final String uuid,
            final Consumer<String> laskennanAloitus,
            final Consumer<Response> callbackResponse) {
        if (uuid == null) {
            LOG.error("Laskentaa ei saatu luotua!");
            callbackResponse.accept(errorResponse("Laskentaa ei saatu luotua!"));
            throw new RuntimeException("Laskentaa ei saatu luotua!");
        }
        try {
            laskennanAloitus.accept(uuid);
        } catch (Throwable e) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe", e);
            callbackResponse.accept(errorResponse("Odottamaton virhe laskennan kaynnistamisessa! " + e.getMessage()));
            throw e;
        }
    }

    private void luoLaskenta(Collection<HakukohdeJaOrganisaatio> hakukohdeData, Consumer<String> laskennanAloitus, LaskentaParams laskentaParams, Consumer<Response> callbackResponse) {
        final List<HakukohdeDto> hakukohdeDtos = filterAndMapTohakukohdeDto(hakukohdeData);
        validateHakukohdeDtos(hakukohdeData, hakukohdeDtos, callbackResponse);

        seurantaAsyncResource.luoLaskenta(
                laskentaParams,
                hakukohdeDtos,
                (String uuid) -> kasitteleLaskennanAloitus(uuid, laskennanAloitus, callbackResponse),
                (Throwable t) -> {
                    LOG.error("Seurannasta uuden laskennan haku paatyi virheeseen", t);
                    callbackResponse.accept(errorResponse(t.getMessage()));
                });
    }

    private void validateHakukohdeDtos(Collection<HakukohdeJaOrganisaatio> hakukohdeData, List<HakukohdeDto> hakukohdeDtos, Consumer<Response> callbackResponse) {
        if (hakukohdeDtos.isEmpty()) {
            LOG.error("Laskentaa ei voida aloittaa hakukohteille joilta puuttuu organisaatio!");
            callbackResponse.accept(errorResponse("Laskentaa ei voida aloittaa hakukohteille joilta puuttuu organisaatio!"));
            throw new RuntimeException("Laskentaa ei voida aloittaa hakukohteille joilta puuttuu organisaatio!");
        } else {
            if (hakukohdeDtos.size() < hakukohdeData.size()) {
                LOG.warn("Hakukohteita puuttuvien organisaatio-oidien vuoksi filtteroinnin jalkeen {}/{}!", hakukohdeDtos.size(), hakukohdeData.size());
            } else {
                LOG.info("Hakukohteita filtteroinnin jalkeen {}/{}!", hakukohdeDtos.size(), hakukohdeData.size());
            }
        }
    }

    private Response redirectResponse(final String target) {
        return Response.ok(Vastaus.uudelleenOhjaus(target)).build();
    }

    private Response errorResponse(final String errorMessage){
        return Response.serverError().entity(errorMessage).build();
    }

    private Maski luoMaskiLaskennanPohjalta(final LaskentaDto laskenta) {
        final List<String> hakukohdeOids = laskenta.getHakukohteet().stream()
                .filter(h -> !HakukohdeTila.VALMIS.equals(h.getTila()))
                .map(HakukohdeDto::getHakukohdeOid)
                .collect(Collectors.toList());
        return new Maski(true, hakukohdeOids);
    }

    private List<HakukohdeDto> filterAndMapTohakukohdeDto(Collection<HakukohdeJaOrganisaatio> hakukohdeData) {
        return hakukohdeData.stream()
                .filter(hk -> {
                    if (hk == null) {
                        LOG.error("Null referenssi hakukohdeOidsien joukossa laskentaa luotaessa!");
                        return false;
                    }
                    if (hk.getHakukohdeOid() == null) {
                        LOG.error("HakukohdeOid oli null laskentaa luotaessa! OrganisaatioOid == {}, joten hakukohde ohitetaan!",
                                hk.getOrganisaatioOid());
                        return false;
                    }
                    if (hk.getOrganisaatioOid() == null) {
                        LOG.error("OrganisaatioOid oli null laskentaa luotaessa! HakukohdeOid == {}, joten hakukohde ohitetaan!",
                                hk.getHakukohdeOid());
                        return false;
                    }
                    return true;
                })
                .map(hk -> new HakukohdeDto(hk.getHakukohdeOid(), hk.getOrganisaatioOid()))
                .collect(Collectors.toList());
    }
}
