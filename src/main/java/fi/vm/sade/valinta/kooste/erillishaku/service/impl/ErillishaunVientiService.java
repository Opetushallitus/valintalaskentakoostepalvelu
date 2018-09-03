package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuExcel;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRiviBuilder;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Sukupuoli;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Lukuvuosimaksu;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Maksuntila;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Valinnantulos;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import org.apache.commons.lang3.BooleanUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static rx.Observable.zip;

@Service
public class ErillishaunVientiService {
    private static final Logger LOG = LoggerFactory.getLogger(ErillishaunVientiService.class);
    private final ValintaTulosServiceAsyncResource tilaAsyncResource;
    private final TarjontaAsyncResource hakuV1AsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final AtaruAsyncResource ataruAsyncResource;
    private final DokumenttiResource dokumenttiResource;
    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    @Autowired
    public ErillishaunVientiService(
            ValintaTulosServiceAsyncResource tilaAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            AtaruAsyncResource ataruAsyncResource,
            TarjontaAsyncResource hakuV1AsyncResource,
            DokumenttiResource dokumenttiResource,
            KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
        this.tilaAsyncResource = tilaAsyncResource;
        this.hakuV1AsyncResource = hakuV1AsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource = ataruAsyncResource;
        this.dokumenttiResource = dokumenttiResource;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
        LOG.info("Luetaan valinnantulokset ja sijoittelu valinta-tulos-servicestä!!!!!!!");
    }

    private String teksti(final Map<String, String> nimi) {
        return new Teksti(nimi).getTeksti();
    }

    public void vie(final AuditSession auditSession, KirjeProsessi prosessi, ErillishakuDTO erillishaku) {
        Observable<HakuV1RDTO> hakuFuture = hakuV1AsyncResource.haeHaku(erillishaku.getHakuOid());
        Observable<List<HakemusWrapper>> hakemusObservable = hakuFuture.flatMap(haku -> {
            if (haku.getAtaruLomakeAvain() == null) {
                return applicationAsyncResource.getApplicationsByOid(erillishaku.getHakuOid(), erillishaku.getHakukohdeOid());
            } else {
                return ataruAsyncResource.getApplicationsByHakukohde(erillishaku.getHakukohdeOid());
            }
        });
        Observable<HakukohdeV1RDTO> tarjontaHakukohdeObservable = hakuV1AsyncResource.haeHakukohde(erillishaku.getHakukohdeOid());
        Observable<List<Lukuvuosimaksu>> lukuvuosimaksutObs = tilaAsyncResource.fetchLukuvuosimaksut(erillishaku.getHakukohdeOid(), auditSession);

        Observable<ErillishakuExcel> erillishakuExcel =
                generoiValintarekisterista(auditSession, erillishaku, hakemusObservable, hakuFuture, tarjontaHakukohdeObservable, lukuvuosimaksutObs);

        erillishakuExcel.subscribeOn(Schedulers.newThread()).subscribe(
            excel -> {
                LOG.info("Aloitetaan dokumenttipalveluun tallennus");
                String uuid = UUID.randomUUID().toString();
                dokumenttiResource.tallenna(uuid, "erillishaku.xlsx", DateTime.now().plusHours(1).toDate().getTime(),
                        Collections.singletonList("erillishaku"), "application/octet-stream", excel.getExcel().vieXlsx());
                prosessi.vaiheValmistui();
                prosessi.valmistui(uuid);
            },
            poikkeus -> {
                LOG.error("Erillishaun vienti keskeytyi virheeseen", poikkeus);
                prosessi.keskeyta();
            },
            () -> {
                LOG.info("Erillishaun vienti onnistui!");
                prosessi.keskeyta();
            }
        );
    }

    private String objectToString(Object o) {
        if(o == null) {
            return "";
        } else {
            return o.toString();
        }
    }

    private Observable<ErillishakuExcel> generoiValintarekisterista(AuditSession auditSession,
                                                                    ErillishakuDTO erillishaku,
                                                                    Observable<List<HakemusWrapper>> hakemusObservable,
                                                                    Observable<HakuV1RDTO> hakuFuture,
                                                                    Observable<HakukohdeV1RDTO> tarjontaHakukohdeObservable,
                                                                    Observable<List<Lukuvuosimaksu>> lukuvuosimaksuObs) {
        Observable<List<Valinnantulos>> valinnantulosObservable = tilaAsyncResource.getErillishaunValinnantulokset(auditSession, erillishaku.getValintatapajonoOid());

        return zip(hakemusObservable, hakuFuture, tarjontaHakukohdeObservable, valinnantulosObservable, lukuvuosimaksuObs,
                (hakemukset, haku, tarjontaHakukohde, valinnantulos, lukuvuosimaksus) -> {
                    if(valinnantulos.isEmpty()) {
                        return generoiIlmanHakukohdettaJaTuloksia(erillishaku, hakemukset, lukuvuosimaksus, haku, tarjontaHakukohde);
                    } else {
                        return generoiValinnantuloksista(erillishaku, hakemukset, lukuvuosimaksus, haku, tarjontaHakukohde, valinnantulos);
                    }
                }
        );
    }

    private ErillishakuExcel generoiValinnantuloksista(final ErillishakuDTO erillishaku, final List<HakemusWrapper> hakemukset, final List<Lukuvuosimaksu> lukuvuosimaksus,
                                                       final HakuV1RDTO haku, final HakukohdeV1RDTO tarjontaHakukohde,
                                                       final List<Valinnantulos> valinnantulos) {
        LOG.info("Muodostetaan Excel valinnantuloksista!");
        Map<String, Valinnantulos> valinnantulokset = valinnantulos.stream().collect(Collectors.toMap(Valinnantulos::getHakemusOid, v -> v));
        List<ErillishakuRivi> erillishakuRivit = hakemukset.stream().map(hakemus -> {
            Optional<Valinnantulos> tulosOpt = Optional.ofNullable(valinnantulokset.get(hakemus.getOid()));
            Optional<Maksuntila> maksuntila = lukuvuosimaksus.stream().filter(l -> hakemus.getPersonOid().equals(l.getPersonOid())).map(Lukuvuosimaksu::getMaksuntila).findAny();
            return tulosOpt.map(tulos ->
                    createErillishakuRivi(hakemus.getOid(), hakemus,
                            maksuntila,
                            tulos.getValinnantila().toString(),
                            new Valintatulos(
                                    hakemus.getOid(),
                                    tulos.getHenkiloOid(),
                                    tulos.getHakukohdeOid(),
                                    haku.getOid(),
                                    0, // NB: hakutoive aina 0!
                                    BooleanUtils.isTrue(tulos.getHyvaksyttyVarasijalta()),
                                    tulos.getIlmoittautumistila(),
                                    BooleanUtils.isTrue(tulos.getJulkaistavissa()),
                                    tulos.getVastaanottotila(),
                                    BooleanUtils.isTrue(tulos.getEhdollisestiHyvaksyttavissa()),
                                    tulos.getValintatapajonoOid(),
                                    null == tulos.getHyvaksymiskirjeLahetetty() ? null : Date.from(tulos.getHyvaksymiskirjeLahetetty().toInstant()),
                                    tulos.getEhdollisenHyvaksymisenEhtoKoodi(),
                                    tulos.getEhdollisenHyvaksymisenEhtoFI(),
                                    tulos.getEhdollisenHyvaksymisenEhtoSV(),
                                    tulos.getEhdollisenHyvaksymisenEhtoEN()
                            ),
                            tulos.getHakukohdeOid()
                    )
            ).orElse(
                    createErillishakuRivi(hakemus.getOid(), hakemus,
                            maksuntila,
                            "KESKEN",
                            null,
                            tarjontaHakukohde.getOid()
                    )
            );
        }).collect(Collectors.toList());
        return new ErillishakuExcel(erillishaku.getHakutyyppi(), teksti(haku.getNimi()), teksti(tarjontaHakukohde.getHakukohteenNimet()), teksti(tarjontaHakukohde.getTarjoajaNimet()), erillishakuRivit, koodistoCachedAsyncResource);
    }

    private ErillishakuExcel generoiIlmanHakukohdettaJaTuloksia(final ErillishakuDTO erillishaku, final List<HakemusWrapper> hakemukset, final List<Lukuvuosimaksu> lukuvuosimaksus,
                                                                final HakuV1RDTO haku, final HakukohdeV1RDTO tarjontaHakukohde) {
        LOG.info("Hakemuksia ei ole viela tuotu ensimmaistakaan kertaa talle hakukohteelle! Generoidaan hakemuksista excel...");
        Map<String, Maksuntila> personOidToMaksuntila = lukuvuosimaksus.stream().collect(Collectors.toMap(l -> l.getPersonOid(), l -> l.getMaksuntila()));
        List<ErillishakuRivi> rivit = hakemukset.stream().map(hakemus ->
            createErillishakuRivi(hakemus.getOid(), hakemus, ofNullable(personOidToMaksuntila.get(hakemus.getPersonOid())), "KESKEN", null, tarjontaHakukohde.getOid())
        ).collect(Collectors.toList());
        return new ErillishakuExcel(erillishaku.getHakutyyppi(), teksti(haku.getNimi()), teksti(tarjontaHakukohde.getHakukohteenNimet()), teksti(tarjontaHakukohde.getTarjoajaNimet()), rivit, koodistoCachedAsyncResource);
    }

    private ErillishakuRivi createErillishakuRivi(String oid, HakemusWrapper wrapper, Optional<Maksuntila> lukuvuosimaksu, String hakemuksenTila,
                                                  Valintatulos valintatulos, String hakukohdeOid) {
        ErillishakuRiviBuilder builder = new ErillishakuRiviBuilder()
                .hakemusOid(oid)
                .sukunimi(wrapper.getSukunimi())
                .etunimi(wrapper.getEtunimi())
                .henkilotunnus(wrapper.getHenkilotunnus())
                .sahkoposti(wrapper.getSahkopostiOsoite())
                .syntymaAika(wrapper.getSyntymaaika())
                .sukupuoli(Sukupuoli.fromString(wrapper.getSukupuoliAsIs()))
                .personOid(wrapper.getPersonOid())
                .aidinkieli(wrapper.getAidinkieli())
                .hakemuksenTila(hakemuksenTila)
                .poistetaankoRivi(false)
                .asiointikieli(readAsiointikieli(wrapper))
                .puhelinnumero(wrapper.getPuhelinnumero())
                .osoite(readLahiosoite(wrapper))
                .postinumero(readPostinumero(wrapper))
                .postitoimipaikka(readPostitoimipaikka(wrapper))
                .asuinmaa(wrapper.getAsuinmaa())
                .kansalaisuus(wrapper.getKansalaisuus())
                .kotikunta(readKotikunta(wrapper))
                .toisenAsteenSuoritus(wrapper.getToisenAsteenSuoritus())
                .toisenAsteenSuoritusmaa(wrapper.getToisenAsteenSuoritusmaa())
                .maksuvelvollisuus(wrapper.getMaksuvelvollisuus(hakukohdeOid))
                .maksuntila(wrapper.isMaksuvelvollinen(hakukohdeOid) ? lukuvuosimaksu.orElse(Maksuntila.MAKSAMATTA) : null);

        if (valintatulos != null) {
            builder
                .ehdollisestiHyvaksyttavissa(valintatulos.getEhdollisestiHyvaksyttavissa())
                .ehdollisenHyvaksymisenEhtoKoodi(valintatulos.getEhdollisenHyvaksymisenEhtoKoodi())
                .ehdollisenHyvaksymisenEhtoFI(valintatulos.getEhdollisenHyvaksymisenEhtoFI())
                .ehdollisenHyvaksymisenEhtoSV(valintatulos.getEhdollisenHyvaksymisenEhtoSV())
                .ehdollisenHyvaksymisenEhtoEN(valintatulos.getEhdollisenHyvaksymisenEhtoEN())
                .hyvaksymiskirjeLahetetty(valintatulos.getHyvaksymiskirjeLahetetty())
                .vastaanottoTila(objectToString(valintatulos.getTila()))
                .ilmoittautumisTila(objectToString(valintatulos.getIlmoittautumisTila()))
                .julkaistaankoTiedot(valintatulos.getJulkaistavissa());
        }

        return builder.build();
    }

    private String readAsiointikieli(HakemusWrapper wrapper) {
        return wrapper.hasAsiointikieli() ? wrapper.getAsiointikieli() : null;
    }

    private String readKotikunta(HakemusWrapper wrapper) {
        String kuntanumero = wrapper.getKotikunta();
        if(isNotBlank(kuntanumero)) {
            Map<String, Koodi> kuntaKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.KUNTA);
            Koodi postitoimipaikka = kuntaKoodit.get(kuntanumero);
            return KoodistoCachedAsyncResource.haeKoodistaArvo(postitoimipaikka, "FI", null);
        }
        return null;
    }

    private String readPostitoimipaikka(HakemusWrapper wrapper) {
        String suomalainenPostinumero = wrapper.getSuomalainenPostinumero();

        if(isNotBlank(suomalainenPostinumero)) {
            Map<String, Koodi> postiKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
            Koodi postitoimipaikka = postiKoodit.get(suomalainenPostinumero);
            return KoodistoCachedAsyncResource.haeKoodistaArvo(postitoimipaikka, "FI", null);
        }

        return wrapper.getKaupunkiUlkomaa();
    }

    private String readLahiosoite(HakemusWrapper wrapper) {
        return isNotBlank(wrapper.getSuomalainenLahiosoite()) ? wrapper.getSuomalainenLahiosoite() : wrapper.getUlkomainenLahiosoite();
    }

    private String readPostinumero(HakemusWrapper wrapper) {
        return isNotBlank(wrapper.getSuomalainenPostinumero()) ? wrapper.getSuomalainenPostinumero() : wrapper.getUlkomainenPostinumero();
    }
}
