package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import fi.vm.sade.authentication.model.Henkilo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.GsonBuilder;

import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.service.ErillishaunTuontiService;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * @author Jussi Jartamo
 */
@Service
public class ErillishaunTuontiServiceImpl implements ErillishaunTuontiService {

    private static final Logger LOG = LoggerFactory
            .getLogger(ErillishaunTuontiServiceImpl.class);
    private final TilaAsyncResource tilaAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final HenkiloAsyncResource henkiloAsyncResource;

    @Autowired
    public ErillishaunTuontiServiceImpl(TilaAsyncResource tilaAsyncResource,
                                        ApplicationAsyncResource applicationAsyncResource,
                                        HenkiloAsyncResource henkiloAsyncResource) {
        this.applicationAsyncResource = applicationAsyncResource;
        this.tilaAsyncResource = tilaAsyncResource;
        this.henkiloAsyncResource = henkiloAsyncResource;
    }

    @Override
    public void tuo(KirjeProsessi prosessi, ErillishakuDTO erillishaku, InputStream data) {
        LOG.info("Aloitetaan tuonti");
        Observable.just(erillishaku).subscribeOn(Schedulers.newThread()).subscribe(haku -> {
            final Collection<ErillishaunHakijaDTO> hakijat = tuoHakijatJaLuoHakemukset(data, haku);
            LOG.info("Viedaan hakijoita {} jonoon {}", hakijat.size(), haku.getValintatapajononNimi());
            if (hakijat.isEmpty()) {
                throw new RuntimeException("Taulukkolaskentatiedostosta ei saatu poimittua yhtaan hakijaa sijoitteluun tuotavaksi!");
            }
            tuoErillishaunTilat(haku, hakijat);
            prosessi.vaiheValmistui();
            prosessi.valmistui("ok");
        }, poikkeus -> {
            if (poikkeus == null) {
                LOG.error("Suoritus keskeytyi tuntemattomaan NPE poikkeukseen!");
            } else {
                LOG.error("Erillishaun tuonti keskeytyi virheeseen", poikkeus);
            }
            prosessi.keskeyta();
        }, () -> {
            LOG.info("Tuonti onnistui");
        });
    }

    private void tuoErillishaunTilat(final ErillishakuDTO haku, final Collection<ErillishaunHakijaDTO> hakijat) {
        try {
            tilaAsyncResource.tuoErillishaunTilat(haku.getHakuOid(), haku.getHakukohdeOid(), haku.getValintatapajononNimi(), hakijat);
        } catch (Exception e) {
            LOG.error("Erillishaun tilojen tuonti epaonnistui", e);
            throw new RuntimeException(e);
        }
    }

    private Collection<ErillishaunHakijaDTO> tuoHakijatJaLuoHakemukset(final InputStream data, final ErillishakuDTO haku) {
        final Collection<ErillishaunHakijaDTO> hakijat;
        try {
            ImportedErillisHakuExcel erillishakuExcel = new ImportedErillisHakuExcel(haku.getHakutyyppi(), data);
            List<Hakemus> hakemukset = hakemukset(haku, erillishakuExcel);
            hakijat = hakemukset.stream()
                .map(hakemusToHakija(haku, erillishakuExcel.hetuToRivi)).collect(Collectors.toList());

        } catch (Throwable e) {
            LOG.error("Excelin tuonti epaonnistui", e);
            throw new RuntimeException(e);
        }
        return hakijat;
    }

    private List<Hakemus> hakemukset(ErillishakuDTO haku, ImportedErillisHakuExcel erillishakuExcel) throws InterruptedException, ExecutionException {
        try {
            LOG.info("Haetaan henkilöt ja käsitellään hakemukset");
            final List<HakemusPrototyyppi> hakemusPrototyypit = henkiloAsyncResource.haeTaiLuoHenkilot(erillishakuExcel.henkiloPrototyypit).get().stream()
                    .map(personToHakemusPrototyyppi).collect(Collectors.toList());
            return applicationAsyncResource.putApplicationPrototypes(haku.getHakuOid(), haku.getHakukohdeOid(), haku.getTarjoajaOid(), hakemusPrototyypit).get();
        } catch (ExecutionException e) { // temporary catch to avoid missing service dependencies
            LOG.warn("Fallback: käytetään hakemusten hakua oidien perusteella", e);
            return applicationAsyncResource.getApplicationsByOid(haku.getHakuOid(), haku.getHakukohdeOid()).get();
        }
    }

    private Function<Henkilo, HakemusPrototyyppi> personToHakemusPrototyyppi = h -> {
        LOG.info("Hakija {}", new GsonBuilder().setPrettyPrinting().create().toJson(h));
        return new HakemusPrototyyppi(h.getOidHenkilo(), h.getEtunimet(), h.getSukunimi(), h.getHetu(), null);
    };

    private Function<Hakemus, ErillishaunHakijaDTO> hakemusToHakija(final ErillishakuDTO erillishaku, final Map<String, ErillishakuRivi> hetuToRivi) {
        return hakemus -> {
            HakemusWrapper wrapper = new HakemusWrapper(hakemus);
            ErillishakuRivi rivi = hetuToRivi.get(wrapper.getHenkilotunnusTaiSyntymaaika());
            return new ErillishaunHakijaDTO(
                erillishaku.getValintatapajonoOid(),
                hakemus.getOid(),
                erillishaku.getHakukohdeOid(),
                rivi.isJulkaistaankoTiedot(),
                hakemus.getPersonOid(),
                erillishaku.getHakuOid(),
                erillishaku.getTarjoajaOid(),
                valintatuloksenTila(rivi),
                ilmoittautumisTila(rivi),
                hakemuksenTila(rivi),
                wrapper.getEtunimi(),
                wrapper.getSukunimi()
            );
        };
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
}

