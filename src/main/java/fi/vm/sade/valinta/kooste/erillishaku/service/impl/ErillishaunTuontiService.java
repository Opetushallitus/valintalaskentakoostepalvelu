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
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static rx.schedulers.Schedulers.newThread;

import com.google.common.collect.Lists;

import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.authentication.model.Kielisyys;
import fi.vm.sade.sijoittelu.domain.EhdollisenHyvaksymisenEhtoKoodi;
import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.http.FailedHttpException;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuDataRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRiviBuilder;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Sukupuoli;
import fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource;
import fi.vm.sade.valinta.kooste.erillishaku.util.ValidoiTilatUtil;
import fi.vm.sade.valinta.kooste.excel.ExcelValidointiPoikkeus;
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
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Valinnantulos;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValinnantulosUpdateStatus;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoRecordDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoResultDTO;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tunniste;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import org.apache.commons.lang.BooleanUtils;
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
import java.util.function.Consumer;
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

    public void tuoExcelistä(AuditSession auditSession, KirjeProsessi prosessi, ErillishakuDTO erillishaku, InputStream data) {
        tuoData(auditSession, prosessi, erillishaku, (haku) -> new ImportedErillisHakuExcel(haku.getHakutyyppi(), data), true);
    }

    public void tuoJson(AuditSession auditSession, KirjeProsessi prosessi, ErillishakuDTO erillishaku, List<ErillishakuRivi> erillishakuRivit, final boolean saveApplications) {
        tuoData(auditSession, prosessi, erillishaku, (haku) -> new ImportedErillisHakuExcel(erillishakuRivit), saveApplications);
    }

    private void tuoData(AuditSession auditSession, KirjeProsessi prosessi, ErillishakuDTO erillishaku, Function<ErillishakuDTO, ImportedErillisHakuExcel> importer, final boolean saveApplications) {
        Observable.just(erillishaku).subscribeOn(scheduler).subscribe(haku -> {
            final ImportedErillisHakuExcel erillishakuExcel;
            try {
                erillishakuExcel = importer.apply(haku);
                tuoHakijatJaLuoHakemukset(auditSession, prosessi, erillishakuExcel, saveApplications, haku);
            } catch(ErillishaunDataException dataException) {
                LOG.warn("excel ei validi:", dataException);
                prosessi.keskeyta(Poikkeus.koostepalvelupoikkeus(ErillishakuResource.POIKKEUS_VIALLINEN_DATAJOUKKO,
                        dataException.getPoikkeusRivit().stream().map(p -> new Tunniste("Rivi " + p.getIndeksi() + ": " + p.getSelite(),ErillishakuResource.RIVIN_TUNNISTE_KAYTTOLIITTYMAAN)).collect(Collectors.toList())));
            } catch(ExcelValidointiPoikkeus validointiPoikkeus) {
                LOG.warn("excel ei validi", validointiPoikkeus);
                prosessi.keskeyta(validointiPoikkeus.getMessage());
            } catch(Exception e) {
                LOG.error("unexpexted tuoData exception!", e);
                prosessi.keskeyta();
            }
        }, poikkeus -> {
            LOG.error("Erillishaun tuonti keskeytyi virheeseen", poikkeus);
            prosessi.keskeyta();
        }, () -> LOG.info("Tuonti lopetettiin"));
    }

    private void validoiRivit(final KirjeProsessi prosessi, final ErillishakuDTO haku, final List<ErillishakuRivi> rivit, final boolean saveApplications) {
        if (rivit.isEmpty()) {
            prosessi.keskeyta(ErillishakuResource.POIKKEUS_TYHJA_DATAJOUKKO);
            throw new RuntimeException(ErillishakuResource.POIKKEUS_TYHJA_DATAJOUKKO);
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
                        List<String> errors = validoi(haku.getHakutyyppi(), rivi, saveApplications);
                        if(errors.size() > 0) {
                            poikkeusRivis.add(new ErillishaunDataException.PoikkeusRivi(indeksi,  StringUtils.join(errors, " ") + " : " + rivi));
                        }
                    } else {
                        // validoi poistettavaksi merkitty rivi
                        if (rivi.getHakemusOid() == null) {
                            poikkeusRivis.add(new ErillishaunDataException.PoikkeusRivi(indeksi, "Poistettavaksi merkatulla riville ei löytynyt hakemuksen tunnistetta"));
                        }
                    }
                });
        if(!poikkeusRivis.isEmpty()) {
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
            if (VAIN_HAKEMUKSENTILALLISET_TILAT.contains(hakemuksenTila(rivi))
                    && !isUusi(rivi) && !ValintatuloksenTila.OTTANUT_VASTAAN_TOISEN_PAIKAN.name().equals(rivi.getVastaanottoTila())) {
                return ErillishakuRiviBuilder.fromRivi(rivi)
                        .vastaanottoTila("KESKEN")
                        .ilmoittautumisTila("EI_TEHTY")
                        .build();
            } else {
                return rivi;
                }
            }).collect(Collectors.toList());
        }

    private void tuoHakijatJaLuoHakemukset(final AuditSession auditSession, final KirjeProsessi prosessi, final ImportedErillisHakuExcel erillishakuExcel, final boolean saveApplications, final ErillishakuDTO haku) throws Exception {
        LOG.info("Aloitetaan tuonti. Rivit=" + erillishakuExcel.rivit.size());
        final List<ErillishakuRivi> rivit = autoTaytto(erillishakuExcel.rivit);

        validoiRivit(prosessi,haku,rivit,saveApplications);

        List<ErillishakuRivi> lisattavatTaiKeskeneraiset = rivit.stream()
                .filter(rivi -> !rivi.isPoistetaankoRivi()).collect(Collectors.toList());
        LOG.info("lisattavatTaiKeskeneraiset="+lisattavatTaiKeskeneraiset.size());
        List<ErillishakuRivi> poistettavat = rivit.stream()
                .filter(rivi -> rivi.isPoistetaankoRivi()).collect(Collectors.toList());
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
            lisattavatTaiKeskeneraiset = kasitteleHakemukset(haku, henkilot, lisattavatTaiKeskeneraiset, saveApplications, prosessi);
        }
        LOG.info("Viedaan hakijoita ({}kpl) jonoon {}", lisattavatTaiKeskeneraiset.size(), haku.getValintatapajonoOid());
        tuoErillishaunTilat(auditSession, haku, lisattavatTaiKeskeneraiset, poistettavat, prosessi);
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

    private List<ErillishakuRivi> kasitteleHakemukset(ErillishakuDTO haku, List<Henkilo> henkilot, List<ErillishakuRivi> lisattavatTaiKeskeneraiset, boolean saveApplications, KirjeProsessi prosessi) throws InterruptedException, ExecutionException {
        try {
            final List<ErillishakuRivi> rivitWithHenkiloData = henkilot.stream().map(h -> riviWithHenkiloData(h, lisattavatTaiKeskeneraiset)).collect(Collectors.toList());
            if(saveApplications) {
                List<HakemusPrototyyppi> hakemusPrototyypit = rivitWithHenkiloData.stream().map(rivi -> createHakemusprototyyppi(rivi)).collect(Collectors.toList());
                LOG.info("Tallennetaan hakemukset ({}kpl) hakemuspalveluun", lisattavatTaiKeskeneraiset.size());
                final List<Hakemus> hakemukset = applicationAsyncResource.putApplicationPrototypes(haku.getHakuOid(), haku.getHakukohdeOid(), haku.getTarjoajaOid(), hakemusPrototyypit).get();
                assert (hakemukset.size() == lisattavatTaiKeskeneraiset.size()); // 1-1 relationship assumed
                return zip(hakemukset.stream(), rivitWithHenkiloData.stream(), (hakemus, rivi) ->
                        rivi.withHakemusOid(hakemus.getOid())).collect(Collectors.toList());
            } else {
                return rivitWithHenkiloData;
            }
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

    private ErillishakuRivi riviWithHenkiloData(Henkilo henkilo, List<ErillishakuRivi> kaikkiLisattavatTaiKeskeneraiset) {
        ErillishakuRivi rivi = etsiHenkiloaVastaavaRivi(henkilo, kaikkiLisattavatTaiKeskeneraiset);
        return riviWithHenkiloData(henkilo, rivi);
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
        Optional<ErillishakuRivi> riviSyntymaajanJaSukupuolenMukaan = kaikkiLisattavatTaiKeskeneraiset.stream()
                .filter(r -> r.parseSyntymaAika() != null)
                .filter(r ->
                    HakemusPrototyyppi.parseDate(r.parseSyntymaAika()).equals(HakemusPrototyyppi.parseDate(henkilo.getSyntymaaika())) &&
                    r.getSukupuoli().equals(Sukupuoli.fromString(henkilo.getSukupuoli()))
        ).findFirst();
        if (riviSyntymaajanJaSukupuolenMukaan.isPresent()) {
            return riviSyntymaajanJaSukupuolenMukaan.get();
        }
        throw new HenkilonRivinPaattelyEpaonnistuiException("Ei löytynyt " + kaikkiLisattavatTaiKeskeneraiset.size() + " tuodusta rivistä henkilöä " + henkilo);
    }

    private ErillishakuRivi riviWithHenkiloData(Henkilo henkilo, ErillishakuRivi rivi) {
        String aidinkieli = kielisyysToString(henkilo.getAidinkieli());
        String asiointikieli = kielisyysToString(henkilo.getAsiointiKieli());
        String sukupuoli = henkilo.getSukupuoli();
        return ErillishakuRiviBuilder.fromRivi(rivi)
                .sukunimi(henkilo.getSukunimi())
                .etunimi(henkilo.getEtunimet())
                .henkilotunnus(henkilo.getHetu())
                .sahkoposti(StringUtils.trimToEmpty(rivi.getSahkoposti()))
                .syntymaAika(HakemusPrototyyppi.parseDate(henkilo.getSyntymaaika()))
                .sukupuoli(isNotBlank(sukupuoli) ? Sukupuoli.fromString(sukupuoli) : rivi.getSukupuoli())
                .personOid(henkilo.getOidHenkilo())
                .aidinkieli(isNotBlank(aidinkieli) ? aidinkieli : rivi.getAidinkieli())
                .asiointikieli(isNotBlank(asiointikieli) ? asiointikieli : rivi.getAsiointikieli())
                .build();
    }

    private HakemusPrototyyppi createHakemusprototyyppi(ErillishakuRivi rivi) {
        HakemusPrototyyppi hakemus = new HakemusPrototyyppi();
        hakemus.setAidinkieli(rivi.getAidinkieli());
        hakemus.setAsiointikieli(rivi.getAsiointikieli());
        hakemus.setAsuinmaa(rivi.getAsuinmaa());
        hakemus.setEtunimi(rivi.getEtunimi());
        hakemus.setHakijaOid(rivi.getPersonOid());
        hakemus.setHenkilotunnus(rivi.getHenkilotunnus());
        hakemus.setKansalaisuus(rivi.getKansalaisuus());
        hakemus.setKotikunta(convertKuntaNimiToKuntaKoodi(rivi.getKotikunta()));
        hakemus.setOsoite(rivi.getOsoite());
        hakemus.setPostinumero(rivi.getPostinumero());
        hakemus.setPostitoimipaikka(rivi.getPostitoimipaikka());
        hakemus.setPuhelinnumero(rivi.getPuhelinnumero());
        hakemus.setSukunimi(rivi.getSukunimi());
        hakemus.setSukupuoli(Sukupuoli.toHenkiloString(rivi.getSukupuoli()));
        hakemus.setSahkoposti(StringUtils.trimToEmpty(rivi.getSahkoposti()));
        hakemus.setSyntymaAika(rivi.getSyntymaAika());
        hakemus.setToisenAsteenSuoritus(rivi.getToisenAsteenSuoritus());
        hakemus.setToisenAsteenSuoritusmaa(rivi.getToisenAsteenSuoritusmaa());
        hakemus.setMaksuvelvollisuus(rivi.getMaksuvelvollisuus());
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

    private void tuoErillishaunTilat(final AuditSession auditSession, final ErillishakuDTO haku, final List<ErillishakuRivi> lisattavatTaiKeskeneraiset, final List<ErillishakuRivi> poistettavat, final KirjeProsessi prosessi) {
        final String username = auditSession.getPersonOid();

        final List<ErillishaunHakijaDTO> hakijat = lisattavatTaiKeskeneraiset.stream().flatMap(rivi ->
                toErillishaunHakijaStream(haku, rivi)).collect(Collectors.toList());

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
                        rivi.getHyvaksymiskirjeLahetetty(),
                        Lists.newArrayList(),
                        rivi.getEhdollisenHyvaksymisenEhtoKoodi(),
                        rivi.getEhdollisenHyvaksymisenEhtoFI(),
                        rivi.getEhdollisenHyvaksymisenEhtoSV(),
                        rivi.getEhdollisenHyvaksymisenEhtoEN()))
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
                final List<ErillishaunHakijaDTO> hakijatKaikillaTilaPaivityksilla = hakijat.stream()
                        .filter(h -> !ainoastaanHakemuksenTilaPaivitys(h) && !ValintatuloksenTila.OTTANUT_VASTAAN_TOISEN_PAIKAN.equals(h.valintatuloksenTila))
                        .collect(Collectors.toList());

                Observable<List<VastaanottoResultDTO>> vastaanottoTilojenTallennus =
                        doVastaanottoTilojenTallennusValintaTulosServiceen(username, hakijatKaikillaTilaPaivityksilla, prosessi);
                vastaanottoTilojenTallennus.flatMap(vastaanottoResponse -> {
                    List<VastaanottoResultDTO> epaonnistuneet = vastaanottoResponse.stream().filter(VastaanottoResultDTO::isFailed).collect(Collectors.toList());
                    epaonnistuneet.forEach(v -> LOG.warn(v.toString()));
                    if (epaonnistuneet.isEmpty()) {
                        return tilaAsyncResource.tuoErillishaunTilat(haku.getHakuOid(), haku.getHakukohdeOid(), hakijatJaPoistettavat).doOnError(
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
                            Supplier<List<Valinnantulos>> valinnantuloksetForValintaTulosService = () -> {
                                List<Valinnantulos> valinnantulokset = poistettavat.stream().flatMap(rivi ->
                                        toErillishaunHakijaStream(haku, rivi)).map(hakijaDTO -> Valinnantulos.of(hakijaDTO)).collect(Collectors.toList());
                                valinnantulokset.addAll(hakijat.stream().map(hakijaDTO -> Valinnantulos.of(hakijaDTO, ainoastaanHakemuksenTilaPaivitys(hakijaDTO))).collect(Collectors.toList()));
                                return valinnantulokset;
                            };

                            doValinnantilojenTallennusValintaTulosServiceen(auditSession, haku, valinnantuloksetForValintaTulosService.get(),(ok) -> {
                                prosessi.vaiheValmistui();
                                prosessi.valmistui(ok);
                            });
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

    private Observable<List<VastaanottoResultDTO>> doVastaanottoTilojenTallennusValintaTulosServiceen(String username, List<ErillishaunHakijaDTO> hakijat, final KirjeProsessi prosessi) {
        if(hakijat.isEmpty()) {
            return Observable.just(Collections.emptyList());
        } else {
            return valintaTulosServiceAsyncResource.tallenna(convertToValintaTulosList(hakijat, username, "Erillishaun tuonti")).doOnError(
                    e -> {
                        LOG.error("Virhe vastaanottotilojen tallennuksessa valinta-tulos-serviceen", e);
                        LOG.error("", e.getCause());
                        prosessi.keskeyta(new Poikkeus(Poikkeus.KOOSTEPALVELU, Poikkeus.VALINTA_TULOS_SERVICE, e.getMessage()));
                    });
        }
    }

    private void doValinnantilojenTallennusValintaTulosServiceen(final AuditSession auditSession, final ErillishakuDTO haku, List<Valinnantulos> valinnantulokset, Consumer<String> ready) {
        try {
            valintaTulosServiceAsyncResource.postErillishaunValinnantulokset(auditSession, haku.getValintatapajonoOid(), valinnantulokset).subscribe(
                    done -> {
                        if(done.isEmpty()) {
                            LOG.info("Erillishaun tulokset tallennettu onnistuneesti Valintarekisteriin.");
                        } else {
                            LOG.info("Saatiin 200 erillishaun tulosten tallennuksessa Valintarekisteriin, mutta kaikkien tulosten tallennus ei onnistunut: " +
                              String.join("\n", done.stream().map(status -> status.toString()).collect(Collectors.toList())));
                        }
                        ready.accept("ok");
                    },
                    poikkeus -> {
                        LOG.warn("Erillishaun tulosten tallennus Valintarekisteriin epäonnistui", poikkeus);
                        ready.accept("ok");
                    }
            );
        } catch(Exception e) {
            LOG.error("Erillishaun tulosten tallennus Valintarekisteriin epäonnistui", e);
            ready.accept("ok");
        }
    }

    private Stream<ErillishaunHakijaDTO> toErillishaunHakijaStream(ErillishakuDTO haku, ErillishakuRivi rivi) {
        return Stream.of(new ErillishaunHakijaDTO(
                haku.getValintatapajonoOid(),
                rivi.getHakemusOid(),
                haku.getHakukohdeOid(),
                rivi.isJulkaistaankoTiedot(),
                rivi.getPersonOid(),
                haku.getHakuOid(),
                haku.getTarjoajaOid(),
                valintatuloksenTila(rivi),
                rivi.getEhdollisestiHyvaksyttavissa(),
                ilmoittautumisTila(rivi),
                hakemuksenTila(rivi),
                rivi.getEtunimi(),
                rivi.getSukunimi(),
                Optional.of(rivi.isPoistetaankoRivi() || StringUtils.isBlank(rivi.getHakemuksenTila())),
                rivi.getHyvaksymiskirjeLahetetty(),
                Lists.newArrayList(),
                rivi.getEhdollisenHyvaksymisenEhtoKoodi(), rivi.getEhdollisenHyvaksymisenEhtoFI(), rivi.getEhdollisenHyvaksymisenEhtoSV(), rivi.getEhdollisenHyvaksymisenEhtoEN()));
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

    private List<String> validoi(Hakutyyppi tyyppi, ErillishakuRivi rivi, boolean saveApplications) {
        List<String> errors = new ArrayList<>();
        // Yksilöinti onnistuu, eli joku kolmesta löytyy: henkilötunnus,syntymäaika+sukupuoli,henkilö-oid
        if (// mikään seuraavista ei ole totta:
                !(// on syntymaika+sukupuoli tunnistus
                (!isBlank(rivi.getSyntymaAika())
                && !Sukupuoli.EI_SUKUPUOLTA.equals(rivi.getSukupuoli()))
                || // on henkilotunnus
                        !isBlank(rivi.getHenkilotunnus()) ||
                        // on henkilo OID
                        !isBlank(rivi.getPersonOid()))) {
            errors.add("Henkilötunnus, syntymäaika + sukupuoli ja henkilö-oid olivat tyhjiä (vähintään yksi tunniste on syötettävä).");
        }
        // Syntymäaika oikeassa formaatissa
        if(!isBlank(rivi.getSyntymaAika())) {
            try {
                ErillishakuRivi.SYNTYMAAIKAFORMAT.parseDateTime(rivi.getSyntymaAika());
            } catch(Exception e){
                errors.add("Syntymäaika '" + rivi.getSyntymaAika() + "' on väärin muotoiltu (syötettävä muodossa pp.mm.vvvv).");
            }
        }
        // Jos vahvatunniste puuttuu niin nimet on pakollisia tietoja
        if(isBlank(rivi.getPersonOid())) {
            if (isBlank(rivi.getEtunimi()) || isBlank(rivi.getSukunimi())) {
                errors.add("Etunimi ja sukunimi on pakollisia.");
            }
        }
        // Henkilötunnus on oikeassa formaatissa jos sellainen on syötetty
        if(!isBlank(rivi.getHenkilotunnus()) && !tarkistaHenkilotunnus(rivi.getHenkilotunnus())) {
            errors.add("Henkilötunnus ("+rivi.getHenkilotunnus()+") on virheellinen.");
        }
        if (!rivi.isJulkaistaankoTiedot() && !(ValintatuloksenTila.KESKEN.name().equals(rivi.getVastaanottoTila()) || StringUtils.isEmpty(rivi.getVastaanottoTila()))) {
            errors.add("Vastaanottotietoa ei voi päivittää jos valinta ei ole julkaistavissa tai vastaanottotieto ei ole kesken");
        }
        if (!"KESKEN".equalsIgnoreCase(rivi.getHakemuksenTila())) {
            ValintatuloksenTila vt = valintatuloksenTila(rivi);
            String tilaVirhe = ValidoiTilatUtil.validoi(hakemuksenTila(rivi), vt, ilmoittautumisTila(rivi));
            if (tilaVirhe != null) {
                errors.add(tilaVirhe + ".");
            }
        }
        if((isBlank(rivi.getPersonOid()) && isBlank(rivi.getHenkilotunnus())) && Sukupuoli.EI_SUKUPUOLTA.equals(rivi.getSukupuoli())) {
            errors.add("Sukupuoli ("+rivi.getSukupuoli()+") on pakollinen kun henkilötunnus ja personOID puuttuu.");
        }

        if (isBlank(rivi.getHenkilotunnus()) &&
                isBlank(rivi.getPersonOid()) &&
                StringUtils.trimToEmpty(rivi.getAidinkieli()).isEmpty()) {
            errors.add("Äidinkieli on pakollinen tieto, kun henkilötunnus ja henkilö OID puuttuvat.");
        }

        Map<String, Koodi> kieliKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.KIELI);
        if (! StringUtils.trimToEmpty(rivi.getAidinkieli()).isEmpty() &&
                ! kieliKoodit.keySet().contains(rivi.getAidinkieli().toUpperCase())) {
            errors.add("Äidinkielen kielikoodi ("+rivi.getAidinkieli()+") on virheellinen.");
        }

        if (!isBlank(rivi.getAsiointikieli()) && !ErillishakuDataRivi.ASIONTIKIELEN_ARVOT.contains(StringUtils.trimToEmpty(rivi.getAsiointikieli()).toLowerCase())) {
            errors.add("Asiointikieli (" + rivi.getAsiointikieli() + ") on virheellinen (sallitut arvot ["+
                    StringUtils.join(ErillishakuDataRivi.ASIONTIKIELEN_ARVOT, '|') +
                    "]).");
        }


        Map<String, Koodi> maaKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
        String asuinmaa = StringUtils.trimToEmpty(rivi.getAsuinmaa()).toUpperCase();
        if (!asuinmaa.isEmpty() && !maaKoodit.keySet().contains(asuinmaa)) {
            errors.add("Asuinmaan maakoodi (" +  rivi.getAsuinmaa() + ") on virheellinen.");
        }

        String kansalaisuus = StringUtils.trimToEmpty(rivi.getKansalaisuus()).toUpperCase();
        if (! kansalaisuus.isEmpty() && !maaKoodit.keySet().contains(kansalaisuus)) {
            errors.add("Kansalaisuuden maakoodi (" + rivi.getKansalaisuus() + ") on virheellinen.");
        }

        String toisenAsteenSuoritusmaa = StringUtils.trimToEmpty(rivi.getToisenAsteenSuoritusmaa()).toUpperCase();
        if (! toisenAsteenSuoritusmaa.isEmpty() && !maaKoodit.keySet().contains(toisenAsteenSuoritusmaa)) {
            errors.add("Toisen asteen pohjakoulutuksen suoritusmaan maakoodi (" + rivi.getToisenAsteenSuoritusmaa() + ") on virheellinen.");
        }

        String kotikunta = StringUtils.trimToEmpty(rivi.getKotikunta());
        if(!kotikunta.isEmpty()) {
            if (convertKuntaNimiToKuntaKoodi(kotikunta) == null) {
                errors.add("Virheellinen kotikunta (" + rivi.getKotikunta() + ").");
            }
        }

        if (asuinmaa.equals(OsoiteHakemukseltaUtil.SUOMI)) {
            Map<String, Koodi> postiKoodit = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
            String postinumero = StringUtils.trimToEmpty(rivi.getPostinumero());
            if (!postinumero.isEmpty() && !postiKoodit.keySet().contains(postinumero)) {
                errors.add("Virheellinen suomalainen postinumero (" + rivi.getPostinumero() + ").");
            }

            String postitoimipaikka = StringUtils.trimToEmpty(rivi.getPostitoimipaikka()).toUpperCase();

            if(!postitoimipaikka.isEmpty()) {
                boolean postitoimipaikkaKoodistossa = postiKoodit.values().stream()
                        .flatMap(x -> x.getMetadata().stream())
                        .map(Metadata::getNimi)
                        .anyMatch(x -> x.equalsIgnoreCase(postitoimipaikka));
                if (!postitoimipaikkaKoodistossa) {
                    errors.add("Virheellinen suomalainen postitoimipaikka (" + rivi.getPostinumero() + ").");
                }

                if (!postinumero.isEmpty() && postiKoodit.containsKey(postinumero) &&
                        !postiKoodit.get(postinumero).getMetadata().stream().anyMatch(m -> m.getNimi().equalsIgnoreCase(postitoimipaikka))) {
                    errors.add("Annettu suomalainen postinumero (" + rivi.getPostinumero() + ") ei vastaa annettua postitoimipaikkaa ("
                            + rivi.getPostitoimipaikka() + ").");
                }
            }
        }

        String puhelinnumero = StringUtils.trimToEmpty(rivi.getPuhelinnumero());
        if (! puhelinnumero.isEmpty() && !puhelinnumero.matches(PHONE_PATTERN)) {
            errors.add("Virheellinen puhelinnumero (" + rivi.getPuhelinnumero() + ").");
        }

        if (saveApplications && tyyppi == Hakutyyppi.KORKEAKOULU) {
            validateRequiredValue(asuinmaa, "asuinmaa", errors);
            validateRequiredValue(kansalaisuus, "kansalaisuus", errors);
            validateRequiredValue(kotikunta, "kotikunta", errors);

            Boolean toisenAsteenSuoritus = rivi.getToisenAsteenSuoritus();
            validateRequiredValue(ErillishakuDataRivi.getTotuusarvoString(toisenAsteenSuoritus), "toisen asteen suoritus", errors);
            if(BooleanUtils.isTrue(toisenAsteenSuoritus)) {
                validateRequiredValue(toisenAsteenSuoritusmaa, "toisen asteen pohjakoulutuksen maa", errors);
            } else if(StringUtils.isNotBlank(toisenAsteenSuoritusmaa)) {
                errors.add("Toisen asteen pohjakoulutuksen suoritusmaata (" + rivi.getToisenAsteenSuoritusmaa() + ") ei saa antaa, jos ei toisen asteen pohjakoulutusta ole suoritettu.");
            }
        }

        if (rivi.getEhdollisestiHyvaksyttavissa() && rivi.getEhdollisenHyvaksymisenEhtoKoodi() != null &&
                rivi.getEhdollisenHyvaksymisenEhtoKoodi().equals(EhdollisenHyvaksymisenEhtoKoodi.EHTO_MUU)) {
            if (StringUtils.isEmpty(rivi.getEhdollisenHyvaksymisenEhtoFI())) errors.add("Ehdollisen hyväksynnän ehto FI -kenttä oli tyhjä");
            if (StringUtils.isEmpty(rivi.getEhdollisenHyvaksymisenEhtoSV())) errors.add("Ehdollisen hyväksynnän ehto SV -kenttä oli tyhjä");
            if (StringUtils.isEmpty(rivi.getEhdollisenHyvaksymisenEhtoEN())) errors.add("Ehdollisen hyväksynnän ehto EN -kenttä oli tyhjä");
        }

        return errors;
    }

    private static void validateRequiredValue(String value, String name, List<String> errors) {
        if(StringUtils.isBlank(value)) {
            errors.add("Pakollinen tieto \"" + name + "\" puuttuu.");
        }
    }

    public static class HenkilonRivinPaattelyEpaonnistuiException extends RuntimeException {
        private HenkilonRivinPaattelyEpaonnistuiException(String message) {
            super(message);
        }
    }
}

