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
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.HyvaksymiskirjeetServiceImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetHakukohdeCache;
import io.reactivex.Observable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid)
                .flatMap(valintatulokset -> tarjontaAsyncResource.haeHaku(hakuOid)
                        .flatMap(haku -> {
                            List<String> hakemusOids = valintatulokset.getResults().stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList());
                            return StringUtils.isEmpty(haku.getAtaruLomakeAvain())
                                    ? applicationAsyncResource.getApplicationsByhakemusOidsInParts(hakuOid, hakemusOids, ApplicationAsyncResource.DEFAULT_KEYS)
                                    : ataruAsyncResource.getApplicationsByOids(hakemusOids);
                        })
                        .flatMap(hakemukset -> {
                            Map<String, HakemusWrapper> hakemuksetByOid = hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h));
                            Map<String, List<HakijaDTO>> valintatuloksetByHakukohdeOid = valintatulokset.getResults().stream()
                                    .collect(Collectors.groupingBy(hakija -> hakija.getHakutoiveet().stream()
                                            .filter(h -> h.getHakutoiveenValintatapajonot().stream().anyMatch(j -> j.getTila().isHyvaksytty()))
                                            .findAny()
                                            .get()
                                            .getHakukohdeOid()));
                            prosessi.setKokonaistyo(valintatuloksetByHakukohdeOid.size());
                            return Observable.fromIterable(valintatuloksetByHakukohdeOid.entrySet())
                                    .flatMap(hakukohteenValintatulokset -> {
                                        String hakukohdeOid = hakukohteenValintatulokset.getKey();
                                        List<HakijaDTO> hakukohteenHakijat = hakukohteenValintatulokset.getValue();
                                        List<HakemusWrapper> hakukohteenHakemukset = hakukohteenHakijat.stream().map(hakija -> hakemuksetByOid.get(hakija.getHakemusOid())).collect(Collectors.toList());
                                        return tarjontaAsyncResource.haeHakukohde(hakukohdeOid)
                                                .flatMap(h -> defaultValue.map(Observable::just).orElseGet(() -> haeHakukohteenVakiosisalto(h))
                                                        .flatMap(vakiosisalto -> luoKirjeJaLahetaMuodostettavaksi(
                                                                hakuOid,
                                                                hakukohdeOid,
                                                                h.getTarjoajaOids().iterator().next(),
                                                                hakukohteenHakijat,
                                                                hakukohteenHakemukset,
                                                                vakiosisalto
                                                        )))
                                                .map(v -> Pair.of(hakukohdeOid, Optional.<Throwable>empty()))
                                                .timeout(3, TimeUnit.MINUTES)
                                                .onErrorReturn(e -> Pair.of(hakukohdeOid, Optional.of(e)));
                                    }, 6);
                        }))
                .subscribe(
                        result -> {
                            result.getRight().ifPresentOrElse(e -> {
                                String msg = String.format("Hakukohteen %s kirjeen muodostaminen epäonnistui", result.getLeft());
                                LOG.error(msg, e);
                                prosessi.inkrementoiOhitettujaToita();
                                prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(msg + "\n" + e.getMessage()));
                            }, () -> {
                                LOG.info(String.format("Hakukohteen %s kirjeen muodostaminen onnistui", result.getLeft()));
                                prosessi.inkrementoi();
                            });
                        },
                        error -> {
                            String msg = String.format("Haun %s hyväksymiskirjeiden muodostaminen hakukohteittain epäonnistui", hakuOid);
                            LOG.error(msg, error);
                            prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(msg + "\n" + error.getMessage()));
                        },
                        () -> LOG.info(String.format("Haun %s hyväksymiskirjeiden muodostaminen hakukohteittain onnistui", hakuOid)));
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
