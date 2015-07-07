package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import static com.codepoetics.protonpack.StreamUtils.*;
import static fi.vm.sade.valinta.kooste.converter.ValintatuloksenTilaHakuTyypinMukaanConverter.convertValintatuloksenTilaHakuTyypinMukaan;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_HAKEMUSPALVELUN_VIRHE;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_HENKILOPALVELUN_VIRHE;
import static fi.vm.sade.valinta.kooste.util.HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus;
import static org.apache.commons.lang.StringUtils.*;
import static rx.schedulers.Schedulers.newThread;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fi.vm.sade.authentication.model.Kielisyys;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuDataRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Sukupuoli;
import fi.vm.sade.valinta.kooste.erillishaku.util.ValidoiTilatUtil;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource;
import fi.vm.sade.valinta.kooste.exception.ErillishaunDataException;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tunniste;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import rx.Observable;
import rx.Scheduler;

@Service
public class ErillishaunTuontiService {
    private static final Logger LOG = LoggerFactory.getLogger(ErillishaunTuontiService.class);
    private final TilaAsyncResource tilaAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final HenkiloAsyncResource henkiloAsyncResource;
    private final Scheduler scheduler;

    public ErillishaunTuontiService(TilaAsyncResource tilaAsyncResource, ApplicationAsyncResource applicationAsyncResource, HenkiloAsyncResource henkiloAsyncResource, Scheduler scheduler) {
        this.applicationAsyncResource = applicationAsyncResource;
        this.tilaAsyncResource = tilaAsyncResource;
        this.henkiloAsyncResource = henkiloAsyncResource;
        this.scheduler = scheduler;
    }

    @Autowired
    public ErillishaunTuontiService(TilaAsyncResource tilaAsyncResource, ApplicationAsyncResource applicationAsyncResource, HenkiloAsyncResource henkiloAsyncResource) {
        this(tilaAsyncResource, applicationAsyncResource, henkiloAsyncResource, newThread());
    }

    public void tuoExcelistä(KirjeProsessi prosessi, ErillishakuDTO erillishaku, InputStream data) {
        tuoData(prosessi, erillishaku, (haku) -> new ImportedErillisHakuExcel(haku.getHakutyyppi(), data));
    }

    public void tuoJson(KirjeProsessi prosessi, ErillishakuDTO erillishaku, List<ErillishakuRivi> erillishakuRivit) {
        tuoData(prosessi, erillishaku, (haku) -> new ImportedErillisHakuExcel(erillishaku.getHakutyyppi(), erillishakuRivit));
    }

    private void tuoData(KirjeProsessi prosessi, ErillishakuDTO erillishaku, Function<ErillishakuDTO, ImportedErillisHakuExcel> importer) {
        Observable.just(erillishaku).subscribeOn(scheduler).subscribe(haku -> {
            final ImportedErillisHakuExcel erillishakuExcel;
            try {
                erillishakuExcel = importer.apply(haku);
                tuoHakijatJaLuoHakemukset(prosessi, erillishakuExcel, haku);
            } catch(Exception e) {
                LOG.error("tuoData exception!", e);
                prosessi.keskeyta();
            }
        }, poikkeus -> {
            LOG.error("Erillishaun tuonti keskeytyi virheeseen", poikkeus);
            prosessi.keskeyta();
        }, () -> {
            LOG.info("Tuonti lopetettiin");
        });
    }

    private static void validoiRivit(final KirjeProsessi prosessi, final ErillishakuDTO haku, final List<ErillishakuRivi> rivit) {
        if (rivit.isEmpty()) {
            LOG.error("Syötteestä ei saatu poimittua yhtaan hakijaa sijoitteluun tuotavaksi!");
            prosessi.keskeyta(ErillishakuResource.POIKKEUS_TYHJA_DATAJOUKKO);
            throw new RuntimeException("Syötteestä ei saatu poimittua yhtaan hakijaa sijoitteluun tuotavaksi!");
        }

        Collection<ErillishaunDataException.PoikkeusRivi> poikkeusRivis = Lists.newArrayList();
        zipWithIndex(rivit.stream()
                .map(rivi -> {
                    // AUTOTAYTTO VA
                    rivi.getHakemuksenTila();
                    return rivi;
                })).forEach(riviJaIndeksi -> {
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
            Arrays.asList(HakemuksenTila.PERUNUT, HakemuksenTila.PERUUTETTU, HakemuksenTila.HYLATTY,
            HakemuksenTila.VARALLA,HakemuksenTila.PERUUNTUNUT);

    private static List<ErillishakuRivi> autoTaytto(final List<ErillishakuRivi> rivit) {
        // jos hakemuksentila on hylatty tai varalla niin autotaytetaan loput tilat KESKEN, EI_TEHTY

        return rivit.stream().map(rivi -> {
            if (VAIN_HAKEMUKSENTILALLISET_TILAT.contains(hakemuksenTila(rivi))) {
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
                        "KESKEN", "EI_TEHTY",
                        rivi.isJulkaistaankoTiedot(), rivi.isPoistetaankoRivi());
            } else {
                return rivi;
                }
            }).collect(Collectors.toList());
        }

    private void tuoHakijatJaLuoHakemukset(final KirjeProsessi prosessi, final ImportedErillisHakuExcel erillishakuExcel, final ErillishakuDTO haku) throws Exception {
        LOG.info("Aloitetaan tuonti");
        final List<ErillishakuRivi> rivit = autoTaytto(erillishakuExcel.rivit);

        validoiRivit(prosessi,haku,rivit);

        List<ErillishakuRivi> lisattavatTaiKeskeneraiset = rivit.stream()
                .filter(rivi -> !rivi.isPoistetaankoRivi()).collect(Collectors.toList());

        List<ErillishakuRivi> poistettavat = rivit.stream()
                .filter(rivi -> !rivi.isKesken() && rivi.isPoistetaankoRivi()).collect(Collectors.toList());
        final List<Hakemus> hakemukset;
        if(!lisattavatTaiKeskeneraiset.isEmpty()) {
            LOG.info("Haetaan/luodaan henkilöt");
            final List<Henkilo> henkilot;
            try {
                henkilot = henkiloAsyncResource.haeTaiLuoHenkilot(lisattavatTaiKeskeneraiset.stream()
                        .map(rivi -> {
                            return rivi.toHenkiloCreateDTO();
                        }).collect(Collectors.toList())).get();
            } catch (Exception e) {
                LOG.error(POIKKEUS_HENKILOPALVELUN_VIRHE, e);
                prosessi.keskeyta(Poikkeus.henkilopalvelupoikkeus(POIKKEUS_HENKILOPALVELUN_VIRHE));
                throw e;
            }
            LOG.info("Käsitellään hakemukset ({}kpl)", lisattavatTaiKeskeneraiset.size());
            Map<String, String> sahkopostit = ImmutableMap.<String, String>builder()
                    .putAll(lisattavatTaiKeskeneraiset.stream().filter(rivi -> isNotBlank(rivi.getPersonOid())).collect(Collectors.toMap(rivi -> rivi.getPersonOid(), rivi -> rivi.getSahkoposti())))
                    .putAll(lisattavatTaiKeskeneraiset.stream().filter(rivi -> isNotBlank(rivi.getHenkilotunnus())).collect(Collectors.toMap(rivi -> rivi.getHenkilotunnus(), rivi -> rivi.getSahkoposti())))
                    .build();
            hakemukset = kasitteleHakemukset(haku, henkilot, sahkopostit, prosessi);
        } else {
            hakemukset = Collections.emptyList();
        }
        if(LOG.isInfoEnabled()) { // Count vie aikaa
            LOG.info("Viedaan hakijoita ohittaen rivit hakemuksentilalla kesken ({}/{}) jonoon {}", lisattavatTaiKeskeneraiset.stream().filter(r -> !r.isKesken()).count(), rivit.size(), haku.getValintatapajononNimi());
        }
        tuoErillishaunTilat(haku, lisattavatTaiKeskeneraiset, poistettavat, hakemukset);

        prosessi.vaiheValmistui();
        prosessi.valmistui("ok");
    }

    private List<Hakemus> kasitteleHakemukset(ErillishakuDTO haku, List<Henkilo> henkilot, Map<String, String> sahkopostit, KirjeProsessi prosessi) throws InterruptedException, ExecutionException {
        try {
            final List<HakemusPrototyyppi> hakemusPrototyypit = henkilot.stream()
                    .map(h -> {
                        //LOG.info("Hakija {}", new GsonBuilder().setPrettyPrinting().create().toJson(h));
                        return new HakemusPrototyyppi(h.getSukupuoli(), aidinkieliToString(h.getAidinkieli()), h.getOidHenkilo(), h.getEtunimet(), h.getSukunimi(), h.getHetu(), selectEmail(h, sahkopostit).orElse(""), h.getSyntymaaika());
                    }).collect(Collectors.toList());
            return applicationAsyncResource.putApplicationPrototypes(haku.getHakuOid(), haku.getHakukohdeOid(), haku.getTarjoajaOid(), hakemusPrototyypit).get();
        } catch (Throwable e) { // temporary catch to avoid missing service dependencies
            LOG.error(POIKKEUS_HAKEMUSPALVELUN_VIRHE, e);
            prosessi.keskeyta(Poikkeus.hakemuspalvelupoikkeus(POIKKEUS_HAKEMUSPALVELUN_VIRHE));
            throw e;
        }
    }
    private String aidinkieliToString(Kielisyys kielisyys) {
        if(kielisyys == null) {
            return "";
        } else {
            return kielisyys.getKieliKoodi();
        }
    }

    private static Optional<String> selectEmail(Henkilo henkilo, Map<String, String> sahkopostit) {
        Optional<String> email = Optional.ofNullable(sahkopostit.get(henkilo.getOidHenkilo()));
        if (email.isPresent()) {
            return email;
        } else {
            return Optional.ofNullable(sahkopostit.get(henkilo.getHetu()));
        }
    }

    private void tuoErillishaunTilat(final ErillishakuDTO haku, final List<ErillishakuRivi> lisattavatTaiKeskeneraiset, final List<ErillishakuRivi> poistettavat,final List<Hakemus> hakemukset) {
        final Stream<ErillishaunHakijaDTO> hakijat;
        final Stream<ErillishaunHakijaDTO> pois;
        if(!lisattavatTaiKeskeneraiset.isEmpty()){
            assert (hakemukset.size() == lisattavatTaiKeskeneraiset.size()); // 1-1 relationship assumed
            hakijat = zip(hakemukset.stream(), lisattavatTaiKeskeneraiset.stream(), (hakemus, rivi) -> {
                if (rivi.isKesken()) {
                    return Stream.<ErillishaunHakijaDTO>empty(); // Keskeneräisiä ei viedä sijoitteluun
                } else {
                    HakemusWrapper wrapper = new HakemusWrapper(hakemus);
                    return Stream.of(new ErillishaunHakijaDTO(haku.getValintatapajonoOid(), hakemus.getOid(), haku.getHakukohdeOid(),
                            rivi.isJulkaistaankoTiedot(), hakemus.getPersonOid(), haku.getHakuOid(),
                            haku.getTarjoajaOid(),
                            convertValintatuloksenTilaHakuTyypinMukaan(valintatuloksenTila(rivi), haku.getHakutyyppi()), ilmoittautumisTila(rivi),
                            hakemuksenTila(rivi), wrapper.getEtunimi(), wrapper.getSukunimi(), Optional.of(rivi.isPoistetaankoRivi())));
                }
            }).flatMap(s -> s);
        } else {
            hakijat = Stream.empty();
        }
        if(!poistettavat.isEmpty()) {
            pois = poistettavat.stream().map(rivi ->
                new ErillishaunHakijaDTO(
                       haku.getValintatapajonoOid(), rivi.getHakemusOid(), haku.getHakukohdeOid(), rivi.isJulkaistaankoTiedot(), rivi.getPersonOid(),
                       haku.getHakuOid(),  haku.getTarjoajaOid(), null,null,null, rivi.getEtunimi(), rivi.getSukunimi(),
                       Optional.of(true)));
        } else {
            pois = Stream.empty();
        }
        try {
            tilaAsyncResource.tuoErillishaunTilat(haku.getHakuOid(), haku.getHakukohdeOid(), haku.getValintatapajononNimi(),
                    Stream.concat(hakijat, pois).collect(Collectors.toList()));
        } catch (Exception e) {
            LOG.error("Erillishaun tilojen tuonti epaonnistui", e);
            throw new RuntimeException(e);
        }
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
    private static String validoi(Hakutyyppi tyyppi, ErillishakuRivi rivi) {
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
            // Valintatuloksen tila on hakua vastaava
            ValintatuloksenTila vt = valintatuloksenTila(rivi);
            ValintatuloksenTila vtc = convertValintatuloksenTilaHakuTyypinMukaan(vt, tyyppi);
            if (vt != null && vtc == null) {
                return "Valintatuloksen tila (" + vt + ") on virheellinen. " + rivi.toString();
            }
            String tilaVirhe = ValidoiTilatUtil.validoi(hakemuksenTila(rivi), vtc, ilmoittautumisTila(rivi));
            if (tilaVirhe != null) {
                return tilaVirhe + ". " + rivi.toString();
            }
        }
        if((isBlank(rivi.getPersonOid()) && isBlank(rivi.getHenkilotunnus())) && Sukupuoli.EI_SUKUPUOLTA.equals(rivi.getSukupuoli())) {
            return "Sukupuoli ("+rivi.getSukupuoli()+") on pakollinen kun henkilötunnus ja personOID puuttuu. " + rivi.toString();
        }
        if(isBlank(rivi.getPersonOid()) && !ErillishakuDataRivi.KIELITYYPIN_ARVOT.contains(StringUtils.trimToEmpty(rivi.getAidinkieli()).toLowerCase())) {
            return "Äidinkieli ("+rivi.getAidinkieli()+") on virheellinen. Lailliset arvot [" +
            "fi|en|sv|ae|lo|sl|bm|mo|nr|kn|ga|tl|la|nv|ti|gl|to|sa|lv|hi|ke|ty|ho|cv|ts|kj|xx|vo|ro|mr|sd|ak|kv|98|fj|su|sq|" +
            "ie|ab|ug|hr|my|hy|is|gd|ko|tg|am|bi|so|te|lg|dz|wo|az|oc|kl|kw|sk|uz|oj|ng|uk|gg|se|gu|ii|ne|ce|ee|ur|hu|mt|mg|je|zu|pa|sg|" +
                    "aa|ml|eu|bn|zh|rw|99|ha|nn|or|ta|ks|co|cr|mk|vi|io|lt|bo|ru|ik|ja|be|sc|ka|ay|he|xh|fy|dv|tn|eo|jv|sn|na|os|ln|rn|om|hz|rm|" +
                    "ss|et|bs|af|za|ve|ia|gv|st|mn|mi|fo|ri|gn|ku|es|as|ff|ig|da|av|ch|lb|tr|cy|el|li|ki|nb|lu|sm|no|tw|sw|mh|wa|tt|fr|de|km|fa|" +
                    "ht|kk|yo|ny|qu|ca|an|pt|yi|si|bg|cu|nd|ky|th|sr|ba|kr|ps|br|it|im|id|bh|iu|ar|pl|nl|ms|pi|tk|sh|cs|vk|kg] "
                    + rivi.toString();
        }
        return null;
    }
}

