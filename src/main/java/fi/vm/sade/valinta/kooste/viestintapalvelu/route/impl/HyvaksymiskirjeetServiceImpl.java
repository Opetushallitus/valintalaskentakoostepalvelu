package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tunniste;
import fi.vm.sade.valinta.kooste.viestintapalvelu.Hakijapalvelu;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateDetail;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeetService;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource.VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Service
public class HyvaksymiskirjeetServiceImpl implements HyvaksymiskirjeetService {
    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetServiceImpl.class);
    private static final String VAKIOTEMPLATE = "default";
    private static final String VAKIODETAIL = "sisalto";

    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final AtaruAsyncResource ataruAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private final OrganisaatioAsyncResource organisaatioAsyncResource;
    private final HakuParametritService hakuParametritService;
    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
    private final DokumenttiAsyncResource dokumenttiAsyncResource;
    private final DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;
    private final KirjeetHakukohdeCache kirjeetHakukohdeCache;
    private final int pollingIntervalMillis;

    @Autowired
    public HyvaksymiskirjeetServiceImpl(
            ViestintapalveluAsyncResource viestintapalveluAsyncResource,
            ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            AtaruAsyncResource ataruAsyncResource,
            TarjontaAsyncResource tarjontaAsyncResource,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            HakuParametritService hakuParametritService,
            KoodistoCachedAsyncResource koodistoCachedAsyncResource,
            DokumenttiAsyncResource dokumenttiAsyncResource,
            DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti,
            KirjeetHakukohdeCache kirjeetHakukohdeCache,
            @Value("${valintalaskentakoostepalvelu.hyvaksymiskirjeet.polling.interval.millis:10000}") int pollingIntervalMillis) {
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource = ataruAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.hakuParametritService = hakuParametritService;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
        this.dokumenttiProsessiKomponentti = dokumenttiProsessiKomponentti;
        this.kirjeetHakukohdeCache = kirjeetHakukohdeCache;
        this.pollingIntervalMillis = pollingIntervalMillis;
    }

    @Override
    public ProsessiId hyvaksymiskirjeetHakemuksille(HyvaksymiskirjeDTO hyvaksymiskirjeDTO, List<String> hakemusOids) {
        String hakuOid = hyvaksymiskirjeDTO.getHakuOid();
        String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();
        LOG.info(String.format("Aloitetaan hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle", hakukohdeOid, hakemusOids.size()));

        CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF = hakemuksetByOids(hakuOid, hakemusOids);
        CompletableFuture<List<HakijaDTO>> hakijatF = hakijatByHakemusOids(hakuOid, hakemusOids)
                .thenComposeAsync(hakijat -> hakemuksetF.thenApplyAsync(hakemukset -> hyvaksytytHakijat(
                        hakijat,
                        hakemukset,
                        hakukohdeOid,
                        null,
                        hyvaksymiskirjeDTO.getVainTulosEmailinKieltaneet()
                )));
        return muodostaKirjeet(hakuParametritService.getParametritForHakuAsync(hakuOid), hakijatF, hakemuksetF, hyvaksymiskirjeDTO, null, false,
                String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle valmistui", hakukohdeOid, hakemusOids.size()),
                String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle epäonnistui", hakukohdeOid, hakemusOids.size()));
    }

    @Override
    public ProsessiId jalkiohjauskirjeHakukohteelle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
        String hakuOid = hyvaksymiskirjeDTO.getHakuOid();
        String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();
        LOG.info(String.format("Aloitetaan hakukohteen %s jälkiohjauskirjeiden muodostaminen", hakukohdeOid));

        CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF = hakemuksetByHakukohde(hakuOid, hakukohdeOid);
        CompletableFuture<List<HakijaDTO>> hakijatF = valintaTulosServiceAsyncResource.getKaikkiHakijat(hakuOid, hakukohdeOid)
                .thenApplyAsync(HyvaksymiskirjeetServiceImpl::hylatytHakijat);
        CompletableFuture<ParametritParser> haunParametritF = CompletableFuture.completedFuture(new ParametritParser(new ParametritDTO(), ""));
        return muodostaKirjeet(haunParametritF, hakijatF, hakemuksetF, hyvaksymiskirjeDTO, null, false,
                String.format("Hakukohteen %s jälkiohjauskirjeiden muodostaminen valmistui", hakukohdeOid),
                String.format("Hakukohteen %s jälkiohjauskirjeiden muodostaminen epäonnistui", hakukohdeOid));
    }

    @Override
    public ProsessiId hyvaksymiskirjeetHakukohteelle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
        String hakuOid = hyvaksymiskirjeDTO.getHakuOid();
        String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();
        LOG.info(String.format("Aloitetaan hakukohteen %s hyväksymiskirjeiden muodostaminen", hakukohdeOid));

        CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF = hakemuksetByHakukohde(hakuOid, hakukohdeOid);
        CompletableFuture<List<HakijaDTO>> hakijatF = valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid, hakukohdeOid)
                .thenComposeAsync(hakijat -> hakemuksetF.thenApplyAsync(hakemukset -> hyvaksytytHakijat(
                        hakijat,
                        hakemukset,
                        hakukohdeOid,
                        null,
                        hyvaksymiskirjeDTO.getVainTulosEmailinKieltaneet()
                )));
        return muodostaKirjeet(hakuParametritService.getParametritForHakuAsync(hakuOid), hakijatF, hakemuksetF, hyvaksymiskirjeDTO, null, false,
                String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen valmistui", hakukohdeOid),
                String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen epäonnistui", hakukohdeOid));
    }

    @Override
    public ProsessiId hyvaksymiskirjeetHaulle(String hakuOid, String asiointikieli, String defaultValue) {
        LOG.info(String.format("Aloitetaan haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s", hakuOid, asiointikieli));

        CompletableFuture<List<HakijaDTO>> kaikkiHakijatF = valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid);
        CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF = kaikkiHakijatF
                .thenComposeAsync(hakijat -> hakemuksetByOids(hakuOid, hakijat.stream()
                        .map(HakijaDTO::getHakemusOid)
                        .collect(Collectors.toList())));
        CompletableFuture<List<HakijaDTO>> hakijatF = kaikkiHakijatF
                .thenComposeAsync(hakijat -> hakemuksetF.thenApplyAsync(hakemukset -> hyvaksytytHakijat(
                        hakijat,
                        hakemukset,
                        null,
                        asiointikieli,
                        false
                )));
        HyvaksymiskirjeDTO hyvaksymiskirjeDTO = new HyvaksymiskirjeDTO(
                null,
                defaultValue,
                "hyvaksymiskirje",
                hakuOid,
                null,
                hakuOid,
                null,
                null,
                null,
                false
        );
        return muodostaKirjeet(hakuParametritService.getParametritForHakuAsync(hakuOid), hakijatF, hakemuksetF, hyvaksymiskirjeDTO, asiointikieli, true,
                String.format("Haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s valmistui", hakuOid, asiointikieli),
                String.format("Haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s epäonnistui", hakuOid, asiointikieli));
    }

    public ProsessiId hyvaksymiskirjeetHaulleHakukohteittain(String hakuOid, Optional<String> defaultValue) {
        LOG.info(String.format("Aloitetaan haun %s hyväksymiskirjeiden muodostaminen hakukohteittain", hakuOid));
        DokumenttiProsessi prosessi = new DokumenttiProsessi("hyvaksymiskirjeet", "Luo hyvaksymiskirjeet haulle", hakuOid, Arrays.asList("hyvaksymiskirjeet", "haulle"));
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);

        CompletableFuture<Map<String, Koodi>> maatjavaltiot1F = koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
        CompletableFuture<Map<String, Koodi>> postinumerotF = koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.POSTI);
        CompletableFuture<ParametritParser> haunParametritF = hakuParametritService.getParametritForHakuAsync(hakuOid);
        CompletableFuture<List<HakijaDTO>> hakijatF = valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid);
        CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF = hakijatF
                .thenComposeAsync(hakijat -> hakemuksetByOids(hakuOid, hakijat.stream()
                        .map(HakijaDTO::getHakemusOid)
                        .collect(Collectors.toList())));
        CompletableFuture<Map<String, MetaHakukohde>> hakukohteetF = hakijatF
                .thenComposeAsync(this::kiinnostavatHakukohteet);
        CompletableFuture<Map<String, Optional<Osoite>>> osoitteetF = hakukohteetF
                .thenComposeAsync(hakukohteet -> this.hakukohteidenHakutoimistojenOsoitteet(prosessi, hakukohteet, null));
        CompletableFuture.allOf(maatjavaltiot1F, postinumerotF, haunParametritF, hakijatF, hakemuksetF, hakukohteetF, osoitteetF)
                .thenComposeAsync(v -> {
                    List<String> hakukohteetJoissaHyvaksyttyja = hakijatF.join().stream()
                            .flatMap(hakija -> hakija.getHakutoiveet().stream())
                            .filter(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream().anyMatch(valintatapajono -> valintatapajono.getTila().isHyvaksytty()))
                            .map(HakutoiveDTO::getHakukohdeOid)
                            .distinct()
                            .collect(Collectors.toList());
                    prosessi.setKokonaistyo(hakukohteetJoissaHyvaksyttyja.size());
                    CompletableFuture<List<Pair<String, Optional<Throwable>>>> f = new CompletableFuture<>();
                    Disposable s = Observable.fromIterable(hakukohteetJoissaHyvaksyttyja)
                            .flatMap(hakukohdeOid -> Observable.fromFuture(this.haeHakukohteenVakiosisalto(defaultValue.orElse(null), hakukohdeOid))
                                            .map(vakiosisalto -> HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                                                    maatjavaltiot1F.join(),
                                                    postinumerotF.join(),
                                                    osoitteetF.join(),
                                                    hakukohteetF.join(),
                                                    hyvaksytytHakijat(hakijatF.join(), hakemuksetF.join(), hakukohdeOid, null, false),
                                                    hakemuksetF.join(),
                                                    hakukohdeOid,
                                                    hakuOid,
                                                    Optional.empty(),
                                                    vakiosisalto,
                                                    hakuOid,
                                                    "hyvaksymiskirje",
                                                    parsePalautusPvm(null, haunParametritF.join()),
                                                    parsePalautusAika(null, haunParametritF.join()),
                                                    false))
                                            .flatMap(letterBatch -> Observable.fromFuture(letterBatchToViestintapalvelu(prosessi, letterBatch)))
                                            .flatMap(batchId -> renameHakukohteenHyvaksymiskirjeet(prosessi, hakukohdeOid, batchId))
                                            .map(r -> Pair.of(hakukohdeOid, Optional.<Throwable>empty()))
                                            .doOnNext(p -> {
                                                LOG.info(String.format("Haun %s hakukohteen %s hyväksymiskirjeiden muodostaminen valmistui", hakuOid, hakukohdeOid));
                                                prosessi.inkrementoiTehtyjaToita();
                                            })
                                            .doOnError(e -> {
                                                LOG.error(String.format("Haun %s hakukohteen %s hyväksymiskirjeiden muodostaminen epäonnistui", hakuOid, hakukohdeOid), e);
                                                prosessi.inkrementoiOhitettujaToita();
                                            })
                                            .onErrorReturn(e -> Pair.of(hakukohdeOid, Optional.of(e))),
                                    6)
                            .toList()
                            .subscribe(f::complete, f::completeExceptionally);
                    f.whenComplete((l, e) -> s.dispose());
                    return f;
                })
                .whenComplete((result, error) -> {
                    if (result != null) {
                        List<Poikkeus> poikkeukset = result.stream()
                                .flatMap(r -> r.getRight().map(e -> Poikkeus.koostepalvelupoikkeus(e.getMessage(), Collections.singletonList(new Tunniste(r.getLeft(), Poikkeus.HAKUKOHDEOID)))).stream())
                                .collect(Collectors.toList());
                        if (poikkeukset.isEmpty()) {
                            LOG.info(String.format("Haun %s hyväksymiskirjeiden muodostaminen hakukohteittain valmistui", hakuOid));
                            prosessi.setDokumenttiId("valmistumisen-ilmaiseva-tunniste");
                        } else {
                            LOG.error(String.format("Haun %s hyväksymiskirjeiden muodostaminen hakukohteittain epäonnistui", hakuOid));
                            prosessi.getPoikkeukset().addAll(poikkeukset);
                        }
                    } else {
                        LOG.error(String.format("Haun %s hyväksymiskirjeiden muodostaminen hakukohteittain epäonnistui", hakuOid), error);
                        prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(error.getMessage()));
                    }
                });
        return prosessi.toProsessiId();
    }

    private CompletableFuture<List<HakijaDTO>> hakijatByHakemusOids(String hakuOid, List<String> hakemusOids) {
        List<CompletableFuture<HakijaDTO>> hakijaFs = hakemusOids.stream()
                .map(hakemusOid -> valintaTulosServiceAsyncResource.getHakijaByHakemus(hakuOid, hakemusOid))
                .collect(Collectors.toList());
        return CompletableFuture.allOf(hakijaFs.toArray(new CompletableFuture[] {}))
                .thenApplyAsync(v -> hakijaFs.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }

    private CompletableFuture<Map<String, HakemusWrapper>> hakemuksetByHakukohde(String hakuOid, String hakukohdeOid) {
        return tarjontaAsyncResource.haeHaku(hakuOid)
                .thenComposeAsync(haku -> haku.getAtaruLomakeAvain() == null
                        ? applicationAsyncResource.getApplicationsByOidsWithPOST(hakuOid, Collections.singletonList(hakukohdeOid))
                        : ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid))
                .thenApplyAsync(hakemukset -> hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h)));
    }

    private CompletableFuture<Map<String, HakemusWrapper>> hakemuksetByOids(String hakuOid, List<String> hakemusOids) {
        return tarjontaAsyncResource.haeHaku(hakuOid)
                .thenComposeAsync(haku -> haku.getAtaruLomakeAvain() == null
                        ? applicationAsyncResource.getApplicationsByhakemusOidsInParts(hakuOid, hakemusOids, ApplicationAsyncResource.DEFAULT_KEYS)
                        : ataruAsyncResource.getApplicationsByOids(hakemusOids))
                .thenApplyAsync(hakemukset -> hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h)));
    }

    private ProsessiId muodostaKirjeet(
            CompletableFuture<ParametritParser> haunParametritF,
            CompletableFuture<List<HakijaDTO>> hakijatF,
            CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF,
            HyvaksymiskirjeDTO hyvaksymiskirjeDTO,
            String asiointikieli,
            boolean sahkoinenKorkeakoulunMassaposti,
            String successMessage,
            String errorMessage
    ) {
        DokumenttiProsessi prosessi = new DokumenttiProsessi("", "", "", Collections.emptyList());
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
        prosessi.setKokonaistyo(1);

        CompletableFuture<Map<String, MetaHakukohde>> hakukohteetF = hakijatF.thenComposeAsync(this::kiinnostavatHakukohteet);
        CompletableFuture<Map<String, Koodi>> maatjavaltiot1F = koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
        CompletableFuture<Map<String, Koodi>> postinumerotF = koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.POSTI);
        CompletableFuture<Map<String, Optional<Osoite>>> osoitteetF = hakukohteetF.thenComposeAsync(hakukohteet -> this.hakukohteidenHakutoimistojenOsoitteet(prosessi, hakukohteet, asiointikieli));
        CompletableFuture<String> vakiosisaltoF = this.haeHakukohteenVakiosisalto(hyvaksymiskirjeDTO.getSisalto(), hyvaksymiskirjeDTO.getHakukohdeOid());
        CompletableFuture.allOf(maatjavaltiot1F, postinumerotF, haunParametritF, hakijatF, hakemuksetF, hakukohteetF, osoitteetF, vakiosisaltoF)
                .thenApplyAsync(v -> HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                        maatjavaltiot1F.join(),
                        postinumerotF.join(),
                        osoitteetF.join(),
                        hakukohteetF.join(),
                        hakijatF.join(),
                        hakemuksetF.join(),
                        hyvaksymiskirjeDTO.getHakukohdeOid(),
                        hyvaksymiskirjeDTO.getHakuOid(),
                        Optional.ofNullable(asiointikieli),
                        vakiosisaltoF.join(),
                        hyvaksymiskirjeDTO.getTag(),
                        hyvaksymiskirjeDTO.getTemplateName(),
                        parsePalautusPvm(hyvaksymiskirjeDTO.getPalautusPvm(), haunParametritF.join()),
                        parsePalautusAika(hyvaksymiskirjeDTO.getPalautusAika(), haunParametritF.join()),
                        sahkoinenKorkeakoulunMassaposti))
                .thenComposeAsync(letterBatch -> letterBatchToViestintapalvelu(prosessi, letterBatch))
                .whenComplete((batchId, e) -> {
                    if (batchId != null) {
                        LOG.info(successMessage);
                        prosessi.inkrementoiTehtyjaToita();
                        prosessi.setDokumenttiId(batchId);
                    } else {
                        LOG.error(errorMessage, e);
                        prosessi.inkrementoiOhitettujaToita();
                        prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                    }
                });
        return prosessi.toProsessiId();
    }

    private static List<HakijaDTO> hyvaksytytHakijat(List<HakijaDTO> hakijat, Map<String, HakemusWrapper> hakemukset, String hakukohdeJossaHyvaksytty, String asiointikieli, boolean vainTulosEmailinKieltaneet) {
        return hakijat.stream()
                .filter(hakija -> hakemukset.containsKey(hakija.getHakemusOid()))
                .filter(hakija -> !vainTulosEmailinKieltaneet || !hakemukset.get(hakija.getHakemusOid()).getLupaTulosEmail())
                .filter(hakija -> asiointikieli == null || asiointikieli.equalsIgnoreCase(hakemukset.get(hakija.getHakemusOid()).getAsiointikieli()))
                .filter(hakija -> hakija.getHakutoiveet().stream()
                        .filter(hakutoive -> hakukohdeJossaHyvaksytty == null || hakukohdeJossaHyvaksytty.equals(hakutoive.getHakukohdeOid()))
                        .flatMap(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream())
                        .anyMatch(valintatapajono -> valintatapajono.getTila().isHyvaksytty()))
                .collect(Collectors.toList());
    }

    private static List<HakijaDTO> hylatytHakijat(List<HakijaDTO> hakijat) {
        List<HakijaDTO> l = hakijat.stream()
                .filter(hakija -> hakija.getHakutoiveet().stream()
                        .flatMap(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream())
                        .noneMatch(valintatapajono -> valintatapajono.getTila().isHyvaksytty()))
                .collect(Collectors.toList());
        if (l.isEmpty()) {
            throw new RuntimeException("Ei hylättyjä hakijoita");
        }
        return l;
    }

    private CompletableFuture<Map<String, MetaHakukohde>> kiinnostavatHakukohteet(List<HakijaDTO> hakijat) {
        List<CompletableFuture<Pair<String, MetaHakukohde>>> hakukohdeFs = hakijat.stream()
                .flatMap(hakija -> hakija.getHakutoiveet().stream())
                .map(HakutoiveDTO::getHakukohdeOid)
                .distinct()
                .map(hakukohdeOid -> kirjeetHakukohdeCache.haeHakukohdeAsync(hakukohdeOid).thenApplyAsync(hakukohde -> Pair.of(hakukohdeOid, hakukohde)))
                .collect(Collectors.toList());
        return CompletableFuture.allOf(hakukohdeFs.toArray(new CompletableFuture[] {}))
                .thenApplyAsync(v -> hakukohdeFs.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));
    }

    private CompletableFuture<String> haeHakukohteenVakiosisalto(String annettuVakiosisalto, String hakukohdeOid) {
        if (annettuVakiosisalto != null) {
            return CompletableFuture.completedFuture(annettuVakiosisalto);
        }
        return this.tarjontaAsyncResource.haeHakukohde(hakukohdeOid)
                .thenComposeAsync(hakukohde -> viestintapalveluAsyncResource.haeKirjepohja(
                        hakukohde.getHakuOid(),
                        hakukohde.getTarjoajaOids().iterator().next(),
                        "hyvaksymiskirje",
                        KirjeetHakukohdeCache.getOpetuskieli(hakukohde.getOpetusKielet()),
                        hakukohde.getOid()
                ))
                .thenComposeAsync(kirjepohjat -> kirjepohjat.stream()
                        .filter(kirjepohja -> VAKIOTEMPLATE.equals(kirjepohja.getName()))
                        .flatMap(kirjepohja -> kirjepohja.getTemplateReplacements().stream())
                        .filter(tdd -> VAKIODETAIL.equals(tdd.getName()))
                        .map(TemplateDetail::getDefaultValue)
                        .map(CompletableFuture::completedFuture)
                        .findAny()
                        .orElse(CompletableFuture.failedFuture(new RuntimeException(String.format(
                                "Ei %s tai %s templateDetailia hakukohteelle %s",
                                VAKIOTEMPLATE,
                                VAKIODETAIL,
                                hakukohdeOid
                        )))));
    }

    private CompletableFuture<Map<String, Optional<Osoite>>> hakukohteidenHakutoimistojenOsoitteet(DokumenttiProsessi prosessi, Map<String, MetaHakukohde> hakukohteet, String asiointikieli) {
        if (prosessi.isKeskeytetty()) {
            return CompletableFuture.failedFuture(new RuntimeException("Kirjeiden muodostus keskeytetty"));
        }
        List<CompletableFuture<Pair<String, Optional<HakutoimistoDTO>>>> hakutoimistoFs = hakukohteet.values().stream().map(MetaHakukohde::getTarjoajaOid).distinct()
                .map(tarjoajaOid -> organisaatioAsyncResource.haeHakutoimisto(tarjoajaOid).thenApplyAsync(t -> Pair.of(tarjoajaOid, t)))
                .collect(Collectors.toList());
        return CompletableFuture.allOf(hakutoimistoFs.toArray(new CompletableFuture[]{}))
                .thenApplyAsync(v -> {
                    Map<String, Optional<HakutoimistoDTO>> hakutoimistot = hakutoimistoFs.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
                    return hakukohteet.entrySet().stream()
                            .map(e -> {
                                MetaHakukohde hakukohde = e.getValue();
                                String kieli = asiointikieli == null ? hakukohde.getHakukohteenKieli() : asiointikieli;
                                return Pair.of(
                                        e.getKey(),
                                        hakutoimistot.getOrDefault(hakukohde.getTarjoajaOid(), Optional.empty())
                                                .flatMap(t -> Hakijapalvelu.osoite(t, kieli))
                                );
                            })
                            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
                });
    }

    private CompletableFuture<String> letterBatchToViestintapalvelu(DokumenttiProsessi prosessi, LetterBatch letterBatch) {
        return vieLetterBatch(prosessi, letterBatch)
                .thenComposeAsync(batchId -> {
                    CompletableFuture<String> f = new CompletableFuture<>();
                    Disposable s = Observable.interval(pollingIntervalMillis, MILLISECONDS)
                            .flatMap(i -> letterBatchProcessingStatus(prosessi, batchId))
                            .firstOrError()
                            .timeout(VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA.toMillis(), MILLISECONDS)
                            .subscribe(b -> f.complete(batchId), f::completeExceptionally);
                    f.whenComplete((b, e) -> s.dispose());
                    return f;
                });
    }

    private CompletableFuture<String> vieLetterBatch(DokumenttiProsessi prosessi, LetterBatch letterBatch) {
        if (prosessi.isKeskeytetty()) {
            throw new RuntimeException("Kirjeiden muodostus keskeytetty");
        }
        return viestintapalveluAsyncResource.vieLetterBatch(letterBatch)
                .thenApplyAsync(response -> {
                    if (response.getStatus().equals(LetterResponse.STATUS_SUCCESS)) {
                        return response.getBatchId();
                    }
                    throw new RuntimeException(
                            response.getErrors().entrySet().stream()
                                    .map(e -> e.getKey() + ": " + e.getValue())
                                    .collect(Collectors.joining("\n"))
                    );
                });
    }

    private Observable<String> letterBatchProcessingStatus(DokumenttiProsessi prosessi, String batchId) {
        if (prosessi.isKeskeytetty()) {
            return Observable.error(new RuntimeException("Kirjeiden muodostus keskeytetty"));
        }
        return Observable.fromFuture(viestintapalveluAsyncResource.haeLetterBatchStatus(batchId))
                .flatMap(response -> {
                    if ("ready".equals(response.getStatus())) {
                        return Observable.just(batchId);
                    }
                    if ("error".equals(response.getStatus())) {
                        return Observable.error(new RuntimeException("Kirjeiden vienti viestintäpalveluun epäonnistui"));
                    }
                    return Observable.empty();
                });
    }

    private Observable<String> renameHakukohteenHyvaksymiskirjeet(DokumenttiProsessi prosessi, String hakukohdeOid, String batchId) {
        if (prosessi.isKeskeytetty()) {
            return Observable.error(new RuntimeException("Kirjeiden muodostus keskeytetty"));
        }
        return Observable.fromFuture(
                dokumenttiAsyncResource.uudelleenNimea(batchId, "hyvaksymiskirje_" + hakukohdeOid + ".pdf")
                        .thenApplyAsync(response -> batchId)
        );
    }

    private static String parsePalautusPvm(String specifiedPvm, ParametritParser haunParametrit) {
        if (StringUtils.trimToNull(specifiedPvm) == null && haunParametrit.opiskelijanPaikanVastaanottoPaattyy() != null) {
            return new SimpleDateFormat("dd.MM.yyyy").format(haunParametrit.opiskelijanPaikanVastaanottoPaattyy());
        }
        return specifiedPvm;
    }

    private static String parsePalautusAika(String specifiedAika, ParametritParser haunParametrit) {
        if (StringUtils.trimToNull(specifiedAika) == null && haunParametrit.opiskelijanPaikanVastaanottoPaattyy() != null) {
            return new SimpleDateFormat("HH.mm").format(haunParametrit.opiskelijanPaikanVastaanottoPaattyy());
        }
        return specifiedAika;
    }
}
