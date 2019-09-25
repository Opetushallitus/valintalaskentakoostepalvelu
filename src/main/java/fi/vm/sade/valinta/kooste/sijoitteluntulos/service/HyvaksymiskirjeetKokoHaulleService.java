package fi.vm.sade.valinta.kooste.sijoitteluntulos.service;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
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
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.Hakijapalvelu;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.HyvaksymiskirjeetServiceImpl;
import io.reactivex.Observable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    public void hyvaksymiskirjeetHaulle(String hakuOid, String asiointikieli, SijoittelunTulosProsessi prosessi, String defaultValue) {
        LOG.info("Aloitetaan haun {} hyväksymiskirjeiden luonti asiointikielelle {} hakemalla hyväksytyt koko haulle", hakuOid, prosessi.getAsiointikieli());
        prosessi.setKokonaistyo(1);
        valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid)
                .flatMap(valintatulokset -> hakuV1AsyncResource.haeHaku(hakuOid)
                        .flatMap(haku -> {
                            List<String> hakemusOids = valintatulokset.getResults().stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList());
                            return StringUtils.isEmpty(haku.getAtaruLomakeAvain())
                                    ? applicationAsyncResource.getApplicationsByhakemusOidsInParts(hakuOid, hakemusOids, ApplicationAsyncResource.DEFAULT_KEYS)
                                    : ataruAsyncResource.getApplicationsByOids(hakemusOids);
                        })
                        .flatMap(hakemukset -> {
                            List<HakemusWrapper> asiointikielisetHakemukset = hakemukset.stream()
                                    .filter(h -> asiointikieli.equalsIgnoreCase(h.getAsiointikieli()))
                                    .collect(Collectors.toList());
                            Set<String> asiointikielisetHakemusOids = asiointikielisetHakemukset.stream().map(HakemusWrapper::getOid).collect(Collectors.toSet());
                            List<HakijaDTO> asiointikielisetValintatulokset = valintatulokset.getResults().stream()
                                    .filter(v -> asiointikielisetHakemusOids.contains(v.getHakemusOid()))
                                    .collect(Collectors.toList());

                            Map<String, MetaHakukohde> hakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(asiointikielisetValintatulokset);
                            ParametritParser haunParametrit = hakuParametritService.getParametritForHaku(hakuOid);

                            return Observable.fromIterable(hakukohteet.values().stream().map(MetaHakukohde::getTarjoajaOid).distinct()::iterator)
                                    .flatMap(tarjoajaOid -> organisaatioAsyncResource.haeHakutoimisto(tarjoajaOid)
                                            .map(toimisto -> Pair.of(tarjoajaOid, toimisto.flatMap(t -> Hakijapalvelu.osoite(t, asiointikieli)))),
                                            1)
                                    .<Map<String, Optional<Osoite>>>collect(HashMap::new, (map, pair) -> map.put(pair.getLeft(), pair.getRight()))
                                    .toObservable()
                                    .map(hakijapalveluidenOsoite -> HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                                            koodistoCachedAsyncResource::haeKoodisto,
                                            hakijapalveluidenOsoite,
                                            hakukohteet,
                                            asiointikielisetValintatulokset,
                                            asiointikielisetHakemukset,
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
                        }))
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
                .timeout(780, TimeUnit.MINUTES, Observable.just("timeout"))
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

}
