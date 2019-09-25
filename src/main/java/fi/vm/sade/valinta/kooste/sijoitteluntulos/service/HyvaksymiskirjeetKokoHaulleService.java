package fi.vm.sade.valinta.kooste.sijoitteluntulos.service;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables.HaunResurssit;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.HyvaksymiskirjeetServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class HyvaksymiskirjeetKokoHaulleService {

    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetKokoHaulleService.class);

    private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final AtaruAsyncResource ataruAsyncResource;
    private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
    private final OrganisaatioAsyncResource organisaatioAsyncResource;
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final HyvaksymiskirjeetServiceImpl hyvaksymiskirjeetServiceImpl;
    private final HakuParametritService hakuParametritService;
    private final TarjontaAsyncResource hakuV1AsyncResource;
    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    @Autowired
    private HyvaksymiskirjeetKokoHaulleService(
            HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
            ApplicationAsyncResource applicationAsyncResource,
            AtaruAsyncResource ataruAsyncResource,
            ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            ViestintapalveluAsyncResource viestintapalveluAsyncResource,
            HyvaksymiskirjeetServiceImpl hyvaksymiskirjeetServiceImpl,
            HakuParametritService hakuParametritService,
            TarjontaAsyncResource hakuV1AsyncResource,
            KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
        this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource = ataruAsyncResource;
        this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.hyvaksymiskirjeetServiceImpl = hyvaksymiskirjeetServiceImpl;
        this.hakuParametritService = hakuParametritService;
        this.hakuV1AsyncResource = hakuV1AsyncResource;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    }

    public void muodostaHyvaksymiskirjeetKokoHaulle(String hakuOid, String asiointikieli, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue) {
        muodostaHyvaksymiskirjeetKokoHaulle(() ->
                        ViestintapalveluObservables.haunResurssit(asiointikieli, valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid),
                                (oids) -> hakuV1AsyncResource.haeHaku(hakuOid)
                                        .flatMap(haku -> StringUtils.isEmpty(haku.getAtaruLomakeAvain())
                                                ? applicationAsyncResource.getApplicationsByhakemusOidsInParts(hakuOid, oids, ApplicationAsyncResource.DEFAULT_KEYS)
                                                : ataruAsyncResource.getApplicationsByOids(oids))),
                hakuOid, asiointikieli, prosessi, defaultValue);
    }

    private void muodostaHyvaksymiskirjeetKokoHaulle(Supplier<Observable<HaunResurssit>> haeHaunResurssit, String hakuOid, String asiointikieli, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue) {
        LOG.info("Aloitetaan haun {} hyväksymiskirjeiden luonti asiointikielelle {} hakemalla hyväksytyt koko haulle", hakuOid, prosessi.getAsiointikieli());

        haeHaunResurssit.get()
                .doOnError(error -> {
                    LOG.error("Ei saatu hakukohteen resursseja massahyväksymiskirjeitä varten hakuun {}", hakuOid, error);
                })
                .doOnNext(list -> prosessi.setKokonaistyo(1))
                .doOnNext(n -> LOG.info("Aloitetaan haun {} hyväksymiskirjeiden luonti", hakuOid))
                .flatMap(resurssit -> luoKirjeJaLahetaMuodostettavaksi(hakuOid, asiointikieli, resurssit, defaultValue.get())
                        .timeout(780, TimeUnit.MINUTES, Observable.just("timeout")))
                .subscribe(
                        batchId -> {
                            // TODO timeout handling
                            prosessi.setDokumenttiId(batchId);
                            prosessi.inkrementoi();
                            LOG.info("Haun hyväksymiskirjeet valmiit");
                        },
                        error -> {
                            LOG.error("Haun hyväksymiskirjeiden muodostaminen ei onnistunut", error);
                            prosessi.inkrementoiOhitettujaToita();
                            prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus("Hyväksymiskirjeiden muodostaminen ei onnistunut.\n" + error.getMessage()));
                        }
                );
    }

    private Observable<String> luoKirjeJaLahetaMuodostettavaksi(String hakuOid, String asiointikieli, HaunResurssit resurssit, String defaultValue) {

        Map<String, MetaHakukohde> hakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(resurssit.hakijat);
        ParametritParser haunParametrit = hakuParametritService.getParametritForHaku(hakuOid);

        Observable<LetterBatch> kirjeet = ViestintapalveluObservables.haunOsoitteet(asiointikieli, hakukohteet, organisaatioAsyncResource::haeHakutoimisto)
                .map(hakijapalveluidenOsoite -> HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                        koodistoCachedAsyncResource::haeKoodisto,
                        hakijapalveluidenOsoite,
                        hakukohteet,
                        resurssit.hakijat,
                        resurssit.hakemukset,
                        null,
                        hakuOid,
                        Optional.of(asiointikieli),
                        defaultValue,
                        hakuOid,
                        "hyvaksymiskirje",
                        hyvaksymiskirjeetServiceImpl.parsePalautusPvm(null, haunParametrit),
                        hyvaksymiskirjeetServiceImpl.parsePalautusAika(null, haunParametrit),
                        true,
                        true
                ));
        return ViestintapalveluObservables.batchId(
                kirjeet,
                viestintapalveluAsyncResource::viePdfJaOdotaReferenssiObservable,
                viestintapalveluAsyncResource::haeStatusObservable,
                780L,
                status -> Observable.just(status.batchId));
    }

    private boolean isKorkeakouluhaku(HakuV1RDTO haku) {
        return haku.getKohdejoukkoUri().startsWith("haunkohdejoukko_12"); //"kohdejoukkoUri": "haunkohdejoukko_12#1"
    }
}
