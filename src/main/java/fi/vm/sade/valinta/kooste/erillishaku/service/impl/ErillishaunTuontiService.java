package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import fi.vm.sade.authentication.model.Henkilo;
import static fi.vm.sade.valinta.kooste.converter.ValintatuloksenTilaHakuTyypinMukaanConverter.*;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuDataRivi;
import fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource;

import fi.vm.sade.valinta.kooste.exception.ErillishaunDataException;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tunniste;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codepoetics.protonpack.StreamUtils;
import com.google.gson.GsonBuilder;

import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_HAKEMUSPALVELUN_VIRHE;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_HENKILOPALVELUN_VIRHE;
import static fi.vm.sade.valinta.kooste.util.HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus;

/**
 * @author Jussi Jartamo
 */
@Service
public class ErillishaunTuontiService {

    private static final Logger LOG = LoggerFactory
            .getLogger(ErillishaunTuontiService.class);
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
        this(tilaAsyncResource,applicationAsyncResource,henkiloAsyncResource,Schedulers.newThread());
    }

    public void tuoExcelistä(KirjeProsessi prosessi, ErillishakuDTO erillishaku, InputStream data) {
        tuoData(prosessi, erillishaku, (haku) -> new ImportedErillisHakuExcel(haku.getHakutyyppi(), data));
    }

    public void tuoJson(KirjeProsessi prosessi, ErillishakuDTO erillishaku, List<ErillishakuRivi> erillishakuRivit) {
        tuoData(prosessi, erillishaku, (haku) -> new ImportedErillisHakuExcel(erillishaku.getHakutyyppi(), erillishakuRivit));
    }

    void tuoData(KirjeProsessi prosessi, ErillishakuDTO erillishaku, Function<ErillishakuDTO, ImportedErillisHakuExcel> importer) {
        Observable.just(erillishaku).subscribeOn(scheduler).subscribe(haku -> {
            final ImportedErillisHakuExcel erillishakuExcel;
            try {
                erillishakuExcel = importer.apply(haku);
                tuoHakijatJaLuoHakemukset(prosessi, erillishakuExcel, haku);
            } catch(Exception e) {
                LOG.error("Poikkeus {}", e.getMessage());
                prosessi.keskeyta();
            }
        }, poikkeus -> {
            LOG.error("Erillishaun tuonti keskeytyi virheeseen", poikkeus);
            prosessi.keskeyta();
        }, () -> {
            LOG.info("Tuonti onnistui");
        });
    }


    private void tuoHakijatJaLuoHakemukset(final KirjeProsessi prosessi, final ImportedErillisHakuExcel erillishakuExcel, final ErillishakuDTO haku) throws Exception {
        LOG.info("Aloitetaan tuonti");
        final List<ErillishakuRivi> rivit = erillishakuExcel.rivit;
        if (rivit.isEmpty()) {
            LOG.error("Syötteestä ei saatu poimittua yhtaan hakijaa sijoitteluun tuotavaksi!");
            prosessi.keskeyta(ErillishakuResource.POIKKEUS_TYHJA_DATAJOUKKO);
            throw new RuntimeException("Syötteestä ei saatu poimittua yhtaan hakijaa sijoitteluun tuotavaksi!");
        }
        Collection<ErillishaunDataException.PoikkeusRivi> poikkeusRivis = Lists.newArrayList();
        int indeksi = 0;
        for(ErillishakuRivi rivi : rivit) {
            ++indeksi;
            String validointiVirhe = validoi(haku.getHakutyyppi(), rivi);
            if(validointiVirhe != null) {
                poikkeusRivis.add(new ErillishaunDataException.PoikkeusRivi(indeksi, validointiVirhe));
            }
        }
        if(!poikkeusRivis.isEmpty()) {
            prosessi.keskeyta(Poikkeus.koostepalvelupoikkeus(ErillishakuResource.POIKKEUS_VIALLINEN_DATAJOUKKO,
                    poikkeusRivis.stream().map(p -> new Tunniste("Rivi " + p.getIndeksi() + ": " + p.getSelite(),ErillishakuResource.RIVIN_TUNNISTE_KAYTTOLIITTYMAAN)).collect(Collectors.toList())));
            throw new ErillishaunDataException(poikkeusRivis);
        }


        LOG.info("Haetaan/luodaan henkilöt");
        final List<Henkilo> henkilot;
        try {
            henkilot= henkiloAsyncResource.haeTaiLuoHenkilot(rivit.stream().map(rivi -> {
                return rivi.toHenkilo();
            }).collect(Collectors.toList())).get();
        }catch(Exception e) {
            LOG.error("{}: {} {}",POIKKEUS_HENKILOPALVELUN_VIRHE,e.getMessage(),Arrays.toString(e.getStackTrace()));
            prosessi.keskeyta(Poikkeus.henkilopalvelupoikkeus(POIKKEUS_HENKILOPALVELUN_VIRHE));
            throw e;
        }
        LOG.info("Käsitellään hakemukset");
        final List<Hakemus> hakemukset = kasitteleHakemukset(haku, henkilot, prosessi);

        LOG.info("Viedaan hakijoita {} jonoon {}", rivit.size(), haku.getValintatapajononNimi());
        tuoErillishaunTilat(haku, rivit, hakemukset);

        prosessi.vaiheValmistui();
        prosessi.valmistui("ok");
    }

    private List<Hakemus> kasitteleHakemukset(ErillishakuDTO haku, List<Henkilo> henkilot, KirjeProsessi prosessi) throws InterruptedException, ExecutionException {
        try {
            final List<HakemusPrototyyppi> hakemusPrototyypit = henkilot.stream()
                    .map(h -> {
                        //LOG.info("Hakija {}", new GsonBuilder().setPrettyPrinting().create().toJson(h));
                        return new HakemusPrototyyppi(h.getOidHenkilo(), h.getEtunimet(), h.getSukunimi(), h.getHetu(), h.getSyntymaaika());
                    }).collect(Collectors.toList());

            return applicationAsyncResource.putApplicationPrototypes(haku.getHakuOid(), haku.getHakukohdeOid(), haku.getTarjoajaOid(), hakemusPrototyypit).get();
        } catch (Throwable e) { // temporary catch to avoid missing service dependencies
            LOG.error("{}: {} {}",POIKKEUS_HAKEMUSPALVELUN_VIRHE,e.getMessage(),Arrays.toString(e.getStackTrace()));
            prosessi.keskeyta(Poikkeus.hakemuspalvelupoikkeus(POIKKEUS_HAKEMUSPALVELUN_VIRHE));
            throw e;
        }
    }

    private void tuoErillishaunTilat(final ErillishakuDTO haku, final List<ErillishakuRivi> rivit, final List<Hakemus> hakemukset) {
        assert(hakemukset.size() == rivit.size()); // 1-1 relationship assumed
        final List<ErillishaunHakijaDTO> hakijat = StreamUtils.zip(hakemukset.stream(), rivit.stream(), (hakemus, rivi) -> {
            HakemusWrapper wrapper = new HakemusWrapper(hakemus);
            return new ErillishaunHakijaDTO(haku.getValintatapajonoOid(), hakemus.getOid(), haku.getHakukohdeOid(), rivi.isJulkaistaankoTiedot(), hakemus.getPersonOid(), haku.getHakuOid(), haku.getTarjoajaOid(), valintatuloksenTila(rivi), ilmoittautumisTila(rivi), hakemuksenTila(rivi), wrapper.getEtunimi(), wrapper.getSukunimi());
        }).collect(Collectors.toList());

        try {
            tilaAsyncResource.tuoErillishaunTilat(haku.getHakuOid(), haku.getHakukohdeOid(), haku.getValintatapajononNimi(), hakijat);
        } catch (Exception e) {
            LOG.error("Erillishaun tilojen tuonti epaonnistui", e);
            throw new RuntimeException(e);
        }
    }

    private HakemuksenTila hakemuksenTila(ErillishakuRivi rivi) {
        return Optional.ofNullable(nullIfFails(() -> HakemuksenTila.valueOf(rivi.getHakemuksenTila()))).orElse(HakemuksenTila.HYLATTY);
    }

    private IlmoittautumisTila ilmoittautumisTila(ErillishakuRivi rivi) {
        return nullIfFails(() -> IlmoittautumisTila.valueOf(rivi.getIlmoittautumisTila()));
    }

    private ValintatuloksenTila valintatuloksenTila(ErillishakuRivi rivi) {
        return nullIfFails(() -> ValintatuloksenTila.valueOf(rivi.getVastaanottoTila()));
    }

    private <T> T nullIfFails(Supplier<T> lambda) {
        try {
            return lambda.get();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return Validointivirhe tai null jos kaikki ok
     */
    public String validoi(Hakutyyppi tyyppi, ErillishakuRivi rivi) {
        // Yksilöinti onnistuu, eli joku kolmesta löytyy: henkilötunnus,syntymäaika,henkilö-oid
        if(StringUtils.isBlank(rivi.getSyntymaAika())&&StringUtils.isBlank(rivi.getHenkilotunnus())&&StringUtils.isBlank(rivi.getPersonOid())) {
            return "Henkilötunnus, syntymäaika ja henkilö-oid oli tyhjiä. Vähintään yksi tunniste on syötettävä. " + rivi.toString();
        }
        // Syntymäaika oikeassa formaatissa
        if(!StringUtils.isBlank(rivi.getSyntymaAika())) {
            try {
                DateTime p = ErillishakuRivi.SYNTYMAAIKAFORMAT.parseDateTime(rivi.getSyntymaAika());
            } catch(Exception e){
                return "Syntymäaika '" + rivi.getSyntymaAika() + "' on väärin muotoiltu. Syntymäaika on syötettävä muodossa pp.mm.vvvv. " + rivi.toString();
            }
        }
        // Henkilölle on syötetty nimi
        if(StringUtils.isBlank(rivi.getEtunimi())&&StringUtils.isBlank(rivi.getSukunimi())) {
            return "Etunimi ja sukunimi on pakollisia. " + rivi.toString();
        }
        // Henkilötunnus on oikeassa formaatissa jos sellainen on syötetty
        if(!StringUtils.isBlank(rivi.getHenkilotunnus()) && !tarkistaHenkilotunnus(rivi.getHenkilotunnus())) {
            return "Henkilötunnus ("+rivi.getHenkilotunnus()+") on virheellinen. " + rivi.toString();
        }
        // Valintatuloksen tila on hakua vastaava
        ValintatuloksenTila vt = valintatuloksenTila(rivi);
        if(vt != null && convertValintatuloksenTilaHakuTyypinMukaan(vt, tyyppi) == null) {
            return "Valintatuloksen tila ("+vt+") on virheellinen. " + rivi.toString();
        }
        return null;
    }
}

