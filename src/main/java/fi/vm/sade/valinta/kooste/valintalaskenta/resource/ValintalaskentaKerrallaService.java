package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

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
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class ValintalaskentaKerrallaService {
    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaKerrallaService.class);

    @Autowired
    private ValintalaskentaKerrallaRouteValvomo valintalaskentaValvomo;
    @Autowired
    private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    @Autowired
    private OhjausparametritAsyncResource ohjausparametritAsyncResource;
    @Autowired
    private ValintalaskentaKerrallaRoute valintalaskentaRoute;

    public ValintalaskentaKerrallaService() {
    }

    void kaynnistaLaskenta(
            final LaskentaTyyppi tyyppi,
            final String hakuOid,
            final Maski maski,
            final BiConsumer<Collection<HakukohdeJaOrganisaatio>, Consumer<String>> seurantaTunnus,
            final boolean erillishaku,
            final boolean valintaryhmalaskenta,
            final Integer valinnanvaihe,
            final Boolean valintakoelaskenta,
            final AsyncResponse asyncResponse) {
        if (StringUtils.isBlank(hakuOid)) {
            LOG.error("HakuOid on pakollinen");
            throw new RuntimeException("HakuOid on pakollinen");
        }
        // maskilla kaynnistettaessa luodaan aina uusi laskenta
        if (!maski.isMask()) { // muuten tarkistetaan onko laskenta jo olemassa
            // Kaynnissa oleva laskenta koko haulle
            final Optional<Laskenta> ajossaOlevaLaskentaHaulle = valintalaskentaValvomo
                    .ajossaOlevatLaskennat()
                    .stream()
                    // Tama haku ... ja koko haun laskennasta on kyse
                    .filter(l -> hakuOid.equals(l.getHakuOid())
                            && !l.isOsittainenLaskenta())
                    .findFirst();
            if (ajossaOlevaLaskentaHaulle.isPresent()) {
                // palautetaan seurattavaksi ajossa olevan hakukohteen seurantatunnus
                final String uuid = ajossaOlevaLaskentaHaulle.get().getUuid();
                LOG.warn("Laskenta on jo kaynnissa haulle {} joten palautetaan seurantatunnus({}) ajossa olevaan hakuun",
                        hakuOid, uuid);
                asyncResponse.resume(Response
                        .ok(Vastaus.uudelleenOhjaus(uuid))
                        .build());
                return;
            }
        }
        LOG.info("Aloitetaan laskenta haulle {}", hakuOid);
        haunHakukohteet(
                hakuOid,
                (List<HakukohdeJaOrganisaatio> haunHakukohteetOids) -> {
                    kasitteleHaunkohteetOids(
                            haunHakukohteetOids,
                            tyyppi,
                            hakuOid,
                            maski,
                            seurantaTunnus,
                            erillishaku,
                            valintaryhmalaskenta,
                            valinnanvaihe,
                            valintakoelaskenta,
                            asyncResponse);
                },
                (Throwable poikkeus) -> asyncResponse.resume(errorResponce(poikkeus.getMessage())));
    }


    void haunHakukohteet(
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

    void kasitteleHakukohdeViitteet(
            final List<HakukohdeViiteDTO> hakukohdeViitteet,
            final String hakuOid, Consumer<List<HakukohdeJaOrganisaatio>> hakukohdeJaOrganisaatioKasittelijaCallback,
            final Consumer<Throwable> failureCallback) {
        LOG.info("Tarkastellaan hakukohdeviitteita haulle {}", hakuOid);

        if (hakukohdeViitteet == null || hakukohdeViitteet.isEmpty()) {
            LOG.error("Valintaperusteet palautti tyhjat hakukohdeviitteet haulle {}!", hakuOid);
            throw new NullPointerException("Valintaperusteet palautti tyhjat hakukohdeviitteet!");
        }
        final List<HakukohdeJaOrganisaatio> haunHakukohdeOidit = hakukohdeViitteet
                .stream()
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
            failureCallback.accept(
                    new RuntimeException("Haulla " + hakuOid + " ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?"));
        } else {
            hakukohdeJaOrganisaatioKasittelijaCallback.accept(haunHakukohdeOidit);
        }
    }

    public void kasitteleHaunkohteetOids(
            final Collection<HakukohdeJaOrganisaatio> haunHakukohteetOids,
            final LaskentaTyyppi tyyppi,
            final String hakuOid,
            final Maski maski,
            final BiConsumer<Collection<HakukohdeJaOrganisaatio>, Consumer<String>> seurantaTunnus,
            final boolean erillishaku,
            final boolean valintaryhmalaskenta,
            final Integer valinnanvaihe,
            final Boolean valintakoelaskenta,
            final AsyncResponse asyncResponse) {
        Collection<HakukohdeJaOrganisaatio> oids;
        if (maski.isMask()) {
            oids = maski.maskaa(haunHakukohteetOids);
            if (oids.isEmpty()) {
                throw new RuntimeException(
                        "Hakukohdemaskauksen jalkeen haulla ei ole hakukohteita! Ei voida aloittaa laskentaa hakukohteettomasti.");
            }
        } else {
            oids = haunHakukohteetOids;
        }
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
                                                tyyppi));
                                asyncResponse.resume(Response
                                        .ok(Vastaus.uudelleenOhjaus(uuid))
                                        .build());
                            });
                },
                poikkeus -> {
                    LOG.error("Ohjausparametrien luku epäonnistui: {} {}",
                            poikkeus.getMessage(), Arrays.toString(poikkeus.getStackTrace()));
                    asyncResponse.resume(errorResponce(poikkeus.getMessage()));
                });
    }

    public void kasitteleLaskennanAloitus(
            final String uuid,
            final AsyncResponse asyncResponse,
            final Consumer<String> laskennanAloitus) {
        if (uuid == null) {
            LOG.error("Laskentaa ei saatu luotua!");
            asyncResponse.resume(errorResponce("Laskentaa ei saatu luotua!"));
            throw new RuntimeException("Laskentaa ei saatu luotua!");
        }
        try {
            laskennanAloitus.accept(uuid);
        } catch (Throwable e) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe: {}", e.getMessage());
            asyncResponse.resume(errorResponce("Odottamaton virhe laskennan kaynnistamisessa! " + e.getMessage()));
            throw e;
        }
    }

    public void kasitteleKokoPaska(
            final Collection<HakukohdeJaOrganisaatio> hakukohdeOids,
            final Consumer<String> laskennanAloitus,
            final AsyncResponse asyncResponse,
            final LaskentaSeurantaAsyncResource seurantaAsyncResource,
            final String hakuOid, LaskentaTyyppi tyyppi,
            final Boolean erillishaku,
            final Integer valinnanvaihe,
            final Boolean valintakoelaskenta) {
        final List<HakukohdeDto> hakukohdeDtos = hakukohdeOids
                .stream()
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
        if (hakukohdeDtos.isEmpty() || hakukohdeDtos.size() == 0) {
            LOG.error("Laskentaa ei voida aloittaa hakukohteille joilta puuttuu organisaatio!");
            asyncResponse.resume(errorResponce("Laskentaa ei voida aloittaa hakukohteille joilta puuttuu organisaatio!"));
            throw new RuntimeException("Laskentaa ei voida aloittaa hakukohteille joilta puuttuu organisaatio!");
        } else {
            if (hakukohdeDtos.size() < hakukohdeOids.size()) {
                LOG.warn("Hakukohteita puuttuvien organisaatio-oidien vuoksi filtteroinnin jalkeen {}/{}!", hakukohdeDtos.size(), hakukohdeOids.size());
            } else {
                LOG.info("Hakukohteita filtteroinnin jalkeen {}/{}!", hakukohdeDtos.size(), hakukohdeOids.size());
            }
        }
        seurantaAsyncResource.luoLaskenta(
                hakuOid,
                tyyppi,
                erillishaku,
                valinnanvaihe,
                valintakoelaskenta,
                hakukohdeDtos,
                (String uuid) -> kasitteleLaskennanAloitus(uuid, asyncResponse, laskennanAloitus),
                (Throwable poikkeus) -> {
                    LOG.error("Seurannasta uuden laskennan haku paatyi virheeseen: {}", poikkeus.getMessage());
                    asyncResponse.resume(errorResponce(poikkeus.getMessage()));
                });
    }

    public void kasitteleKaynnistaLaskentaUudelleen(
            final LaskentaDto laskenta,
            final AsyncResponse asyncResponse) {
        try {
            final List<HakukohdeJaOrganisaatio> maski = laskenta
                    .getHakukohteet()
                    .stream()
                    .filter(h -> !HakukohdeTila.VALMIS.equals(h.getTila()))
                    .map(h -> new HakukohdeJaOrganisaatio(h.getHakukohdeOid(), h.getOrganisaatioOid()))
                    .collect(Collectors.toList());
            kaynnistaLaskenta(
                    laskenta.getTyyppi(),
                    laskenta.getHakuOid(),
                    new Maski(
                            true,
                            maski.stream()
                                    .map(hk -> hk.getHakukohdeOid())
                                    .collect(Collectors.toList())),
                    (Collection<HakukohdeJaOrganisaatio> hakuJaHakukohteet, Consumer<String> laskennanAloitus) -> {
                        laskennanAloitus.accept(laskenta.getUuid());
                    },
                    Boolean.TRUE.equals(laskenta.isErillishaku()),
                    LaskentaTyyppi.VALINTARYHMA.equals(laskenta.getTyyppi()),
                    laskenta.getValinnanvaihe(),
                    laskenta.getValintakoelaskenta(),
                    asyncResponse);
        } catch (Throwable e) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe: {}", e.getMessage());
            asyncResponse.resume(errorResponce("Odottamaton virhe laskennan kaynnistamisessa! " + e.getMessage()));
            throw e;
        }
    }

    private Response errorResponce(final String errorMessage){
        return Response.serverError().entity(errorMessage).build();
    }
}