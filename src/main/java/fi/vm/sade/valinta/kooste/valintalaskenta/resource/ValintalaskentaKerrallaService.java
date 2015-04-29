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
            LaskentaTyyppi tyyppi,
            String hakuOid,
            Maski maski,
            BiConsumer<Collection<HakukohdeJaOrganisaatio>, Consumer<String>> seurantaTunnus,
            boolean erillishaku, boolean valintaryhmalaskenta,
            Integer valinnanvaihe, Boolean valintakoelaskenta,
            AsyncResponse asyncResponse) {
        if (StringUtils.isBlank(hakuOid)) {
            LOG.error("HakuOid on pakollinen");
            throw new RuntimeException("HakuOid on pakollinen");
        }
        // maskilla kaynnistettaessa luodaan aina uusi laskenta
        if (!maski.isMask()) { // muuten tarkistetaan onko laskenta jo olemassa
            // Kaynnissa oleva laskenta koko haulle
            Optional<Laskenta> ajossaOlevaLaskentaHaulle = valintalaskentaValvomo
                    .ajossaOlevatLaskennat().stream()
                            // Tama haku ...
                    .filter(l -> hakuOid.equals(l.getHakuOid())
                            // .. ja koko haun laskennasta on kyse
                            && !l.isOsittainenLaskenta()).findFirst();
            if (ajossaOlevaLaskentaHaulle.isPresent()) {
                // palautetaan seurattavaksi ajossa olevan hakukohteen
                // seurantatunnus
                String uuid = ajossaOlevaLaskentaHaulle.get().getUuid();
                LOG.warn(
                        "Laskenta on jo kaynnissa haulle {} joten palautetaan seurantatunnus({}) ajossa olevaan hakuun",
                        hakuOid, uuid);
                asyncResponse.resume(Response.ok(Vastaus.uudelleenOhjaus(uuid))
                        .build());
                return;
            }
        }
        LOG.info("Aloitetaan laskenta haulle {}", hakuOid);
        haunHakukohteet(
                hakuOid,
                haunHakukohteetOids -> {
                    kasitteleHaunkohteetOids(haunHakukohteetOids, tyyppi,
                            hakuOid,
                            maski,
                            seurantaTunnus,
                            erillishaku,
                            valintaryhmalaskenta,
                            valinnanvaihe,
                            valintakoelaskenta,
                            asyncResponse);
                }, poikkeus -> {
                    asyncResponse.resume(Response.serverError()
                            .entity(poikkeus.getMessage()).build());
                });
    }


    void haunHakukohteet(String hakuOid,
                                 Consumer<List<HakukohdeJaOrganisaatio>> hakukohdeJaOrganisaatioKasittelijaCallback,
                                 Consumer<Throwable> failureCallback) {
        if (StringUtils.isBlank(hakuOid)) {
            LOG.error("Yritettiin hakea hakukohteita ilman hakuOidia!");
            throw new RuntimeException(
                    "Yritettiin hakea hakukohteita ilman hakuOidia!");
        }
        valintaperusteetAsyncResource
                .haunHakukohteet(
                        hakuOid,
                        hakukohdeViitteet -> {
                            kasitteleHakukohdeViitteet(hakukohdeViitteet, hakuOid, hakukohdeJaOrganisaatioKasittelijaCallback, failureCallback);
                        }, poikkeus -> {
                            failureCallback.accept(poikkeus);
                        });
    }

    void kasitteleHakukohdeViitteet(List<HakukohdeViiteDTO> hakukohdeViitteet, String hakuOid, Consumer<List<HakukohdeJaOrganisaatio>> hakukohdeJaOrganisaatioKasittelijaCallback, Consumer<Throwable> failureCallback) {
        LOG.info(
                "Tarkastellaan hakukohdeviitteita haulle {}",
                hakuOid);
        if (hakukohdeViitteet == null
                || hakukohdeViitteet.isEmpty()) {
            LOG.error(
                    "Valintaperusteet palautti tyhjat hakukohdeviitteet haulle {}!",
                    hakuOid);
            throw new NullPointerException(
                    "Valintaperusteet palautti tyhjat hakukohdeviitteet!");
        }
        List<HakukohdeJaOrganisaatio> haunHakukohdeOidit = hakukohdeViitteet
                .stream()
                .filter(Objects::nonNull)
                .filter(h -> {
                    if (h == null) {
                        LOG.error("nonNull filteri ei toimi!");
                        return false;
                    }
                    if (h.getOid() == null) {
                        LOG.error(
                                "Hakukohdeviitteen oid oli null haussa {}",
                                hakuOid);
                        return false;
                    }
                    if (h.getTila() == null) {
                        LOG.error(
                                "Hakukohdeviitteen tila oli null hakukohteelle {}",
                                h.getOid());
                        return false;
                    }
                    boolean julkaistu = "JULKAISTU"
                            .equals(h.getTila());
                    if (!julkaistu) {
                        LOG.warn(
                                "Ohitetaan hakukohde {} koska sen tila on {}.",
                                h.getOid(), h.getTila());
                    }
                    return julkaistu;
                })
                .map(u -> new HakukohdeJaOrganisaatio(u
                        .getOid(), u.getTarjoajaOid()))
                .collect(Collectors.toList());
        if (haunHakukohdeOidit.isEmpty()) {
            LOG.error(
                    "Haulla {} ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?",
                    hakuOid);
            failureCallback
                    .accept(new RuntimeException(
                            "Haulla "
                                    + hakuOid
                                    + " ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?"));
        } else {
            hakukohdeJaOrganisaatioKasittelijaCallback.accept(haunHakukohdeOidit);
        }
    }

    public void kasitteleHaunkohteetOids(Collection<HakukohdeJaOrganisaatio> haunHakukohteetOids, LaskentaTyyppi tyyppi,
                                         String hakuOid,
                                         Maski maski,
                                         BiConsumer<Collection<HakukohdeJaOrganisaatio>, Consumer<String>> seurantaTunnus,
                                         boolean erillishaku, boolean valintaryhmalaskenta,
                                         Integer valinnanvaihe, Boolean valintakoelaskenta,
                                         AsyncResponse asyncResponse) {
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
        final Collection<HakukohdeJaOrganisaatio> finalOids = oids;
        ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid, parametrit -> {
                    seurantaTunnus.accept(
                            finalOids,
                            uuid -> {
                                valintalaskentaRoute
                                        .suoritaValintalaskentaKerralla(
                                                parametrit,
                                                new LaskentaAloitus(
                                                        uuid, hakuOid, erillishaku,
                                                        maski.isMask(),
                                                        valintaryhmalaskenta,
                                                        valinnanvaihe,
                                                        valintakoelaskenta, finalOids,
                                                        tyyppi));
                                asyncResponse.resume(Response.ok(
                                        Vastaus.uudelleenOhjaus(uuid)).build());
                            });
                },
                poikkeus -> {
                    LOG.error("Ohjausparametrien luku epäonnistui: {} {}", poikkeus.getMessage(),
                            Arrays.toString(poikkeus.getStackTrace()));
                    asyncResponse.resume(Response.serverError()
                            .entity(poikkeus.getMessage()).build());
                });
    }

    public void kasitteleLaskennanAloitus(String uuid, AsyncResponse asyncResponse, Consumer<String> laskennanAloitus) {
        if (uuid == null) {
            LOG.error("Laskentaa ei saatu luotua!");
            asyncResponse
                    .resume(Response
                            .serverError()
                            .entity("Laskentaa ei saatu luotua!")
                            .build());
            throw new RuntimeException(
                    "Laskentaa ei saatu luotua!");
        }
        try {
            laskennanAloitus.accept(uuid);
        } catch (Throwable e) {
            LOG.error(
                    "Laskennan kaynnistamisessa tapahtui odottamaton virhe: {}",
                    e.getMessage());
            asyncResponse
                    .resume(Response
                            .serverError()
                            .entity("Odottamaton virhe laskennan kaynnistamisessa! "
                                    + e.getMessage())
                            .build());
            throw e;
        }
    }

    public void kasitteleKokoPaska(Collection<HakukohdeJaOrganisaatio> hakukohdeOids, Consumer<String> laskennanAloitus, AsyncResponse asyncResponse, LaskentaSeurantaAsyncResource seurantaAsyncResource, String hakuOid, LaskentaTyyppi tyyppi, Boolean erillishaku, Integer valinnanvaihe, Boolean valintakoelaskenta) {
        List<HakukohdeDto> hakukohdeDtos = hakukohdeOids
                .stream()
                .filter(hk -> {
                    if (hk == null) {
                        LOG.error("Null referenssi hakukohdeOidsien joukossa laskentaa luotaessa!");
                        return false;
                    }
                    if (hk.getHakukohdeOid() == null) {
                        LOG.error(
                                "HakukohdeOid oli null laskentaa luotaessa! OrganisaatioOid == {}, joten hakukohde ohitetaan!",
                                hk.getOrganisaatioOid());
                        return false;
                    }
                    if (hk.getOrganisaatioOid() == null) {
                        LOG.error(
                                "OrganisaatioOid oli null laskentaa luotaessa! HakukohdeOid == {}, joten hakukohde ohitetaan!",
                                hk.getHakukohdeOid());
                        return false;
                    }
                    return true;
                })
                .map(hk -> new HakukohdeDto(hk
                        .getHakukohdeOid(), hk
                        .getOrganisaatioOid()))
                .collect(Collectors.toList());
        if (hakukohdeDtos.isEmpty()
                || hakukohdeDtos.size() == 0) {
            LOG.error("Laskentaa ei voida aloittaa hakukohteille joilta puuttuu organisaatio!");
            asyncResponse
                    .resume(Response
                            .serverError()
                            .entity("Laskentaa ei voida aloittaa hakukohteille joilta puuttuu organisaatio!")
                            .build());
            throw new RuntimeException(
                    "Laskentaa ei voida aloittaa hakukohteille joilta puuttuu organisaatio!");
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
                uuid -> {
                    kasitteleLaskennanAloitus(uuid, asyncResponse, laskennanAloitus);
                },
                poikkeus -> {
                    LOG.error(
                            "Seurannasta uuden laskennan haku paatyi virheeseen: {}",
                            poikkeus.getMessage());
                    asyncResponse.resume(Response
                            .serverError()
                            .entity(poikkeus.getMessage())
                            .build());
                });
    }

    public void kasitteleKaynnistaLaskentaUudelleen(LaskentaDto laskenta, AsyncResponse asyncResponse) {
        try {
            List<HakukohdeJaOrganisaatio> maski = laskenta
                    .getHakukohteet()
                    .stream()
                    .filter(h -> !HakukohdeTila.VALMIS
                            .equals(h.getTila()))
                    .map(h -> new HakukohdeJaOrganisaatio(
                            h.getHakukohdeOid(),
                            h.getOrganisaatioOid()))
                    .collect(Collectors.toList());
            kaynnistaLaskenta(
                    laskenta.getTyyppi(),
                    laskenta.getHakuOid(),
                    new Maski(
                            true,
                            maski.stream()
                                    .map(hk -> hk
                                            .getHakukohdeOid())
                                    .collect(
                                            Collectors
                                                    .toList())),
                    (hakuJaHakukohteet,
                     laskennanAloitus) -> {
                        laskennanAloitus
                                .accept(laskenta
                                        .getUuid());
                    }, Boolean.TRUE.equals(laskenta
                            .isErillishaku()),
                    LaskentaTyyppi.VALINTARYHMA
                            .equals(laskenta
                                    .getTyyppi()),
                    laskenta.getValinnanvaihe(),
                    laskenta.getValintakoelaskenta(),
                    asyncResponse);
        } catch (Throwable e) {
            LOG.error(
                    "Laskennan kaynnistamisessa tapahtui odottamaton virhe: {}",
                    e.getMessage());
            asyncResponse
                    .resume(Response
                            .serverError()
                            .entity("Odottamaton virhe laskennan kaynnistamisessa! "
                                    + e.getMessage())
                            .build());
            throw e;
        }
    }
}