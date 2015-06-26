package fi.vm.sade.valinta.kooste.sijoitteluntulos.service;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateDetail;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables.HakukohdeJaResurssit;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetHakukohdeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
public class HyvaksymiskirjeetKokoHaulleService {

    private static final String VAKIOTEMPLATE = "default";
    private static final String VAKIODETAIL = "sisalto";
    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetKokoHaulleService.class);

    private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final SijoitteluAsyncResource sijoitteluAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private final DokumenttiAsyncResource dokumenttiAsyncResource;
    private final OrganisaatioAsyncResource organisaatioAsyncResource;
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;

    @Autowired
    private HyvaksymiskirjeetKokoHaulleService(
            HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
            ApplicationAsyncResource applicationAsyncResource,
            SijoitteluAsyncResource sijoitteluAsyncResource,
            TarjontaAsyncResource tarjontaAsyncResource,
            DokumenttiAsyncResource dokumenttiAsyncResource,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            ViestintapalveluAsyncResource viestintapalveluAsyncResource) {
        this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
        this.applicationAsyncResource = applicationAsyncResource;
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
    }

    private static Optional<TemplateDetail> etsiVakioDetail(List<TemplateHistory> t) {
        return t.stream()
                .filter(th -> VAKIOTEMPLATE.equals(th.getName()))
                .flatMap(td -> td.getTemplateReplacements().stream().filter(tdd -> VAKIODETAIL.equals(tdd.getName())))
                .findAny();
    }

    public void muodostaHyvaksymiskirjeetKokoHaulle(String hakuOid, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue) {
        LOG.info("Aloitetaan haun {} hyväksymiskirjeiden luonti asiointikielelle {} hakemalla hyväksytyt koko haulle", hakuOid, prosessi.getAsiointikieli());

        Observable<List<HakukohdeJaResurssit>> hakukohdeJaResurssitObs =
                ViestintapalveluObservables.hakukohteetJaResurssit(prosessi.getAsiointikieli(), sijoitteluAsyncResource.getKoulutuspaikkalliset(hakuOid), applicationAsyncResource::getApplicationsByHakemusOids)
                .doOnNext(list -> prosessi.setKokonaistyo(list.size()));

        hakukohdeJaResurssitObs.subscribe(
                list -> {
                    LOG.info("Hyväksyttyjä asiointikielellä {} on yhteensä {} hakukohteessa", prosessi.getAsiointikieli(), list.size());
                    final ConcurrentLinkedQueue<HakukohdeJaResurssit> hakukohdeQueue = new ConcurrentLinkedQueue<>(list);
                    final boolean onkoTarveSplitata = list.size() > 20;
                    IntStream.range(0, onkoTarveSplitata ? 2 : 1).forEach(i -> hakukohdeKerralla(hakuOid, prosessi, defaultValue, hakukohdeQueue));
                },
                error -> {
                    LOG.error("Ei saatu hakukohteen resursseja massahyväksymiskirjeitä varten hakuun {}", hakuOid, error);
                    prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus("Ei saatu hakukohteen resursseja massahyväksymiskirjeitä varten hakuun"
                            + hakuOid + "\n" + error.getMessage()));
                    throw new RuntimeException(error);
                });
    }

    private void hakukohdeKerralla(String hakuOid, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue, ConcurrentLinkedQueue<HakukohdeJaResurssit> hakukohdeQueue) {
        Optional<HakukohdeJaResurssit> hakukohdeJaResurssit = Optional.ofNullable(hakukohdeQueue.poll());
        hakukohdeJaResurssit.ifPresent(
                resurssit -> {
                    LOG.info("Aloitetaan hakukohteen {} hyväksymiskirjeiden luonti, jäljellä {} hakukohdetta", resurssit.hakukohdeOid, hakukohdeQueue.size());

                    prosessoiHakukohde(hakuOid, prosessi, defaultValue, hakukohdeJaResurssit, resurssit).subscribe(
                            s -> {
                                LOG.info("Hakukohde {} valmis", resurssit.hakukohdeOid);
                                prosessi.inkrementoi();
                                hakukohdeKerralla(hakuOid, prosessi, defaultValue, hakukohdeQueue);
                            },
                            e -> {
                                LOG.info("Hakukohde {} ohitettu", resurssit.hakukohdeOid, e);
                                prosessi.inkrementoiOhitettujaToita();
                                prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus("Hyväksymiskirjeiden muodostaminen ei onnistunut.\n" + e.getMessage()));
                                hakukohdeKerralla(hakuOid, prosessi, defaultValue, hakukohdeQueue);
                            }
                    );
                });
        if (!hakukohdeJaResurssit.isPresent()) {
            LOG.info("### Hyväksymiskirjeiden generointi haulle {} on valmis", hakuOid);

        }
    }

    private Observable<String> prosessoiHakukohde(String hakuOid, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue, Optional<HakukohdeJaResurssit> hakukohdeJaResurssit, HakukohdeJaResurssit resurssit) {
        return getHakukohteenHyvaksymiskirjeObservable(
                hakuOid,
                resurssit.hakukohdeOid,
                defaultValue,
                prosessi.getAsiointikieli(),
                resurssit.hakijat,
                resurssit.hakemukset, prosessi)
                .timeout(ViestintapalveluObservables.getDelay(hakukohdeJaResurssit.get().hakukohdeOid), TimeUnit.MINUTES, Observable.just("timeout"));
    }

    private Observable<String> getHakukohteenHyvaksymiskirjeObservable(String hakuOid, Optional<String> hakukohdeOid, Optional<String> defaultValue, Optional<String> asiointikieli, List<HakijaDTO> hyvaksytytHakijat, Collection<Hakemus> hakemukset,
                                                                  SijoittelunTulosProsessi prosessi) {
        if (!hakukohdeOid.isPresent()) {
            return luoKirjeJaLahetaMuodostettavaksi(hakuOid, Optional.empty(), Optional.empty(), asiointikieli, hyvaksytytHakijat, hakemukset, defaultValue.get(), prosessi);
        } else {
            return
                    tarjontaAsyncResource.haeHakukohde(hakukohdeOid.get()).switchMap(
                            h -> {
                                try {
                                    String tarjoajaOid = h.getTarjoajaOids().iterator().next();
                                    String kieli;
                                    if (asiointikieli.isPresent()) {
                                        kieli = asiointikieli.get();
                                    } else {
                                        kieli = KirjeetHakukohdeCache.getOpetuskieli(h.getOpetusKielet());
                                    }
                                    if (defaultValue.isPresent()) {
                                        return luoKirjeJaLahetaMuodostettavaksi(hakuOid, hakukohdeOid, Optional.of(tarjoajaOid),
                                                asiointikieli, hyvaksytytHakijat, hakemukset, defaultValue.get(), prosessi);
                                    } else {
                                        return viestintapalveluAsyncResource.haeKirjepohja(hakuOid, tarjoajaOid, "hyvaksymiskirje", kieli, hakukohdeOid.get()).switchMap(
                                                (t) -> {
                                                    Optional<TemplateDetail> td = etsiVakioDetail(t);
                                                    if (!td.isPresent()) {
                                                        return Observable.error(new RuntimeException("Ei " + VAKIOTEMPLATE + " tai " + VAKIODETAIL + " templateDetailia hakukohteelle " + hakukohdeOid));
                                                    } else {
                                                        return luoKirjeJaLahetaMuodostettavaksi(hakuOid, hakukohdeOid, Optional.of(tarjoajaOid),
                                                                asiointikieli, hyvaksytytHakijat, hakemukset, td.get().getDefaultValue(), prosessi);
                                                    }
                                                });
                                    }
                                } catch (Throwable e) {
                                    return Observable.error(e);
                                }
                            });
        }
    }

    private Observable<String> luoKirjeJaLahetaMuodostettavaksi(String hakuOid, Optional<String> hakukohdeOid, Optional<String> tarjoajaOid, Optional<String> asiointikieli,
                                                           List<HakijaDTO> hyvaksytytHakijat, Collection<Hakemus> hakemukset, String defaultValue, SijoittelunTulosProsessi prosessi) {

        try {
            LOG.info("##### Saatiin hakemukset hakukohteelle {}", hakukohdeOid);
            Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(hyvaksytytHakijat);

            Observable<Map<String, Optional<Osoite>>> addresses = ViestintapalveluObservables.addresses(hakukohdeOid, tarjoajaOid, hyvaksymiskirjeessaKaytetytHakukohteet, organisaatioAsyncResource.haeHakutoimisto(tarjoajaOid.get()));
            Observable<LetterBatch> hyvaksymiskirje = ViestintapalveluObservables.kirje(hakuOid, asiointikieli, hyvaksytytHakijat, hakemukset, defaultValue, hyvaksymiskirjeessaKaytetytHakukohteet, addresses, hyvaksymiskirjeetKomponentti);
            return ViestintapalveluObservables.batchId(hakukohdeOid, prosessi, hyvaksymiskirje, viestintapalveluAsyncResource::viePdfJaOdotaReferenssiObservable, viestintapalveluAsyncResource::haeStatusObservable, batchId -> dokumenttiAsyncResource.uudelleenNimea(batchId, "hyvaksymiskirje_" + hakukohdeOid.get() + ".pdf"));

        } catch (Throwable error) {
            LOG.error("Viestintäpalveluviestin muodostus epäonnistui hakukohteelle {}", hakukohdeOid, error);
            return Observable.error(error);

        }
    }
}
