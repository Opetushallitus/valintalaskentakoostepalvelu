package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuExcel;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRiviBuilder;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Sukupuoli;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Valinnantulos;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import org.apache.commons.collections.MapUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.functions.Func5;
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static rx.Observable.from;
import static rx.Observable.zip;

@Service
public class ErillishaunVientiService {
    private static final Logger LOG = LoggerFactory.getLogger(ErillishaunVientiService.class);
    private final SijoitteluAsyncResource sijoitteluAsyncResource;
    private final ValintaTulosServiceAsyncResource tilaAsyncResource;
    private final TarjontaAsyncResource hakuV1AsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final DokumenttiResource dokumenttiResource;
    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
    private final boolean useVtsData;

    @Autowired
    public ErillishaunVientiService(
            @Value("${valintalaskentakoostepalvelu.read-from-valintarekisteri}") String useVtsData,
            ValintaTulosServiceAsyncResource tilaAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            SijoitteluAsyncResource sijoitteluAsyncResource,
            TarjontaAsyncResource hakuV1AsyncResource,
            DokumenttiResource dokumenttiResource,
            KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.tilaAsyncResource = tilaAsyncResource;
        this.hakuV1AsyncResource = hakuV1AsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.dokumenttiResource = dokumenttiResource;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
        this.useVtsData = "TRUE".equalsIgnoreCase(useVtsData);
        if(this.useVtsData) {
            LOG.info("Luetaan valinnantulokset ja sijoittelu valinta-tulos-servicestä!!!!!!!");
        }
    }

    private String teksti(final Map<String, String> nimi) {
        return new Teksti(nimi).getTeksti();
    }

    public void vie(final AuditSession auditSession, KirjeProsessi prosessi, ErillishakuDTO erillishaku) {
        Observable<List<Hakemus>> hakemusObservable = applicationAsyncResource.getApplicationsByOid(erillishaku.getHakuOid(), erillishaku.getHakukohdeOid());
        Observable<HakuV1RDTO> hakuFuture = hakuV1AsyncResource.haeHaku(erillishaku.getHakuOid());
        Observable<HakukohdeV1RDTO> tarjontaHakukohdeObservable = hakuV1AsyncResource.haeHakukohde(erillishaku.getHakukohdeOid());

        Observable<ErillishakuExcel> erillishakuExcel = useVtsData ?
                generoiValintarekisterista(auditSession, erillishaku, hakemusObservable, hakuFuture, tarjontaHakukohdeObservable) :
                generoiSijoittelusta(erillishaku, hakemusObservable, hakuFuture, tarjontaHakukohdeObservable);

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
                                                                    Observable<List<Hakemus>> hakemusObservable,
                                                                    Observable<HakuV1RDTO> hakuFuture,
                                                                    Observable<HakukohdeV1RDTO> tarjontaHakukohdeObservable) {
        Observable<List<Valinnantulos>> valinnantulosObservable = tilaAsyncResource.getErillishaunValinnantulokset(auditSession, erillishaku.getValintatapajonoOid());

        return zip(hakemusObservable, hakuFuture, tarjontaHakukohdeObservable, valinnantulosObservable,
                (hakemukset, haku, tarjontaHakukohde, valinnantulos) -> {
                    if(valinnantulos.isEmpty()) {
                        return generoiIlmanHakukohdettaJaTuloksia(erillishaku, hakemukset, haku, tarjontaHakukohde);
                    } else {
                        return generoiValinnantuloksista(erillishaku, hakemukset, haku, tarjontaHakukohde, valinnantulos);
                    }
                }
        );
    }

    private Observable<ErillishakuExcel> generoiSijoittelusta(ErillishakuDTO erillishaku,
                                                              Observable<List<Hakemus>> hakemusObservable,
                                                              Observable<HakuV1RDTO> hakuFuture,
                                                              Observable<HakukohdeV1RDTO> tarjontaHakukohdeObservable) {

        Observable<List<Valintatulos>> valintatulosObservable = tilaAsyncResource.findValintatulokset(erillishaku.getHakuOid(), erillishaku.getHakukohdeOid());
        Future<HakukohdeDTO> hakukohdeFuture = sijoitteluAsyncResource.getLatestHakukohdeBySijoittelu(erillishaku.getHakuOid(), erillishaku.getHakukohdeOid());

        return zip(hakemusObservable, hakuFuture, tarjontaHakukohdeObservable, valintatulosObservable, from(hakukohdeFuture),
            (hakemukset, haku, tarjontaHakukohde, valintatulos, hakukohde) -> {
                Map<String, Valintatulos> valintatulokset = getValintatulokset(erillishaku, valintatulos);
                if (MapUtils.isEmpty(valintatulokset) && hakukohde.getSijoitteluajoId() == null) {
                    // ei viela tuloksia, joten tehdaan tuonti haetuista hakemuksista
                    return generoiIlmanHakukohdettaJaTuloksia(erillishaku, hakemukset, haku, tarjontaHakukohde);
                } else if(MapUtils.isEmpty(valintatulokset) && hakukohde.getSijoitteluajoId() != null) {
                    return generoiHakukohteella(erillishaku, hakemukset, haku, tarjontaHakukohde, hakukohde);
                } else {
                    return generoiHakukohteellaJaTuloksilla(erillishaku, hakemukset, haku, tarjontaHakukohde, hakukohde, valintatulokset);
                }
            }
        );
    }

    private ErillishakuExcel generoiHakukohteellaJaTuloksilla(final ErillishakuDTO erillishaku, final List<Hakemus> hakemukset,
                                                              final HakuV1RDTO haku, final HakukohdeV1RDTO tarjontaHakukohde,
                                                              final HakukohdeDTO hakukohde, final Map<String, Valintatulos> valintatulokset) {
        LOG.info("Muodostetaan Excel valintaperusteista!");
        Map<String, HakemusDTO> oidToHakemus = hakukohde
                .getValintatapajonot().stream()
                .flatMap(v -> v.getHakemukset().stream()).collect(Collectors.toMap(HakemusDTO::getHakemusOid, h -> h));
        List<ErillishakuRivi> erillishakurivit = hakemukset.stream().map(hakemus -> {
            Optional<HakemusDTO> hakemusDTO = Optional.ofNullable(oidToHakemus.get(hakemus.getOid()));
            Optional<HakemuksenTila> hakemuksenTila = hakemusDTO.map(HakemusDTO::getTila);
            Valintatulos valintatulos = Optional.ofNullable(valintatulokset.get(hakemus.getOid())).orElse(new Valintatulos());
            return createErillishakuRivi(hakemus.getOid(), new HakemusWrapper(hakemus),
                    hakemuksenTila.map(HakemuksenTila::toString).orElse("KESKEN"), valintatulos, hakukohde.getOid());
        }).collect(Collectors.toList());
        return new ErillishakuExcel(erillishaku.getHakutyyppi(), teksti(haku.getNimi()), teksti(tarjontaHakukohde.getHakukohteenNimet()), teksti(tarjontaHakukohde.getTarjoajaNimet()), erillishakurivit);
    }

    private ErillishakuExcel generoiValinnantuloksista(final ErillishakuDTO erillishaku, final List<Hakemus> hakemukset,
                                                       final HakuV1RDTO haku, final HakukohdeV1RDTO tarjontaHakukohde,
                                                       final List<Valinnantulos> valinnantulos) {
        LOG.info("Muodostetaan Excel valinnantuloksista!");
        Map<String, Valinnantulos> valinnantulokset = valinnantulos.stream().collect(Collectors.toMap(Valinnantulos::getHakemusOid, v -> v));
        List<ErillishakuRivi> erillishakuRivit = hakemukset.stream().map(hakemus -> {
            Optional<Valinnantulos> tulosOpt = Optional.ofNullable(valinnantulokset.get(hakemus.getOid()));
            return tulosOpt.map(tulos ->
               createErillishakuRivi(hakemus.getOid(), new HakemusWrapper(hakemus),
                     tulos.getValinnantila().toString(),
                     new Valintatulos(
                             hakemus.getOid(),
                             tulos.getHenkiloOid(),
                             tulos.getHakukohdeOid(),
                             haku.getOid(),
                             0,
                             tulos.getHyvaksyttyVarasijalta(),
                             tulos.getIlmoittautumistila(),
                             tulos.getJulkaistavissa(),
                             tulos.getVastaanottotila(),
                             tulos.getEhdollisestiHyvaksyttavissa(),
                             tulos.getValintatapajonoOid(),
                             null == tulos.getHyvaksymiskirjeLahetetty() ? null : tulos.getHyvaksymiskirjeLahetetty().toDate(),
                             tulos.getEhdollisenHyvaksymisenEhtoKoodi(),
                             tulos.getEhdollisenHyvaksymisenEhtoFI(),
                             tulos.getEhdollisenHyvaksymisenEhtoSV(),
                             tulos.getEhdollisenHyvaksymisenEhtoEN()
                     ),
                     tulos.getHakukohdeOid()
               )
            ).orElse(
                createErillishakuRivi(hakemus.getOid(), new HakemusWrapper(hakemus),
                        "KESKEN",
                        null,
                        tarjontaHakukohde.getOid()
                )
            );
        }).collect(Collectors.toList());
        return new ErillishakuExcel(erillishaku.getHakutyyppi(), teksti(haku.getNimi()), teksti(tarjontaHakukohde.getHakukohteenNimet()), teksti(tarjontaHakukohde.getTarjoajaNimet()), erillishakuRivit);
    }

    private ErillishakuExcel generoiHakukohteella(final ErillishakuDTO erillishaku, final List<Hakemus> hakemukset,
                                                  final HakuV1RDTO haku, final HakukohdeV1RDTO tarjontaHakukohde,
                                                  final HakukohdeDTO hakukohde) {
        LOG.info("Muodostetaan Excel valintaperusteista!");
        Map<String, HakemusDTO> oidToHakemus = hakukohde
                .getValintatapajonot().stream()
                .flatMap(v -> v.getHakemukset().stream()).collect(Collectors.toMap(HakemusDTO::getHakemusOid, h -> h));
        List<ErillishakuRivi> erillishakurivit = hakemukset.stream().map(hakemus -> {
                Optional<HakemusDTO> hakemusDTO = Optional.ofNullable(oidToHakemus.get(hakemus.getOid()));
            Optional<HakemuksenTila> hakemuksenTila = hakemusDTO.map(HakemusDTO::getTila);
            return createErillishakuRivi(hakemus.getOid(), new HakemusWrapper(hakemus), hakemuksenTila.map(HakemuksenTila::toString).orElse("KESKEN"), null, hakukohde.getOid());
            }).collect(Collectors.toList());
        return new ErillishakuExcel(erillishaku.getHakutyyppi(), teksti(haku.getNimi()), teksti(tarjontaHakukohde.getHakukohteenNimet()), teksti(tarjontaHakukohde.getTarjoajaNimet()), erillishakurivit);
    }

    private ErillishakuExcel generoiIlmanHakukohdettaJaTuloksia(final ErillishakuDTO erillishaku, final List<Hakemus> hakemukset,
                                                                final HakuV1RDTO haku, final HakukohdeV1RDTO tarjontaHakukohde) {
        LOG.info("Hakemuksia ei ole viela tuotu ensimmaistakaan kertaa talle hakukohteelle! Generoidaan hakemuksista excel...");
        List<ErillishakuRivi> rivit = hakemukset.stream().map(hakemus ->
            createErillishakuRivi(hakemus.getOid(), new HakemusWrapper(hakemus), "KESKEN", null, tarjontaHakukohde.getOid())
        ).collect(Collectors.toList());
        return new ErillishakuExcel(erillishaku.getHakutyyppi(), teksti(haku.getNimi()), teksti(tarjontaHakukohde.getHakukohteenNimet()), teksti(tarjontaHakukohde.getTarjoajaNimet()), rivit);
    }

    private ErillishakuRivi createErillishakuRivi(String oid, HakemusWrapper wrapper, String hakemuksenTila,
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
                .maksuvelvollisuus(wrapper.getMaksuvelvollisuus(hakukohdeOid));

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

    private Map<String, Valintatulos> getValintatulokset(final ErillishakuDTO erillishaku, final List<Valintatulos> valintatulos) {
        try {
            return valintatulos.stream()
                    .filter(v -> erillishaku.getValintatapajonoOid().equals(v.getValintatapajonoOid()))
                    .collect(Collectors.toMap(Valintatulos::getHakemusOid, v -> v));
        } catch (Exception e1) {
            LOG.error("Sijoittelusta ei saatu tietoja hakukohteelle vientia varten! Resurssi /sijoittelu/{}/sijoitteluajo/{}/hakukohde/{} ",
                    erillishaku.getHakuOid(), erillishaku.getHakukohdeOid(), SijoitteluResource.LATEST);
            LOG.error("Stack trace", e1);
            throw new RuntimeException(e1);
        }
    }
}
