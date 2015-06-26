package fi.vm.sade.valinta.kooste.sijoitteluntulos.service;

import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables.HaunResurssit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class HyvaksymiskirjeetKokoHaulleService {

    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetKokoHaulleService.class);

    private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final SijoitteluAsyncResource sijoitteluAsyncResource;
    private final DokumenttiAsyncResource dokumenttiAsyncResource;
    private final OrganisaatioAsyncResource organisaatioAsyncResource;
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;

    @Autowired
    private HyvaksymiskirjeetKokoHaulleService(
            HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
            ApplicationAsyncResource applicationAsyncResource,
            SijoitteluAsyncResource sijoitteluAsyncResource,
            DokumenttiAsyncResource dokumenttiAsyncResource,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            ViestintapalveluAsyncResource viestintapalveluAsyncResource) {
        this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
        this.applicationAsyncResource = applicationAsyncResource;
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
    }

    public void muodostaHyvaksymiskirjeetKokoHaulle(String hakuOid, String asiointikieli, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue) {
        LOG.info("Aloitetaan haun {} hyväksymiskirjeiden luonti asiointikielelle {} hakemalla hyväksytyt koko haulle", hakuOid, prosessi.getAsiointikieli());

        Observable<HaunResurssit> hakukohdeJaResurssitObs =
                ViestintapalveluObservables.haunResurssit(asiointikieli, sijoitteluAsyncResource.getKoulutuspaikkalliset(hakuOid), applicationAsyncResource::getApplicationsByHakemusOids)
                .doOnNext(list -> prosessi.setKokonaistyo(1));

        hakukohdeJaResurssitObs.subscribe(
                list -> hakuKerralla(hakuOid, asiointikieli, prosessi, defaultValue, list),
                error -> {
                    LOG.error("Ei saatu hakukohteen resursseja massahyväksymiskirjeitä varten hakuun {}", hakuOid, error);
                    prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus("Ei saatu hakukohteen resursseja massahyväksymiskirjeitä varten hakuun" + hakuOid + "\n" + error.getMessage()));
                    throw new RuntimeException(error);
                });
    }

    private void hakuKerralla(String hakuOid, String asiointikieli, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue, HaunResurssit resurssit) {
        LOG.info("Aloitetaan haun {} hyväksymiskirjeiden luonti", hakuOid);

        luoKirjeJaLahetaMuodostettavaksi(hakuOid, asiointikieli, resurssit, defaultValue.get(), prosessi)
                .timeout(ViestintapalveluObservables.getDelay(Optional.empty()), TimeUnit.MINUTES, Observable.just("timeout")).subscribe(
                s -> {
                    LOG.info("Haun hyväksymiskirje valmis");
                    prosessi.inkrementoi();
                },
                e -> {
                    LOG.error("Haun hyväksymiskirjeen muodostaminen ei onnistunut", e);
                    prosessi.inkrementoiOhitettujaToita();
                    prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus("Hyväksymiskirjeiden muodostaminen ei onnistunut.\n" + e.getMessage()));
                }
        );
    }

    private Observable<String> luoKirjeJaLahetaMuodostettavaksi(String hakuOid, String asiointikieli, HaunResurssit resurssit, String defaultValue, SijoittelunTulosProsessi prosessi) {

        try {
            Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(resurssit.hakijat);

            Observable<Map<String, Optional<Osoite>>> addresses = ViestintapalveluObservables.addresses(Optional.empty(), Optional.empty(), hyvaksymiskirjeessaKaytetytHakukohteet, organisaatioAsyncResource::haeHakutoimisto);
            Observable<LetterBatch> hyvaksymiskirje = ViestintapalveluObservables.kirje(hakuOid, Optional.of(asiointikieli), resurssit.hakijat, resurssit.hakemukset, defaultValue, hyvaksymiskirjeessaKaytetytHakukohteet, addresses, hyvaksymiskirjeetKomponentti);
            return ViestintapalveluObservables.batchId(Optional.empty(), prosessi, hyvaksymiskirje, viestintapalveluAsyncResource::viePdfJaOdotaReferenssiObservable, viestintapalveluAsyncResource::haeStatusObservable, batchId -> dokumenttiAsyncResource.uudelleenNimea(batchId, "hyvaksymiskirje_" + hakuOid + ".pdf"));

        } catch (Throwable error) {
            LOG.error("Viestintäpalveluviestin muodostus epäonnistui", error);
            return Observable.error(error);

        }
    }
}
