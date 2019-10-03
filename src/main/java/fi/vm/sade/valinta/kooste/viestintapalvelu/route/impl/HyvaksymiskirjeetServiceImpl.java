package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
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

    private SimpleDateFormat pvmMuoto = new SimpleDateFormat("dd.MM.yyyy");
    private SimpleDateFormat kelloMuoto = new SimpleDateFormat("HH.mm");

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

        Observable<Map<String, HakemusWrapper>> hakemuksetF = hakemuksetByOids(hakuOid, hakemusOids);
        Observable<List<HakijaDTO>> hakijatF = hakijatByHakemusOids(hakuOid, hakemusOids)
                .flatMap(hakijat -> hakemuksetF.map(hakemukset -> hyvaksytytHakijat(
                        hakijat,
                        hakemukset,
                        hakukohdeOid,
                        null,
                        hyvaksymiskirjeDTO.getVainTulosEmailinKieltaneet()
                )));
        return muodostaKirjeet(haunParametrit(hakuOid), hakijatF, hakemuksetF, hyvaksymiskirjeDTO, null, false,
                String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle valmistui", hakukohdeOid, hakemusOids.size()),
                String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle epäonnistui", hakukohdeOid, hakemusOids.size()));
    }

    @Override
    public ProsessiId jalkiohjauskirjeHakukohteelle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
        String hakuOid = hyvaksymiskirjeDTO.getHakuOid();
        String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();
        LOG.info(String.format("Aloitetaan hakukohteen %s jälkiohjauskirjeiden muodostaminen", hakukohdeOid));

        Observable<Map<String, HakemusWrapper>> hakemuksetF = hakemuksetByHakukohde(hakuOid, hakukohdeOid);
        Observable<List<HakijaDTO>> hakijatF = hakijatByHakukohde(hakuOid, hakukohdeOid)
                .map(hakijat -> {
                    List<HakijaDTO> hylatyt = hakijat.stream()
                            .filter(HyvaksymiskirjeetServiceImpl::haussaHylatty)
                            .collect(Collectors.toList());
                    if (hylatyt.isEmpty()) {
                        LOG.error("Hakukohteessa {} ei ole jälkiohjattavia hakijoita!", hakukohdeOid);
                        throw new RuntimeException("Hakukohteessa " + hakukohdeOid + " ei ole jälkiohjattavia hakijoita!");
                    }
                    return hylatyt;
                });
        Observable<ParametritParser> haunParametritF = Observable.just(new ParametritParser(new ParametritDTO(), ""));
        return muodostaKirjeet(haunParametritF, hakijatF, hakemuksetF, hyvaksymiskirjeDTO, null, false,
                String.format("Hakukohteen %s jälkiohjauskirjeiden muodostaminen valmistui", hakukohdeOid),
                String.format("Hakukohteen %s jälkiohjauskirjeiden muodostaminen epäonnistui", hakukohdeOid));
    }

    @Override
    public ProsessiId hyvaksymiskirjeetHakukohteelle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
        String hakuOid = hyvaksymiskirjeDTO.getHakuOid();
        String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();
        LOG.info(String.format("Aloitetaan hakukohteen %s hyväksymiskirjeiden muodostaminen", hakukohdeOid));

        Observable<Map<String, HakemusWrapper>> hakemuksetF = hakemuksetByHakukohde(hakuOid, hakukohdeOid);
        Observable<List<HakijaDTO>> hakijatF = hyvaksytytByHakukohde(hakuOid, hakukohdeOid).flatMap(hakijat -> hakemuksetF.map(hakemukset -> hyvaksytytHakijat(
                hakijat,
                hakemukset,
                hakukohdeOid,
                null,
                hyvaksymiskirjeDTO.getVainTulosEmailinKieltaneet()
        )));
        return muodostaKirjeet(haunParametrit(hakuOid), hakijatF, hakemuksetF, hyvaksymiskirjeDTO, null, false,
                String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen valmistui", hakukohdeOid),
                String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen epäonnistui", hakukohdeOid));
    }

    @Override
    public ProsessiId hyvaksymiskirjeetHaulle(String hakuOid, String asiointikieli, String defaultValue) {
        LOG.info(String.format("Aloitetaan haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s", hakuOid, asiointikieli));

        Observable<List<HakijaDTO>> kaikkiHakijatF = hyvaksytytByHaku(hakuOid);
        Observable<Map<String, HakemusWrapper>> hakemuksetF = kaikkiHakijatF.flatMap(hakijat -> hakemuksetByOids(hakuOid, hakijat.stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList())));
        Observable<List<HakijaDTO>> hakijatF = kaikkiHakijatF.flatMap(hakijat -> hakemuksetF.map(hakemukset -> hyvaksytytHakijat(
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
        return muodostaKirjeet(haunParametrit(hakuOid), hakijatF, hakemuksetF, hyvaksymiskirjeDTO, asiointikieli, true,
                String.format("Haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s valmistui", hakuOid, asiointikieli),
                String.format("Haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s epäonnistui", hakuOid, asiointikieli));
    }

    public ProsessiId hyvaksymiskirjeetHaulleHakukohteittain(String hakuOid, Optional<String> defaultValue) {
        LOG.info(String.format("Aloitetaan haun %s hyväksymiskirjeiden muodostaminen hakukohteittain", hakuOid));
        DokumenttiProsessi prosessi = new DokumenttiProsessi("hyvaksymiskirjeet", "Luo hyvaksymiskirjeet haulle", hakuOid, Arrays.asList("hyvaksymiskirjeet", "haulle"));
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);

        Observable<List<HakijaDTO>> hakijatF = hyvaksytytByHaku(hakuOid);
        Observable<Map<String, HakemusWrapper>> hakemuksetF = hakijatF.flatMap(hakijat -> hakemuksetByOids(hakuOid, hakijat.stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList())));
        Observable<Map<String, MetaHakukohde>> hakukohteetF = hakijatF.flatMap(this::kiinnostavatHakukohteet);
        Observable<Map<String, Optional<Osoite>>> osoitteetF = hakukohteetF.flatMap(hakukohteet -> this.hakukohteidenHakutoimistojenOsoitteet(prosessi, hakukohteet, null));
        Observable.zip(
                Observable.fromFuture(koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1)),
                Observable.fromFuture(koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.POSTI)),
                haunParametrit(hakuOid),
                hakijatF,
                hakemuksetF,
                hakukohteetF,
                osoitteetF,
                (maatjavaltiot1, postinumerot, haunParametrit, hakijat, hakemukset, hakukohteet, osoitteet) -> {
                    List<String> hakukohteetJoissaHyvaksyttyja = hakijat.stream()
                            .flatMap(hakija -> hakija.getHakutoiveet().stream())
                            .filter(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream().anyMatch(valintatapajono -> valintatapajono.getTila().isHyvaksytty()))
                            .map(HakutoiveDTO::getHakukohdeOid)
                            .distinct()
                            .collect(Collectors.toList());
                    prosessi.setKokonaistyo(hakukohteetJoissaHyvaksyttyja.size());
                    return Observable.fromIterable(hakukohteetJoissaHyvaksyttyja)
                            .flatMap(hakukohdeOid -> Observable.fromFuture(this.haeHakukohteenVakiosisalto(defaultValue.orElse(null), hakukohdeOid))
                                            .map(vakiosisalto -> HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                                                    maatjavaltiot1,
                                                    postinumerot,
                                                    osoitteet,
                                                    hakukohteet,
                                                    hyvaksytytHakijat(hakijat, hakemukset, hakukohdeOid, null, false),
                                                    hakemukset,
                                                    hakukohdeOid,
                                                    hakuOid,
                                                    Optional.empty(),
                                                    vakiosisalto,
                                                    hakuOid,
                                                    "hyvaksymiskirje",
                                                    parsePalautusPvm(null, haunParametrit),
                                                    parsePalautusAika(null, haunParametrit),
                                                    false))
                                            .flatMap(letterBatch -> letterBatchToViestintapalvelu(prosessi, letterBatch))
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
                                    6);
                }
        ).flatMap(x -> x).toList().subscribe(
                result -> {
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
                },
                e -> {
                    LOG.error(String.format("Haun %s hyväksymiskirjeiden muodostaminen hakukohteittain epäonnistui", hakuOid), e);
                    prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                });
        return prosessi.toProsessiId();
    }

    private Observable<ParametritParser> haunParametrit(String hakuOid) {
        return Observable.fromFuture(hakuParametritService.getParametritForHakuAsync(hakuOid));
    }

    private Observable<List<HakijaDTO>> hyvaksytytByHaku(String hakuOid) {
        return Observable.fromFuture(valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid));
    }

    private Observable<List<HakijaDTO>> hyvaksytytByHakukohde(String hakuOid, String hakukohdeOid) {
        return Observable.fromFuture(valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid, hakukohdeOid));
    }

    private Observable<List<HakijaDTO>> hakijatByHakukohde(String hakuOid, String hakukohdeOid) {
        return Observable.fromFuture(valintaTulosServiceAsyncResource.getKaikkiHakijat(hakuOid, hakukohdeOid));
    }

    private Observable<List<HakijaDTO>> hakijatByHakemusOids(String hakuOid, List<String> hakemusOids) {
        return Observable.fromIterable(hakemusOids)
                .flatMap(hakemusOid -> Observable.fromFuture(valintaTulosServiceAsyncResource.getHakijaByHakemus(hakuOid, hakemusOid)))
                .toList()
                .toObservable();
    }

    private Observable<Map<String, HakemusWrapper>> hakemuksetByHakukohde(String hakuOid, String hakukohdeOid) {
        return Observable.fromFuture(
                tarjontaAsyncResource.haeHaku(hakuOid)
                        .thenCompose(haku -> haku.getAtaruLomakeAvain() == null
                                ? applicationAsyncResource.getApplicationsByOidsWithPOST(hakuOid, Collections.singletonList(hakukohdeOid))
                                : ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid))
                        .thenApply(hakemukset -> hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h)))
        );
    }

    private Observable<Map<String, HakemusWrapper>> hakemuksetByOids(String hakuOid, List<String> hakemusOids) {
        return Observable.fromFuture(
                tarjontaAsyncResource.haeHaku(hakuOid)
                        .thenCompose(haku -> haku.getAtaruLomakeAvain() == null
                                ? applicationAsyncResource.getApplicationsByhakemusOidsInParts(hakuOid, hakemusOids, ApplicationAsyncResource.DEFAULT_KEYS)
                                : ataruAsyncResource.getApplicationsByOids(hakemusOids))
                        .thenApply(hakemukset -> hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h)))
        );
    }

    private ProsessiId muodostaKirjeet(
            Observable<ParametritParser> haunParametritF,
            Observable<List<HakijaDTO>> hakijatF,
            Observable<Map<String, HakemusWrapper>> hakemuksetF,
            HyvaksymiskirjeDTO hyvaksymiskirjeDTO,
            String asiointikieli,
            boolean sahkoinenKorkeakoulunMassaposti,
            String successMessage,
            String errorMessage
    ) {
        DokumenttiProsessi prosessi = new DokumenttiProsessi("", "", "", Collections.emptyList());
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
        prosessi.setKokonaistyo(1);

        Observable<Map<String, MetaHakukohde>> hakukohteetF = hakijatF.flatMap(this::kiinnostavatHakukohteet);
        Observable.zip(
                Observable.fromFuture(koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1)),
                Observable.fromFuture(koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.POSTI)),
                haunParametritF,
                hakijatF,
                hakemuksetF,
                hakukohteetF,
                hakukohteetF.flatMap(hakukohteet -> this.hakukohteidenHakutoimistojenOsoitteet(prosessi, hakukohteet, asiointikieli)),
                Observable.fromFuture(this.haeHakukohteenVakiosisalto(hyvaksymiskirjeDTO.getSisalto(), hyvaksymiskirjeDTO.getHakukohdeOid())),
                (maatjavaltiot1, postinumerot, haunParametrit, hakijat, hakemukset, hakukohteet, osoitteet, vakiosisalto) -> HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                        maatjavaltiot1,
                        postinumerot,
                        osoitteet,
                        hakukohteet,
                        hakijat,
                        hakemukset,
                        hyvaksymiskirjeDTO.getHakukohdeOid(),
                        hyvaksymiskirjeDTO.getHakuOid(),
                        Optional.ofNullable(asiointikieli),
                        vakiosisalto,
                        hyvaksymiskirjeDTO.getTag(),
                        hyvaksymiskirjeDTO.getTemplateName(),
                        parsePalautusPvm(hyvaksymiskirjeDTO.getPalautusPvm(), haunParametrit),
                        parsePalautusAika(hyvaksymiskirjeDTO.getPalautusAika(), haunParametrit),
                        sahkoinenKorkeakoulunMassaposti))
                .flatMap(letterBatch -> letterBatchToViestintapalvelu(prosessi, letterBatch))
                .subscribe(
                        batchId -> {
                            LOG.info(successMessage);
                            prosessi.inkrementoiTehtyjaToita();
                            prosessi.setDokumenttiId(batchId);
                        },
                        e -> {
                            LOG.error(errorMessage, e);
                            prosessi.inkrementoiOhitettujaToita();
                            prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                        }
                );

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

    private static boolean haussaHylatty(HakijaDTO hakija) {
        return Optional.ofNullable(hakija.getHakutoiveet())
                .map(hakutoiveet -> hakutoiveet.stream()
                        .flatMap(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream())
                        .map(HakutoiveenValintatapajonoDTO::getTila)
                        .noneMatch(HakemuksenTila::isHyvaksytty))
                .orElse(true);
    }

    private Observable<Map<String, MetaHakukohde>> kiinnostavatHakukohteet(List<HakijaDTO> hakijat) {
        List<CompletableFuture<Pair<String, MetaHakukohde>>> hakukohdeFs = hakijat.stream()
                .flatMap(hakija -> hakija.getHakutoiveet().stream())
                .map(HakutoiveDTO::getHakukohdeOid)
                .distinct()
                .map(hakukohdeOid -> kirjeetHakukohdeCache.haeHakukohdeAsync(hakukohdeOid).thenApply(hakukohde -> Pair.of(hakukohdeOid, hakukohde)))
                .collect(Collectors.toList());
        return Observable.fromFuture(
                CompletableFuture.allOf(hakukohdeFs.toArray(new CompletableFuture[] {}))
                        .thenApply(v -> hakukohdeFs.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)))
        );
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

    private Observable<Map<String, Optional<Osoite>>> hakukohteidenHakutoimistojenOsoitteet(DokumenttiProsessi prosessi, Map<String, MetaHakukohde> hakukohteet, String asiointikieli) {
        if (prosessi.isKeskeytetty()) {
            return Observable.error(new RuntimeException("Kirjeiden muodostus keskeytetty"));
        }
        List<CompletableFuture<Pair<String, Optional<HakutoimistoDTO>>>> hakutoimistoFs = hakukohteet.values().stream().map(MetaHakukohde::getTarjoajaOid).distinct()
                .map(tarjoajaOid -> organisaatioAsyncResource.haeHakutoimisto(tarjoajaOid).thenApply(t -> Pair.of(tarjoajaOid, t)))
                .collect(Collectors.toList());
        return Observable.fromFuture(
                CompletableFuture.allOf(hakutoimistoFs.toArray(new CompletableFuture[]{}))
                        .thenApply(v -> {
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
                        })
        );
    }

    private Observable<String> letterBatchToViestintapalvelu(DokumenttiProsessi prosessi, LetterBatch letterBatch) {
        return vieLetterBatch(prosessi, letterBatch)
                .flatMap(batchId -> Observable.interval(pollingIntervalMillis, MILLISECONDS)
                        .flatMap(i -> letterBatchProcessingStatus(prosessi, batchId))
                        .take(1)
                        .timeout(VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA.toMillis(),
                                MILLISECONDS,
                                Observable.error(new RuntimeException("Kirjeiden vienti viestintäpalveluun aikakatkaistu"))));
    }

    private Observable<String> vieLetterBatch(DokumenttiProsessi prosessi, LetterBatch letterBatch) {
        if (prosessi.isKeskeytetty()) {
            return Observable.error(new RuntimeException("Kirjeiden muodostus keskeytetty"));
        }
        return Observable.fromFuture(viestintapalveluAsyncResource.vieLetterBatch(letterBatch))
                .flatMap(response -> {
                    if (response.getStatus().equals(LetterResponse.STATUS_SUCCESS)) {
                        return Observable.just(response.getBatchId());
                    }
                    return Observable.error(
                            new RuntimeException(
                                    response.getErrors().entrySet().stream()
                                            .map(e -> e.getKey() + ": " + e.getValue())
                                            .collect(Collectors.joining("\n"))
                            )
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
                        .thenApply(response -> batchId)
        );
    }

    public String parsePalautusPvm(String specifiedPvm, ParametritParser haunParametrit) {
        if (StringUtils.trimToNull(specifiedPvm) == null && haunParametrit.opiskelijanPaikanVastaanottoPaattyy() != null) {
            return pvmMuoto.format(haunParametrit.opiskelijanPaikanVastaanottoPaattyy());
        }
        return specifiedPvm;
    }

    public String parsePalautusAika(String specifiedAika, ParametritParser haunParametrit) {
        if (StringUtils.trimToNull(specifiedAika) == null && haunParametrit.opiskelijanPaikanVastaanottoPaattyy() != null) {
            return kelloMuoto.format(haunParametrit.opiskelijanPaikanVastaanottoPaattyy());
        }
        return specifiedAika;
    }
}
