package fi.vm.sade.valinta.kooste.sijoitteluntulos.service;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.Hakijapalvelu;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateDetail;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables.HakukohdeJaResurssit;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.HyvaksymiskirjeetServiceImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetHakukohdeCache;
import io.reactivex.Observable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
public class HyvaksymiskirjeetHaulleHakukohteittain {

    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetHaulleHakukohteittain.class);
    private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final AtaruAsyncResource ataruAsyncResource;
    private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private final DokumenttiAsyncResource dokumenttiAsyncResource;
    private final OrganisaatioAsyncResource organisaatioAsyncResource;
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final HyvaksymiskirjeetServiceImpl hyvaksymiskirjeetServiceImpl;
    private final HakuParametritService hakuParametritService;
    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    @Autowired
    public HyvaksymiskirjeetHaulleHakukohteittain(HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
                                                  ApplicationAsyncResource applicationAsyncResource,
                                                  AtaruAsyncResource ataruAsyncResource,
                                                  ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
                                                  TarjontaAsyncResource tarjontaAsyncResource,
                                                  DokumenttiAsyncResource dokumenttiAsyncResource,
                                                  OrganisaatioAsyncResource organisaatioAsyncResource,
                                                  ViestintapalveluAsyncResource viestintapalveluAsyncResource,
                                                  HyvaksymiskirjeetServiceImpl hyvaksymiskirjeetServiceImpl,
                                                  HakuParametritService hakuParametritService,
                                                  KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
        this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource = ataruAsyncResource;
        this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.hyvaksymiskirjeetServiceImpl = hyvaksymiskirjeetServiceImpl;
        this.hakuParametritService = hakuParametritService;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    }

    public void muodostaKirjeet(String hakuOid, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue) {
        Observable<List<HakukohdeJaResurssit>> hakukohdeJaResurssitObs =
                ViestintapalveluObservables.hakukohteetJaResurssit(valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid), (oids) ->
                    tarjontaAsyncResource.haeHaku(hakuOid)
                        .flatMap(haku -> StringUtils.isEmpty(haku.getAtaruLomakeAvain())
                                ? applicationAsyncResource.getApplicationsByhakemusOidsInParts(hakuOid, oids, ApplicationAsyncResource.DEFAULT_KEYS)
                                : ataruAsyncResource.getApplicationsByOids(oids)))
                        .doOnNext(list -> prosessi.setKokonaistyo(list.size()));

        hakukohdeJaResurssitObs.subscribe(
                list -> {
                    LOG.info("Hyväksyttyjä yhteensä {} hakukohteessa", list.size());
                    final ConcurrentLinkedQueue<HakukohdeJaResurssit> hakukohdeQueue = new ConcurrentLinkedQueue<>(list);
                    final boolean onkoTarveSplitata = list.size() > 20;
                    IntStream.range(0, onkoTarveSplitata ? 6 : 1).forEach(i -> hakukohdeKerralla(hakuOid, prosessi, defaultValue, hakukohdeQueue));
                },
                error -> {
                    LOG.error("Ei saatu hakukohteen resursseja massahyväksymiskirjeitä varten hakuun " + hakuOid, error);
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

                    prosessoiHakukohde(hakuOid, defaultValue, resurssit).subscribe(
                            s -> {
                                LOG.info("Hakukohde {} valmis", resurssit.hakukohdeOid);
                                prosessi.inkrementoi();
                                hakukohdeKerralla(hakuOid, prosessi, defaultValue, hakukohdeQueue);
                            },
                            e -> {
                                LOG.info("Hakukohde ohitettu virhe?" + resurssit.hakukohdeOid, e);
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

    private Observable<String> prosessoiHakukohde(String hakuOid, Optional<String> defaultValue, HakukohdeJaResurssit resurssit) {
        return tarjontaAsyncResource.haeHakukohde(resurssit.hakukohdeOid)
                .flatMap(h -> defaultValue.map(Observable::just).orElseGet(() -> haeHakukohteenVakiosisalto(h))
                        .flatMap(vakiosisalto -> luoKirjeJaLahetaMuodostettavaksi(
                                hakuOid,
                                resurssit.hakukohdeOid,
                                h.getTarjoajaOids().iterator().next(),
                                resurssit.hakijat,
                                resurssit.hakemukset,
                                vakiosisalto
                        )))
                .timeout(3, TimeUnit.MINUTES, Observable.just("timeout"));
    }

    private Observable<String> luoKirjeJaLahetaMuodostettavaksi(String hakuOid, String hakukohdeOid, String tarjoajaOid,
                                                                List<HakijaDTO> hyvaksytytHakijat, Collection<HakemusWrapper> hakemukset, String defaultValue) {
        LOG.info("##### Saatiin hakemukset hakukohteelle {}", hakukohdeOid);
        Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(hyvaksytytHakijat);
        ParametritParser haunParametrit = hakuParametritService.getParametritForHaku(hakuOid);

        MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hakukohdeOid);
        return organisaatioAsyncResource.haeHakutoimisto(kohdeHakukohde.getTarjoajaOid())
                .map(toimisto -> ImmutableMap.of(tarjoajaOid, toimisto.flatMap(t -> Hakijapalvelu.osoite(t, kohdeHakukohde.getHakukohteenKieli()))))
                .map(hakijapalveluidenOsoite -> HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                        koodistoCachedAsyncResource::haeKoodisto,
                        hakijapalveluidenOsoite,
                        hyvaksymiskirjeessaKaytetytHakukohteet,
                        hyvaksytytHakijat,
                        hakemukset,
                        null,
                        hakuOid,
                        Optional.empty(),
                        defaultValue,
                        hakuOid,
                        "hyvaksymiskirje",
                        hyvaksymiskirjeetServiceImpl.parsePalautusPvm(null, haunParametrit),
                        hyvaksymiskirjeetServiceImpl.parsePalautusAika(null, haunParametrit),
                        false,
                        false))
                .flatMap(viestintapalveluAsyncResource::viePdfJaOdotaReferenssiObservable)
                .flatMap(letterResponse -> Observable.interval(1, TimeUnit.SECONDS)
                        .flatMap(i -> viestintapalveluAsyncResource.haeStatusObservable(letterResponse.getBatchId())
                                .flatMap(letterBatchStatus -> {
                                    if ("error".equals(letterBatchStatus.getStatus())) {
                                        return Observable.error(new RuntimeException("Viestintäpalvelun statuspyyntö palautti virheen"));
                                    }
                                    if ("ready".equals(letterBatchStatus.getStatus())) {
                                        return Observable.just(letterResponse.getBatchId());
                                    }
                                    return Observable.empty();
                                }))
                        .take(1))
                .flatMap(batchId -> dokumenttiAsyncResource.uudelleenNimea(batchId, "hyvaksymiskirje_" + hakukohdeOid + ".pdf")
                        .onErrorReturn(error -> batchId)
                        .map(name -> batchId));
    }

    private Observable<String> haeHakukohteenVakiosisalto(HakukohdeV1RDTO hakukohde) {
        return viestintapalveluAsyncResource.haeKirjepohja(
                hakukohde.getHakuOid(),
                hakukohde.getTarjoajaOids().iterator().next(),
                "hyvaksymiskirje",
                KirjeetHakukohdeCache.getOpetuskieli(hakukohde.getOpetusKielet()),
                hakukohde.getOid()
        ).flatMap(kirjepohjat -> etsiVakioDetail(kirjepohjat)
                .map(TemplateDetail::getDefaultValue)
                .map(Observable::just)
                .orElse(Observable.error(new RuntimeException(String.format("Ei %s tai %s templateDetailia hakukohteelle %s", VAKIOTEMPLATE, VAKIODETAIL, hakukohde.getOid())))));
    }

    private static Optional<TemplateDetail> etsiVakioDetail(List<TemplateHistory> t) {
        return t.stream()
                .filter(th -> VAKIOTEMPLATE.equals(th.getName()))
                .flatMap(td -> td.getTemplateReplacements().stream().filter(tdd -> VAKIODETAIL.equals(tdd.getName())))
                .findAny();
    }

    private static final String VAKIOTEMPLATE = "default";
    private static final String VAKIODETAIL = "sisalto";
}
