package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import static com.codepoetics.protonpack.StreamUtils.zip;
import static com.codepoetics.protonpack.StreamUtils.zipWithIndex;
import static fi.vm.sade.auditlog.valintaperusteet.LogMessage.builder;
import static fi.vm.sade.valinta.kooste.KoosteAudit.AUDIT;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_HAKEMUSPALVELUN_VIRHE;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_HENKILOPALVELUN_VIRHE;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_RIVIN_HAKEMINEN_HENKILOLLA_VIRHE;
import static fi.vm.sade.valinta.kooste.util.HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus;
import static org.apache.commons.lang.StringUtils.isBlank;
import static rx.schedulers.Schedulers.newThread;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.authentication.model.Kielisyys;
import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.http.FailedHttpException;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuDataRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Sukupuoli;
import fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource;
import fi.vm.sade.valinta.kooste.erillishaku.util.ValidoiTilatUtil;
import fi.vm.sade.valinta.kooste.exception.ErillishaunDataException;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Metadata;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.HakukohteenValintatulosUpdateStatuses;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.ValintatulosUpdateStatus;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoRecordDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoResultDTO;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tunniste;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.Scheduler;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ErillishaunTuontiService {
    private static final Logger LOG = LoggerFactory.getLogger(ErillishaunTuontiService.class);

    private final TilaAsyncResource tilaAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final HenkiloAsyncResource henkiloAsyncResource;
    private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
    private final Scheduler scheduler;

    private static final String PHONE_PATTERN = "^$|^([0-9\\(\\)\\/\\+ \\-]*)$";


    public ErillishaunTuontiService(TilaAsyncResource tilaAsyncResource,
                                    ApplicationAsyncResource applicationAsyncResource,
                                    HenkiloAsyncResource henkiloAsyncResource,
                                    ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
                                    KoodistoCachedAsyncResource koodistoCachedAsyncResource,
                                    Scheduler scheduler) {
        this.applicationAsyncResource = applicationAsyncResource;
        this.tilaAsyncResource = tilaAsyncResource;
        this.henkiloAsyncResource = henkiloAsyncResource;
        this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
        this.scheduler = scheduler;
    }

    @Autowired
    public ErillishaunTuontiService(TilaAsyncResource tilaAsyncResource,
                                    ApplicationAsyncResource applicationAsyncResource,
                                    HenkiloAsyncResource henkiloAsyncResource,
                                    ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
                                    KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
        this(tilaAsyncResource,
                applicationAsyncResource,
                henkiloAsyncResource,
                valintaTulosServiceAsyncResource,
                koodistoCachedAsyncResource,
                newThread());
    }

    public void tuoExcelistä(String username, KirjeProsessi prosessi, ErillishakuDTO erillishaku, InputStream data) {
        tuoData(username, prosessi, erillishaku, (haku) -> new ImportedErillisHakuExcel(haku.getHakutyyppi(), data));
    }

    public void tuoJson(String username, KirjeProsessi prosessi, ErillishakuDTO erillishaku, List<ErillishakuRivi> erillishakuRivit) {
        tuoData(username, prosessi, erillishaku, (haku) -> new ImportedErillisHakuExcel(erillishakuRivit));
    }

    private void tuoData(String username, KirjeProsessi prosessi, ErillishakuDTO erillishaku, Function<ErillishakuDTO, ImportedErillisHakuExcel> importer) {
        Observable.just(erillishaku).subscribeOn(scheduler).subscribe(haku -> {
            final ImportedErillisHakuExcel erillishakuExcel;
            try {
                erillishakuExcel = importer.apply(haku);
                tuoHakijatJaLuoHakemukset(username, prosessi, erillishakuExcel, haku);
            } catch(Exception e) {
                LOG.error("tuoData exception!", e);
                prosessi.keskeyta();
            }
        }, poikkeus -> {
            LOG.error("Erillishaun tuonti keskeytyi virheeseen", poikkeus);
            prosessi.keskeyta();
        }, () -> LOG.info("Tuonti lopetettiin"));
    }

    private void validoiRivit(final KirjeProsessi prosessi, final ErillishakuDTO haku, final List<ErillishakuRivi> rivit) {
        if (rivit.isEmpty()) {
            LOG.error("Syötteestä ei saatu poimittua yhtaan hakijaa sijoitteluun tuotavaksi!");
            prosessi.keskeyta(ErillishakuResource.POIKKEUS_TYHJA_DATAJOUKKO);
            throw new RuntimeException("Syötteestä ei saatu poimittua yhtaan hakijaa sijoitteluun tuotavaksi!");
        }

        Collection<ErillishaunDataException.PoikkeusRivi> poikkeusRivis = Lists.newArrayList();
        zipWithIndex(
                rivit.stream().map(rivi -> {
                    // AUTOTAYTTO VA
                    rivi.getHakemuksenTila();
                    return rivi;
                }))
                .forEach(riviJaIndeksi -> {
                    int indeksi = ((int) riviJaIndeksi.getIndex()) + 1;
                    ErillishakuRivi rivi = riviJaIndeksi.getValue();

                    if (!rivi.isPoistetaankoRivi()) {
                        String validointiVirhe = validoi(haku.getHakutyyppi(), rivi);
                        if (validointiVirhe != null) {
                            poikkeusRivis.add(new ErillishaunDataException.PoikkeusRivi(indeksi, validointiVirhe));
                        }
                    } else {
                        // validoi poistettavaksi merkitty rivi
                        if (rivi.getHakemusOid() == null) {
                            poikkeusRivis.add(new ErillishaunDataException.PoikkeusRivi(indeksi, "Poistettavaksi merkatulla riville ei löytynyt hakemuksen tunnistetta"));
                        }
                    }
                });
        if(!poikkeusRivis.isEmpty()) {
            prosessi.keskeyta(Poikkeus.koostepalvelupoikkeus(ErillishakuResource.POIKKEUS_VIALLINEN_DATAJOUKKO,
                    poikkeusRivis.stream().map(p -> new Tunniste("Rivi " + p.getIndeksi() + ": " + p.getSelite(),ErillishakuResource.RIVIN_TUNNISTE_KAYTTOLIITTYMAAN)).collect(Collectors.toList())));
            throw new ErillishaunDataException(poikkeusRivis);
        }
    }

    private static final List<HakemuksenTila> VAIN_HAKEMUKSENTILALLISET_TILAT =
            Arrays.asList(HakemuksenTila.HYLATTY,
            HakemuksenTila.VARALLA,HakemuksenTila.PERUUNTUNUT);

    private static boolean isUusi(ErillishakuRivi rivi) {
        return StringUtils.isEmpty(rivi.getVastaanottoTila()) && StringUtils.isEmpty(rivi.getIlmoittautumisTila());
    }

    private static List<ErillishakuRivi> autoTaytto(final List<ErillishakuRivi> rivit) {
        // jos hakemuksentila on hylatty tai varalla niin autotaytetaan loput tilat KESKEN, EI_TEHTY

        return rivit.stream().map(rivi -> {
            if (VAIN_HAKEMUKSENTILALLISET_TILAT.contains(hakemuksenTila(rivi)) && !isUusi(rivi) ) {
                return new ErillishakuRivi(
                        rivi.getHakemusOid(),
                        rivi.getSukunimi(),
                        rivi.getEtunimi(),
                        rivi.getHenkilotunnus(),
                        rivi.getSahkoposti(),
                        rivi.getSyntymaAika(),
                        rivi.getSukupuoli(),
                        rivi.getPersonOid(),
                        rivi.getAidinkieli(),
                        rivi.getHakemuksenTila(),
                        rivi.getEhdollisestiHyvaksyttavissa(),
                        rivi.getHyvaksymiskirjeLahetetty(),
                        "KESKEN", "EI_TEHTY",
                        rivi.isJulkaistaankoTiedot(),
                        rivi.isPoistetaankoRivi(),
                        rivi.getAsiointikieli(),
                        rivi.getPuhelinnumero(),
                        rivi.getOsoite(),
                        rivi.getPostinumero(),
                        rivi.getPostitoimipaikka(),
                        rivi.getAsuinmaa(),
                        rivi.getKansalaisuus(),
                        rivi.getKotikunta(),
                        rivi.getPohjakoulutusMaaToinenAste());
            } else {
                return rivi;
                }
            }).collect(Collectors.toList());
        }

    private void tuoHakijatJaLuoHakemukset(final String username, final KirjeProsessi prosessi, final ImportedErillisHakuExcel erillishakuExcel, final ErillishakuDTO haku) throws Exception {
        LOG.info("Aloitetaan tuonti. Rivit=" + erillishakuExcel.rivit.size());
        final List<ErillishakuRivi> rivit = autoTaytto(erillishakuExcel.rivit);

        validoiRivit(prosessi,haku,rivit);

        List<ErillishakuRivi> lisattavatTaiKeskeneraiset = rivit.stream()
                .filter(rivi -> !rivi.isPoistetaankoRivi()).collect(Collectors.toList());
        LOG.info("lisattavatTaiKeskeneraiset="+lisattavatTaiKeskeneraiset.size());
        List<ErillishakuRivi> poistettavat = rivit.stream()
                .filter(rivi -> !rivi.isKesken() && rivi.isPoistetaankoRivi()).collect(Collectors.toList());
        final List<Hakemus> hakemukset;
        if(!lisattavatTaiKeskeneraiset.isEmpty()) {
            LOG.info("Haetaan/luodaan henkilöt");
            final List<Henkilo> henkilot;
            try {
                henkilot = henkiloAsyncResource.haeTaiLuoHenkilot(
                        lisattavatTaiKeskeneraiset.stream()
                                .map(rivi -> rivi.toHenkiloCreateDTO(convertKansalaisuusKoodi(rivi.getKansalaisuus())))
                                .collect(Collectors.toList()))
                        .get();
                LOG.info("Luotiin henkilot=" + henkilot.stream().map(h -> h.getOidHenkilo()).collect(Collectors.toList()));
            } catch (Exception e) {
                LOG.error(POIKKEUS_HENKILOPALVELUN_VIRHE, e);
                prosessi.keskeyta(Poikkeus.henkilopalvelupoikkeus(POIKKEUS_HENKILOPALVELUN_VIRHE));
                throw e;
            }
            LOG.info("Käsitellään hakemukset ({}kpl)", lisattavatTaiKeskeneraiset.size());
            hakemukset = kasitteleHakemukset(haku, henkilot, lisattavatTaiKeskeneraiset, prosessi);
        } else {
            hakemukset = Collections.emptyList();
        }
        LOG.info("Viedaan hakijoita ohittaen rivit hakemuksentilalla kesken ({}/{}) jonoon {}", lisattavatTaiKeskeneraiset.stream().filter(r -> !r.isKesken()).count(), rivit.size(), haku.getValintatapajononNimi());
        tuoErillishaunTilat(username, haku, lisattavatTaiKeskeneraiset, poistettavat, hakemukset, prosessi);
    }

    private String convertKansalaisuusKoodi(String kansalaisuus) {
        Map<String, Koodi> maaKoodit1 = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
        Map<String, Koodi> maaKoodit2 = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_2);

        String maaNimi = KoodistoCachedAsyncResource.haeKoodistaArvo(maaKoodit1.get(kansalaisuus), "FI", null);
        return maaKoodit2.values().stream().flatMap(koodi -> koodi.getMetadata().stream().map(metadata -> new ImmutablePair<>(metadata.getNimi(), koodi.getKoodiArvo())))
                .filter(x -> x.getLeft().equalsIgnoreCase(maaNimi))
                .map(ImmutablePair::getRight)
                .findFirst()
                .orElse(null);
    }

    private List<Hakemus> kasitteleHakemukset(ErillishakuDTO haku, List<Henkilo> henkilot, List<ErillishakuRivi> lisattavatTaiKeskeneraiset, KirjeProsessi prosessi) throws InterruptedException, ExecutionException {
        try {
            final List<HakemusPrototyyppi> hakemusPrototyypit = henkilot.stream()
                .map(h -> createHakemusprototyyppi(h, lisattavatTaiKeskeneraiset))
                .collect(Collectors.toList());
            return applicationAsyncResource.putApplicationPrototypes(haku.getHakuOid(), haku.getHakukohdeOid(), haku.getTarjoajaOid(), hakemusPrototyypit).get();
        } catch (HenkilonRivinPaattelyEpaonnistuiException e) {
            LOG.error(POIKKEUS_RIVIN_HAKEMINEN_HENKILOLLA_VIRHE, e);
            prosessi.keskeyta(Poikkeus.hakemuspalvelupoikkeus(POIKKEUS_RIVIN_HAKEMINEN_HENKILOLLA_VIRHE + " " + e.getMessage()));
            throw e;
        } catch (Throwable e) { // temporary catch to avoid missing service dependencies
            LOG.error(POIKKEUS_HAKEMUSPALVELUN_VIRHE, e);
            prosessi.keskeyta(Poikkeus.hakemuspalvelupoikkeus(POIKKEUS_HAKEMUSPALVELUN_VIRHE));
            throw e;
        }
    }

    private HakemusPrototyyppi createHakemusprototyyppi(Henkilo henkilo, List<ErillishakuRivi> kaikkiLisattavatTaiKeskeneraiset) {
        ErillishakuRivi rivi = etsiHenkiloaVastaavaRivi(henkilo, kaikkiLisattavatTaiKeskeneraiset);
        return createHakemusprototyyppi(henkilo, rivi);
    }

    private ErillishakuRivi etsiHenkiloaVastaavaRivi(Henkilo henkilo, List<ErillishakuRivi> kaikkiLisattavatTaiKeskeneraiset) {
        Optional<ErillishakuRivi> riviOidinMukaan = kaikkiLisattavatTaiKeskeneraiset.stream().filter(r ->
            StringUtils.isNotBlank(r.getPersonOid()) && r.getPersonOid().equals(henkilo.getOidHenkilo())).findFirst();
        if (riviOidinMukaan.isPresent()) {
            return riviOidinMukaan.get();
        }
        Optional<ErillishakuRivi> riviHetunMukaan = kaikkiLisattavatTaiKeskeneraiset.stream().filter(r ->
            StringUtils.isNotBlank(r.getHenkilotunnus()) && r.getHenkilotunnus().equals(henkilo.getHetu())).findFirst();
        if (riviHetunMukaan.isPresent()) {
            ErillishakuRivi loytynyt = riviHetunMukaan.get();
            if (StringUtils.isNotBlank(loytynyt.getPersonOid()) && StringUtils.isNotBlank(henkilo.getOidHenkilo()) && !loytynyt.getPersonOid().equals(henkilo.getOidHenkilo())) {
                LOG.warn(String.format("Henkilölle %s hetun mukaan löytyneellä rivillä %s on eri henkilöoid (%s vs %s)", henkilo, loytynyt, henkilo.getOidHenkilo(), loytynyt.getPersonOid()));
            }
            return loytynyt;
        }
        Optional<ErillishakuRivi> riviSyntymaajanJaSukupuolenMukaan = kaikkiLisattavatTaiKeskeneraiset.stream().filter(r ->
            HakemusPrototyyppi.parseDate(r.parseSyntymaAika()).equals(HakemusPrototyyppi.parseDate(henkilo.getSyntymaaika())) &&
                r.getSukupuoli().equals(Sukupuoli.fromString(henkilo.getSukupuoli()))
        ).findFirst();
        if (riviSyntymaajanJaSukupuolenMukaan.isPresent()) {
            return riviSyntymaajanJaSukupuolenMukaan.get();
        }
        throw new HenkilonRivinPaattelyEpaonnistuiException("Ei löytynyt " + kaikkiLisattavatTaiKeskeneraiset.size() + " tuodusta rivistä henkilöä " + henkilo);
    }

    private HakemusPrototyyppi createHakemusprototyyppi(Henkilo henkilo, ErillishakuRivi rivi) {
        HakemusPrototyyppi hakemus = new HakemusPrototyyppi();
        hakemus.setAidinkieli(kielisyysToString(henkilo.getAidinkieli()));
        hakemus.setAsiointikieli(kielisyysToString(henkilo.getAsiointiKieli()));
        hakemus.setAsuinmaa(rivi.getAsuinmaa());
        hakemus.setEtunimi(henkilo.getEtunimet());
        hakemus.setHakijaOid(henkilo.getOidHenkilo());
        hakemus.setHenkilotunnus(henkilo.getHetu());
        hakemus.setKansalaisuus(rivi.getKansalaisuus());
        hakemus.setKotikunta(convertKuntaNimiToKuntaKoodi(rivi.getKotikunta()));
        hakemus.setOsoite(rivi.getOsoite());
        hakemus.setPostinumero(rivi.getPostinumero());
        hakemus.setPostitoimipaikka(rivi.getPostitoimipaikka());
        hakemus.setPuhelinnumero(rivi.getPuhelinnumero());
        hakemus.setSukunimi(henkilo.getSukunimi());
        hakemus.setSukupuoli(henkilo.getSukupuoli());
        hakemus.setSahkoposti(StringUtils.trimToEmpty(rivi.getSahkoposti()));
        hakemus.setSyntymaAika(henkilo.getSyntymaaika());
        hakemus.setToinenAstePohjakoulutusMaa(rivi.getPohjakoulutusMaaToinenAste());
        return hakemus;
    }

    private String kielisyysToString(Kielisyys kielisyys) {
        if(kielisyys == null) {
            return "";
        } else {
            return kielisyys.getKieliKoodi();
        }
    }

    private String convertKuntaNimiToKuntaKoodi(String nimi) {
        Map<String, Koodi> kuntaKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.KUNTA);
        return kuntaKoodit.values().stream().flatMap(koodi -> koodi.getMetadata().stream().map(metadata -> new ImmutablePair<>(metadata.getNimi(), koodi.getKoodiArvo())))
                .filter(x -> x.getLeft().equalsIgnoreCase(nimi))
                .map(ImmutablePair::getRight)
                .findFirst()
                .orElse(null);
    }

    private static boolean ainoastaanHakemuksenTilaPaivitys(ErillishaunHakijaDTO erillishakuRivi) {
        return erillishakuRivi.getValintatuloksenTila() == null && erillishakuRivi.getIlmoittautumisTila() == null;
    }

    private static VastaanottoResultDTO convertToHyvaksyttyResult(ErillishaunHakijaDTO hakija) {
        VastaanottoResultDTO resultDTO = new VastaanottoResultDTO();
        resultDTO.setHakemusOid(hakija.getHakemusOid());
        resultDTO.setHakukohdeOid(hakija.getHakukohdeOid());
        resultDTO.setHenkiloOid(hakija.getHakijaOid());
        resultDTO.setResult(new VastaanottoResultDTO.Result());
        resultDTO.getResult().setStatus(Response.Status.OK.getStatusCode());
        return resultDTO;
    }

    private void tuoErillishaunTilat(final String username, final ErillishakuDTO haku, final List<ErillishakuRivi> lisattavatTaiKeskeneraiset, final List<ErillishakuRivi> poistettavat,final List<Hakemus> hakemukset, final KirjeProsessi prosessi) {
        assert (hakemukset.size() == lisattavatTaiKeskeneraiset.size()); // 1-1 relationship assumed

        final List<ErillishaunHakijaDTO> hakijat = zip(hakemukset.stream(), lisattavatTaiKeskeneraiset.stream(), (hakemus, rivi) ->
                toErillishaunHakijaStream(haku, hakemus, rivi)).flatMap(s -> s).collect(Collectors.toList());

        final List<ErillishaunHakijaDTO> poistettavatDtos = poistettavat.stream()
                .map(rivi -> new ErillishaunHakijaDTO(
                        haku.getValintatapajonoOid(),
                        rivi.getHakemusOid(),
                        haku.getHakukohdeOid(),
                        rivi.isJulkaistaankoTiedot(),
                        rivi.getPersonOid(),
                        haku.getHakuOid(),
                        haku.getTarjoajaOid(),
                        null,
                        false,
                        null,
                        null,
                        rivi.getEtunimi(),
                        rivi.getSukunimi(),
                        Optional.of(true),
                        rivi.getHyvaksymiskirjeLahetetty()))
                .collect(Collectors.toList());
        try {
            if (!poistettavatDtos.isEmpty()) {
                List<String> hakemusOidit = poistettavatDtos.stream().map(ErillishaunHakijaDTO::getHakemusOid).collect(Collectors.toList());
                applicationAsyncResource.changeStateOfApplicationsToPassive(hakemusOidit, "Passivoitu erillishaun valintalaskennan käyttöliittymästä").toBlocking().first();
            }
            List<ErillishaunHakijaDTO> hakijatJaPoistettavat = new ArrayList<>();
            hakijatJaPoistettavat.addAll(hakijat);
            hakijatJaPoistettavat.addAll(poistettavatDtos);
            if (!hakijatJaPoistettavat.isEmpty()) {
                final List<ErillishaunHakijaDTO> hakijatAinastaanHakemuksenTilaPaivityksella = hakijat.stream().filter(h -> ainoastaanHakemuksenTilaPaivitys(h)).collect(Collectors.toList());
                final List<ErillishaunHakijaDTO> hakijatKaikillaTilaPaivityksilla = hakijat.stream().filter(h -> !ainoastaanHakemuksenTilaPaivitys(h)).collect(Collectors.toList());

                Observable<List<VastaanottoResultDTO>> vastaanottoTilojenTallennus =
                        doTilojenTallennusValintaTulosServiceen(username, hakijatKaikillaTilaPaivityksilla, prosessi)
                                .map(o -> {
                                    List<VastaanottoResultDTO> okResult = hakijatAinastaanHakemuksenTilaPaivityksella.stream().map(ErillishaunTuontiService::convertToHyvaksyttyResult).collect(Collectors.toList());
                                    return Lists.newArrayList(Iterables.concat(o, okResult));
                                });
                vastaanottoTilojenTallennus.flatMap(vastaanottoResponse -> {
                    List<VastaanottoResultDTO> epaonnistuneet = vastaanottoResponse.stream().filter(VastaanottoResultDTO::isFailed).collect(Collectors.toList());
                    epaonnistuneet.forEach(v -> LOG.warn(v.toString()));
                    if (epaonnistuneet.isEmpty()) {
                        return tilaAsyncResource.tuoErillishaunTilat(haku.getHakuOid(), haku.getHakukohdeOid(), haku.getValintatapajononNimi(), hakijatJaPoistettavat).doOnError(
                                e -> {
                                    LOG.error("Erillishaun tuonti epäonnistui", e);
                                    List<ValintatulosUpdateStatus> statuses = ((FailedHttpException) e).response.readEntity(HakukohteenValintatulosUpdateStatuses.class).statuses;
                                    prosessi.keskeyta(statuses.stream()
                                            .map(s -> new Poikkeus(Poikkeus.KOOSTEPALVELU, Poikkeus.SIJOITTELU,
                                                    s.message, new Tunniste(s.hakemusOid, Poikkeus.HAKEMUSOID)))
                                            .collect(Collectors.toList()));
                                });
                    } else {
                        List<Poikkeus> poikkeukset = epaonnistuneet.stream()
                                .map(v -> new Poikkeus(Poikkeus.KOOSTEPALVELU, Poikkeus.VALINTA_TULOS_SERVICE,
                                        v.getResult().getMessage(), new Tunniste(v.getHakemusOid(), Poikkeus.HAKEMUSOID)))
                                .collect(Collectors.toList());
                        prosessi.keskeyta(poikkeukset);
                        return Observable.error(new RuntimeException("Error when updating vastaanotto statuses"));
                    }
                }).subscribe(
                        done -> {
                            hakijatJaPoistettavat.forEach(h ->
                                    AUDIT.log(builder()
                                            .id(username)
                                            .hakuOid(haku.getHakuOid())
                                            .hakukohdeOid(haku.getHakukohdeOid())
                                            .hakemusOid(h.getHakemusOid())
                                            //.henkiloOid(h.getHakijaOid())
                                            .valintatapajonoOid(haku.getValintatapajonoOid())
                                            .setOperaatio(h.getPoistetaankoTulokset() ? ValintaperusteetOperation.ERILLISHAKU_TUONTI_HAKIJA_POISTO :
                                                    ValintaperusteetOperation.ERILLISHAKU_TUONTI_HAKIJA_PAIVITYS)
                                            .add("hakemuksentila", h.getHakemuksenTila())
                                            .add("valintatuloksentila", h.getValintatuloksenTila())
                                            .add("ilmoittautumistila", h.getIlmoittautumisTila())
                                            .build())
                            );
                            prosessi.vaiheValmistui();
                            prosessi.valmistui("ok");
                        },
                        poikkeus -> {
                            LOG.error("Erillishaun tilojen tuonti epäonnistui", poikkeus);
                        });
            } else {
                prosessi.vaiheValmistui();
                prosessi.valmistui("ok");
            }
        } catch (Exception e) {
            LOG.error("Erillishaun tilojen tuonti epaonnistui", e);
            throw new RuntimeException(e);
        }
    }

    private Observable<List<VastaanottoResultDTO>> doTilojenTallennusValintaTulosServiceen(String username, List<ErillishaunHakijaDTO> hakijat, final KirjeProsessi prosessi) {
        if(hakijat.isEmpty()) {
            return Observable.just(Collections.emptyList());
        } else {
            return valintaTulosServiceAsyncResource.tallenna(convertToValintaTulosList(hakijat, username, "Erillishaun tuonti")).doOnError(
                    e -> {
                        LOG.error("Virhe vastaanottotilojen tallennuksessa valinta-tulos-serviceen", e);
                        prosessi.keskeyta(new Poikkeus(Poikkeus.KOOSTEPALVELU, Poikkeus.VALINTA_TULOS_SERVICE, e.getMessage()));
                    });
        }
    }

    private Stream<ErillishaunHakijaDTO> toErillishaunHakijaStream(ErillishakuDTO haku, Hakemus hakemus, ErillishakuRivi rivi) {
        if (rivi.isKesken()) {
            return Stream.<ErillishaunHakijaDTO>empty(); // Keskeneräisiä ei viedä sijoitteluun
        } else {
            HakemusWrapper wrapper = new HakemusWrapper(hakemus);
            return Stream.of(new ErillishaunHakijaDTO(
                    haku.getValintatapajonoOid(),
                    hakemus.getOid(),
                    haku.getHakukohdeOid(),
                    rivi.isJulkaistaankoTiedot(),
                    hakemus.getPersonOid(),
                    haku.getHakuOid(),
                    haku.getTarjoajaOid(),
                    valintatuloksenTila(rivi),
                    rivi.getEhdollisestiHyvaksyttavissa(),
                    ilmoittautumisTila(rivi),
                    hakemuksenTila(rivi),
                    wrapper.getEtunimi(),
                    wrapper.getSukunimi(),
                    Optional.of(rivi.isPoistetaankoRivi()),
                    rivi.getHyvaksymiskirjeLahetetty()));
        }
    }

    private List<VastaanottoRecordDTO> convertToValintaTulosList(List<ErillishaunHakijaDTO> hakijatJaPoistettavat, String muokkaaja, String selite) {
        return hakijatJaPoistettavat.stream().map(erillishaunHakijaDTO ->
            VastaanottoRecordDTO.of(erillishaunHakijaDTO, muokkaaja, selite)).
            collect(Collectors.toList());
    }


    private static HakemuksenTila hakemuksenTila(ErillishakuRivi rivi) {
        return Optional.ofNullable(nullIfFails(() -> HakemuksenTila.valueOf(rivi.getHakemuksenTila()))).orElse(HakemuksenTila.HYLATTY);
    }

    private static IlmoittautumisTila ilmoittautumisTila(ErillishakuRivi rivi) {
        return nullIfFails(() -> IlmoittautumisTila.valueOf(rivi.getIlmoittautumisTila()));
    }

    private static ValintatuloksenTila valintatuloksenTila(ErillishakuRivi rivi) {
        return nullIfFails(() -> ValintatuloksenTila.valueOf(rivi.getVastaanottoTila()));
    }

    private static <T> T nullIfFails(Supplier<T> lambda) {
        try {
            return lambda.get();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return Validointivirhe tai null jos kaikki ok
     */
    private String validoi(Hakutyyppi tyyppi, ErillishakuRivi rivi) {
        // Yksilöinti onnistuu, eli joku kolmesta löytyy: henkilötunnus,syntymäaika+sukupuoli,henkilö-oid
        if (// mikään seuraavista ei ole totta:
                !(// on syntymaika+sukupuoli tunnistus
                (!isBlank(rivi.getSyntymaAika())
                && !Sukupuoli.EI_SUKUPUOLTA.equals(rivi.getSukupuoli()))
                || // on henkilotunnus
                        !isBlank(rivi.getHenkilotunnus()) ||
                        // on henkilo OID
                        !isBlank(rivi.getPersonOid()))) {
            return "Henkilötunnus, syntymäaika + sukupuoli ja henkilö-oid oli tyhjiä. Vähintään yksi tunniste on syötettävä. " + rivi.toString();
        }
        // Syntymäaika oikeassa formaatissa
        if(!isBlank(rivi.getSyntymaAika())) {
            try {
                ErillishakuRivi.SYNTYMAAIKAFORMAT.parseDateTime(rivi.getSyntymaAika());
            } catch(Exception e){
                return "Syntymäaika '" + rivi.getSyntymaAika() + "' on väärin muotoiltu. Syntymäaika on syötettävä muodossa pp.mm.vvvv. " + rivi.toString();
            }
        }
        // Jos vahvatunniste puuttuu niin nimet on pakollisia tietoja
        if(isBlank(rivi.getPersonOid())) {
            if (isBlank(rivi.getEtunimi()) || isBlank(rivi.getSukunimi())) {
                return "Etunimi ja sukunimi on pakollisia. " + rivi.toString();
            }
        }
        // Henkilötunnus on oikeassa formaatissa jos sellainen on syötetty
        if(!isBlank(rivi.getHenkilotunnus()) && !tarkistaHenkilotunnus(rivi.getHenkilotunnus())) {
            return "Henkilötunnus ("+rivi.getHenkilotunnus()+") on virheellinen. " + rivi.toString();
        }
        if (!"KESKEN".equalsIgnoreCase(rivi.getHakemuksenTila())) {
            ValintatuloksenTila vt = valintatuloksenTila(rivi);
            String tilaVirhe = ValidoiTilatUtil.validoi(hakemuksenTila(rivi), vt, ilmoittautumisTila(rivi));
            if (tilaVirhe != null) {
                return tilaVirhe + ". " + rivi.toString();
            }
        }
        if((isBlank(rivi.getPersonOid()) && isBlank(rivi.getHenkilotunnus())) && Sukupuoli.EI_SUKUPUOLTA.equals(rivi.getSukupuoli())) {
            return "Sukupuoli ("+rivi.getSukupuoli()+") on pakollinen kun henkilötunnus ja personOID puuttuu. " + rivi.toString();
        }

        if (isBlank(rivi.getHenkilotunnus()) &&
                isBlank(rivi.getPersonOid()) &&
                StringUtils.trimToEmpty(rivi.getAidinkieli()).isEmpty()) {
            return "Äidinkieli puuttuu. Äidinkieli on pakollinen tieto, kun henkilötunnus ja henkilö OID puuttuvat";
        }

        Map<String, Koodi> kieliKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.KIELI);
        if (! StringUtils.trimToEmpty(rivi.getAidinkieli()).isEmpty() &&
                ! kieliKoodit.keySet().contains(rivi.getAidinkieli().toUpperCase())) {
            return "Äidinkielen kielikoodi ("+rivi.getAidinkieli()+") on virheellinen." + rivi.toString();
        }

        if (!isBlank(rivi.getAsiointikieli()) && !ErillishakuDataRivi.ASIONTIKIELEN_ARVOT.contains(StringUtils.trimToEmpty(rivi.getAsiointikieli()).toLowerCase())) {
            return "Asiointikieli on virheellinen. Sallitut arvot ["+
                    StringUtils.join(ErillishakuDataRivi.ASIONTIKIELEN_ARVOT, '|') +
                    "] " + rivi.toString();
        }


        Map<String, Koodi> maaKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
        String asuinmaa = StringUtils.trimToEmpty(rivi.getAsuinmaa()).toUpperCase();
        if (!asuinmaa.isEmpty() && !maaKoodit.keySet().contains(asuinmaa)) {
            return "Asuinmaan maakoodi (" +  rivi.getAsuinmaa() + ") on virheellinen. " + rivi.toString();
        }

        String kansalaisuus = StringUtils.trimToEmpty(rivi.getKansalaisuus()).toUpperCase();
        if (! kansalaisuus.isEmpty() && !maaKoodit.keySet().contains(kansalaisuus)) {
            return "Kansalaisuuden maakoodi (" + rivi.getKansalaisuus() + ") on virheellinen. " + rivi.toString();
        }

        String pohjakoulutusMaaToinenAste = StringUtils.trimToEmpty(rivi.getPohjakoulutusMaaToinenAste()).toUpperCase();
        if (! pohjakoulutusMaaToinenAste.isEmpty() && !maaKoodit.keySet().contains(pohjakoulutusMaaToinenAste)) {
            return "Pohjakoulutuksen (toinen aste) maakoodi (" + rivi.getPohjakoulutusMaaToinenAste() + ") on virheellinen. " + rivi.toString();
        }

        String kotikunta = StringUtils.trimToEmpty(rivi.getKotikunta());
        if(!kotikunta.isEmpty()) {
            if (convertKuntaNimiToKuntaKoodi(kotikunta) == null) {
                return "Virheellinen kotikunta (" + rivi.getKotikunta() + "). " + rivi.toString();
            }
        }

        if (asuinmaa.equals(OsoiteHakemukseltaUtil.SUOMI)) {
            Map<String, Koodi> postiKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
            String postinumero = StringUtils.trimToEmpty(rivi.getPostinumero());
            if (!postinumero.isEmpty() && !postiKoodit.keySet().contains(postinumero)) {
                return "Virheellinen suomalainen postinumero (" + rivi.getPostinumero() + "). " + rivi.toString();
            }

            String postitoimipaikka = StringUtils.trimToEmpty(rivi.getPostitoimipaikka()).toUpperCase();

            if(!postitoimipaikka.isEmpty()) {
                boolean postitoimipaikkaKoodistossa = postiKoodit.values().stream()
                        .flatMap(x -> x.getMetadata().stream())
                        .map(Metadata::getNimi)
                        .anyMatch(x -> x.equalsIgnoreCase(postitoimipaikka));
                if (!postitoimipaikkaKoodistossa) {
                    return "Virheellinen suomalainen postitoimipaikka (" + rivi.getPostinumero() + "). " + rivi.toString();
                }

                if (!postinumero.isEmpty() &&
                        !postiKoodit.get(postinumero).getMetadata().stream().anyMatch(m -> m.getNimi().equalsIgnoreCase(postitoimipaikka))) {
                    return "Annettu suomalainen postinumero (" + rivi.getPostinumero() + ") ei vastaa annettua postitoimipaikkaa ("
                            + rivi.getPostitoimipaikka() + "). " + rivi.toString();
                }
            }
        }

        String puhelinnumero = StringUtils.trimToEmpty(rivi.getPuhelinnumero());
        if (! puhelinnumero.isEmpty() && !puhelinnumero.matches(PHONE_PATTERN)) {
            return "Virheellinen puhelinnumero (" + rivi.getPuhelinnumero() + "). " + rivi.toString();
        }

        return null;
    }

    public static class HenkilonRivinPaattelyEpaonnistuiException extends RuntimeException {
        private HenkilonRivinPaattelyEpaonnistuiException(String message) {
            super(message);
        }
    }
}

