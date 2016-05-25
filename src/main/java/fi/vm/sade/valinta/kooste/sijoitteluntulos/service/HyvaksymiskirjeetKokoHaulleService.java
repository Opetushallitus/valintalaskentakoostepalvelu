package fi.vm.sade.valinta.kooste.sijoitteluntulos.service;

import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables.HaunResurssit;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.HyvaksymiskirjeetServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class HyvaksymiskirjeetKokoHaulleService {

    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetKokoHaulleService.class);

    private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final SijoitteluAsyncResource sijoitteluAsyncResource;
    private final OrganisaatioAsyncResource organisaatioAsyncResource;
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final HyvaksymiskirjeetServiceImpl hyvaksymiskirjeetServiceImpl;
    private final HakuParametritService hakuParametritService;

    @Autowired
    private HyvaksymiskirjeetKokoHaulleService(
            HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
            ApplicationAsyncResource applicationAsyncResource,
            SijoitteluAsyncResource sijoitteluAsyncResource,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            ViestintapalveluAsyncResource viestintapalveluAsyncResource,
            HyvaksymiskirjeetServiceImpl hyvaksymiskirjeetServiceImpl, HakuParametritService hakuParametritService) {
        this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
        this.applicationAsyncResource = applicationAsyncResource;
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.hyvaksymiskirjeetServiceImpl = hyvaksymiskirjeetServiceImpl;
        this.hakuParametritService = hakuParametritService;
    }

    public void muodostaSahkopostiHyvaksymiskirjeetKokoHaulle(String hakuOid, String asiointikieli, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue) {
        muodostaHyvaksymiskirjeetKokoHaulle(() ->
                ViestintapalveluObservables.haunResurssitSahkoposti(asiointikieli, sijoitteluAsyncResource.getKoulutuspaikkalliset(hakuOid),
                        applicationAsyncResource::getApplicationsByHakemusOids), hakuOid, asiointikieli, prosessi, defaultValue);
    }

    public void muodostaIPostiHyvaksymiskirjeetKokoHaulle(String hakuOid, String asiointikieli, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue) {
        muodostaHyvaksymiskirjeetKokoHaulle(() ->
                ViestintapalveluObservables.haunResurssitIPosti(asiointikieli, sijoitteluAsyncResource.getKoulutuspaikkalliset(hakuOid),
                        (oids) -> applicationAsyncResource.getApplicationsByHakemusOids(oids, ApplicationAsyncResource.DEFAULT_KEYS)),
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
                        .timeout(ViestintapalveluObservables.getDelay(Optional.empty()), TimeUnit.MINUTES, Observable.just("")))
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

        Observable<Map<String, Optional<Osoite>>> osoitteet = ViestintapalveluObservables.haunOsoitteet(asiointikieli, hakukohteet, organisaatioAsyncResource::haeHakutoimisto);
        Observable<LetterBatch> kirjeet = ViestintapalveluObservables.kirjeet(hakuOid, Optional.of(asiointikieli), resurssit.hakijat, resurssit.hakemukset, defaultValue, hakukohteet, osoitteet,
                hyvaksymiskirjeetKomponentti, hyvaksymiskirjeetServiceImpl, haunParametrit);
        return ViestintapalveluObservables.batchId(
                kirjeet,
                viestintapalveluAsyncResource::viePdfJaOdotaReferenssiObservable,
                viestintapalveluAsyncResource::haeStatusObservable,
                ViestintapalveluObservables.getDelay(Optional.empty()),
                status -> Observable.just(status.batchId));
    }
}
