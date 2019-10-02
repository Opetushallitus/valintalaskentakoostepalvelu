package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
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
import java.util.Collection;
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
    private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
    private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final AtaruAsyncResource ataruAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private final OrganisaatioAsyncResource organisaatioAsyncResource;
    private final HakuParametritService hakuParametritService;
    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
    private final DokumenttiAsyncResource dokumenttiAsyncResource;
    private final DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;
    private final int pollingIntervalMillis;

    private SimpleDateFormat pvmMuoto = new SimpleDateFormat("dd.MM.yyyy");
    private SimpleDateFormat kelloMuoto = new SimpleDateFormat("HH.mm");

    @Autowired
    public HyvaksymiskirjeetServiceImpl(
            ViestintapalveluAsyncResource viestintapalveluAsyncResource,
            HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
            ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            AtaruAsyncResource ataruAsyncResource,
            TarjontaAsyncResource tarjontaAsyncResource,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            HakuParametritService hakuParametritService,
            KoodistoCachedAsyncResource koodistoCachedAsyncResource,
            DokumenttiAsyncResource dokumenttiAsyncResource,
            DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti,
            @Value("${valintalaskentakoostepalvelu.hyvaksymiskirjeet.polling.interval.millis:10000}") int pollingIntervalMillis) {
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
        this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource = ataruAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.hakuParametritService = hakuParametritService;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
        this.dokumenttiProsessiKomponentti = dokumenttiProsessiKomponentti;
        this.pollingIntervalMillis = pollingIntervalMillis;
    }

    @Override
    public ProsessiId hyvaksymiskirjeetHakemuksille(HyvaksymiskirjeDTO hyvaksymiskirjeDTO, List<String> hakemusOids) {
        String hakuOid = hyvaksymiskirjeDTO.getHakuOid();
        String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();
        LOG.info(String.format("Aloitetaan hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle", hakukohdeOid, hakemusOids.size()));
        DokumenttiProsessi prosessi = new DokumenttiProsessi("", "", "", Collections.emptyList());
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
        prosessi.setKokonaistyo(1);

        Observable.zip(
                haunParametrit(hakuOid),
                hakijatByHakemusOids(hakuOid, hakemusOids),
                hakemuksetByOids(hakuOid, hakemusOids),
                (haunParametrit, hakijat, hakemukset) -> muodostaHyvaksymiskirjeet(
                        prosessi,
                        hakijat,
                        hakemukset,
                        hakuOid,
                        hakukohdeOid,
                        null,
                        false,
                        hyvaksymiskirjeDTO.getSisalto(),
                        hyvaksymiskirjeDTO.getTag(),
                        hyvaksymiskirjeDTO.getTemplateName(),
                        parsePalautusPvm(hyvaksymiskirjeDTO.getPalautusPvm(), haunParametrit),
                        parsePalautusAika(hyvaksymiskirjeDTO.getPalautusAika(), haunParametrit),
                        false
                )
        ).flatMap(x -> x).subscribe(
                batchId -> {
                    LOG.info(String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle valmistui", hakukohdeOid, hakemusOids.size()));
                    prosessi.inkrementoiTehtyjaToita();
                    prosessi.setDokumenttiId(batchId);
                },
                e -> {
                    LOG.error(String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle epäonnistui", hakukohdeOid, hakemusOids.size()), e);
                    prosessi.inkrementoiOhitettujaToita();
                    prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                }
        );
        return prosessi.toProsessiId();
    }

    @Override
    public ProsessiId jalkiohjauskirjeHakukohteelle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
        LOG.info(String.format("Aloitetaan hakukohteen %s jälkiohjauskirjeiden muodostaminen", hyvaksymiskirjeDTO.getHakukohdeOid()));
        DokumenttiProsessi prosessi = new DokumenttiProsessi("", "", "", Collections.emptyList());
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
        prosessi.setKokonaistyo(1);

        Observable.zip(
                hakemuksetByHakukohde(hyvaksymiskirjeDTO.getHakuOid(), hyvaksymiskirjeDTO.getHakukohdeOid()),
                hakijatByHakukohde(hyvaksymiskirjeDTO.getHakuOid(), hyvaksymiskirjeDTO.getHakukohdeOid()),
                (hakemukset, hakijat) -> {
                    LOG.error("Tehdaan hakukohteeseen valitsemattomille filtterointi. Saatiin hakijoita {}", hakijat.size());
                    Collection<HakijaDTO> hylatyt = hakijat.stream()
                            .filter(HyvaksymiskirjeetServiceImpl::haussaHylatty)
                            .collect(Collectors.toList());

                    if (hylatyt.isEmpty()) {
                        LOG.error("Hakukohteessa {} ei ole jälkiohjattavia hakijoita!", hyvaksymiskirjeDTO.getHakukohdeOid());
                        throw new RuntimeException("Hakukohteessa " + hyvaksymiskirjeDTO.getHakukohdeOid() + " ei ole jälkiohjattavia hakijoita!");
                    }

                    Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(hylatyt);

                    return hakukohteidenHakutoimistojenOsoitteet(prosessi, hyvaksymiskirjeessaKaytetytHakukohteet, null)
                            .map(hakutoimistojenOsoitteet -> HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                                    koodistoCachedAsyncResource::haeKoodisto,
                                    hakutoimistojenOsoitteet,
                                    hyvaksymiskirjeessaKaytetytHakukohteet,
                                    hylatyt,
                                    hakemukset,
                                    hyvaksymiskirjeDTO.getHakukohdeOid(),
                                    hyvaksymiskirjeDTO.getHakuOid(),
                                    Optional.empty(),
                                    hyvaksymiskirjeDTO.getSisalto(),
                                    hyvaksymiskirjeDTO.getTag(),
                                    hyvaksymiskirjeDTO.getTemplateName(),
                                    hyvaksymiskirjeDTO.getPalautusPvm(),
                                    hyvaksymiskirjeDTO.getPalautusAika(),
                                    false
                            ))
                            .flatMap(letterBatch -> letterBatchToViestintapalvelu(prosessi, letterBatch));
                }
        ).flatMap(x -> x).subscribe(
                batchId -> {
                    LOG.info(String.format("Hakukohteen %s jälkiohjauskirjeiden muodostaminen valmistui", hyvaksymiskirjeDTO.getHakukohdeOid()));
                    prosessi.inkrementoiTehtyjaToita();
                    prosessi.setDokumenttiId(batchId);
                },
                e -> {
                    LOG.error(String.format("Hakukohteen %s jälkiohjauskirjeiden muodostaminen epäonnistui", hyvaksymiskirjeDTO.getHakukohdeOid()), e);
                    prosessi.inkrementoiOhitettujaToita();
                    prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                }
        );
        return prosessi.toProsessiId();
    }

    private static boolean haussaHylatty(HakijaDTO hakija) {
        return Optional.ofNullable(hakija.getHakutoiveet())
                .map(hakutoiveet -> hakutoiveet.stream()
                        .flatMap(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream())
                        .map(HakutoiveenValintatapajonoDTO::getTila)
                        .noneMatch(HakemuksenTila::isHyvaksytty))
                .orElse(true);
    }

    @Override
    public ProsessiId hyvaksymiskirjeetHakukohteelle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
        String hakuOid = hyvaksymiskirjeDTO.getHakuOid();
        String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();
        LOG.info(String.format("Aloitetaan hakukohteen %s hyväksymiskirjeiden muodostaminen", hakukohdeOid));
        DokumenttiProsessi prosessi = new DokumenttiProsessi("", "", "", Collections.emptyList());
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
        prosessi.setKokonaistyo(1);

        Observable.zip(
                haunParametrit(hakuOid),
                hyvaksytytByHakukohde(hakuOid, hakukohdeOid),
                hakemuksetByHakukohde(hakuOid, hakukohdeOid),
                (haunParametrit, hakijat, hakemukset) -> muodostaHyvaksymiskirjeet(
                        prosessi,
                        hakijat,
                        hakemukset,
                        hakuOid,
                        hakukohdeOid,
                        null,
                        hyvaksymiskirjeDTO.getVainTulosEmailinKieltaneet(),
                        hyvaksymiskirjeDTO.getSisalto(),
                        hyvaksymiskirjeDTO.getTag(),
                        hyvaksymiskirjeDTO.getTemplateName(),
                        parsePalautusPvm(hyvaksymiskirjeDTO.getPalautusPvm(), haunParametrit),
                        parsePalautusAika(hyvaksymiskirjeDTO.getPalautusAika(), haunParametrit),
                        false
                )
        ).flatMap(x -> x).subscribe(
                batchId -> {
                    LOG.info(String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen valmistui", hakukohdeOid));
                    prosessi.inkrementoiTehtyjaToita();
                    prosessi.setDokumenttiId(batchId);
                },
                e -> {
                    LOG.error(String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen epäonnistui", hakukohdeOid), e);
                    prosessi.inkrementoiOhitettujaToita();
                    prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                }
        );
        return prosessi.toProsessiId();
    }

    @Override
    public ProsessiId hyvaksymiskirjeetHaulle(String hakuOid, String asiointikieli, String defaultValue) {
        LOG.info(String.format("Aloitetaan haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s", hakuOid, asiointikieli));
        DokumenttiProsessi prosessi = new DokumenttiProsessi("hyvaksymiskirjeet", "Luo hyvaksymiskirjeet haulle", hakuOid, Arrays.asList("hyvaksymiskirjeet", "haulle"));
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);

        prosessi.setKokonaistyo(1);
        Observable<List<HakijaDTO>> hakijatF = hyvaksytytByHaku(hakuOid);
        Observable.zip(
                haunParametrit(hakuOid),
                hakijatF,
                hakijatF.flatMap(hakijat -> hakemuksetByOids(hakuOid, hakijat.stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList()))),
                (haunParametrit, hakijat, hakemukset) -> muodostaHyvaksymiskirjeet(
                        prosessi,
                        hakijat,
                        hakemukset,
                        hakuOid,
                        null,
                        asiointikieli,
                        false,
                        defaultValue,
                        hakuOid,
                        "hyvaksymiskirje",
                        parsePalautusPvm(null, haunParametrit),
                        parsePalautusAika(null, haunParametrit),
                        true
                )
        ).flatMap(x -> x).subscribe(
                batchId -> {
                    LOG.info(String.format("Haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s valmistui", hakuOid, asiointikieli));
                    prosessi.inkrementoiTehtyjaToita();
                    prosessi.setDokumenttiId(batchId);
                },
                e -> {
                    LOG.error(String.format("Haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s epäonnistui", hakuOid, asiointikieli), e);
                    prosessi.inkrementoiOhitettujaToita();
                    prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                }
        );
        return prosessi.toProsessiId();
    }

    public ProsessiId hyvaksymiskirjeetHaulleHakukohteittain(String hakuOid, Optional<String> defaultValue) {
        LOG.info(String.format("Aloitetaan haun %s hyväksymiskirjeiden muodostaminen hakukohteittain", hakuOid));
        DokumenttiProsessi prosessi = new DokumenttiProsessi("hyvaksymiskirjeet", "Luo hyvaksymiskirjeet haulle", hakuOid, Arrays.asList("hyvaksymiskirjeet", "haulle"));
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);

        Observable<List<HakijaDTO>> hakijatF = hyvaksytytByHaku(hakuOid);
        Observable.zip(
                haunParametrit(hakuOid),
                hakijatF,
                hakijatF.flatMap(hakijat -> hakemuksetByOids(hakuOid, hakijat.stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList()))),
                (haunParametrit, hakijat, hakemukset) -> {
                    List<String> hakukohteetJoissaHyvaksyttyja = hakijat.stream()
                            .flatMap(hakija -> hakija.getHakutoiveet().stream())
                            .filter(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream().anyMatch(valintatapajono -> valintatapajono.getTila().isHyvaksytty()))
                            .map(HakutoiveDTO::getHakukohdeOid)
                            .distinct()
                            .collect(Collectors.toList());
                    prosessi.setKokonaistyo(hakukohteetJoissaHyvaksyttyja.size());
                    return Observable.fromIterable(hakukohteetJoissaHyvaksyttyja)
                            .flatMap(hakukohdeOid -> muodostaHyvaksymiskirjeet(
                                    prosessi,
                                    hakijat,
                                    hakemukset,
                                    hakuOid,
                                    hakukohdeOid,
                                    null,
                                    false,
                                    defaultValue.orElse(null),
                                    hakuOid,
                                    "hyvaksymiskirje",
                                    parsePalautusPvm(null, haunParametrit),
                                    parsePalautusAika(null, haunParametrit),
                                    false)
                                            .flatMap(batchId -> renameHakukohteenHyvaksymiskirjeet(prosessi, hakukohdeOid, batchId))
                                            .map(r -> Pair.of(hakukohdeOid, Optional.<Throwable>empty()))
                                            .onErrorReturn(e -> Pair.of(hakukohdeOid, Optional.of(e))),
                                    6);
                }
        ).flatMap(x -> x).toList().subscribe(
                result -> result.forEach(r -> {
                    String hakukohdeOid = r.getLeft();
                    r.getRight().ifPresentOrElse(
                            e -> {
                                LOG.error(String.format("Haun %s hakukohteen %s hyväksymiskirjeiden muodostaminen epäonnistui", hakuOid, hakukohdeOid), e);
                                prosessi.inkrementoiOhitettujaToita();
                                prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(
                                        e.getMessage(),
                                        Collections.singletonList(new Tunniste(hakukohdeOid, Poikkeus.HAKUKOHDEOID))
                                ));
                            },
                            () -> {
                                LOG.info(String.format("Haun %s hakukohteen %s hyväksymiskirjeiden muodostaminen valmistui", hakuOid, hakukohdeOid));
                                prosessi.inkrementoiTehtyjaToita();
                            }
                    );
                    LOG.info(String.format("Haun %s hyväksymiskirjeiden muodostaminen hakukohteittain valmistui", hakuOid));
                    if (prosessi.getPoikkeukset().isEmpty()) {
                        prosessi.setDokumenttiId("valmistumisen-ilmaiseva-tunniste");
                    }
                }),
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

    private Observable<String> muodostaHyvaksymiskirjeet(DokumenttiProsessi prosessi,
                                                         List<HakijaDTO> hakijat,
                                                         Map<String, HakemusWrapper> hakemukset,
                                                         String hakuOid,
                                                         String hakukohdeJossaHyvaksytty,
                                                         String asiointikieli,
                                                         boolean vainTulosEmailinKieltaneet,
                                                         String vakiosisalto,
                                                         String tag,
                                                         String templateName,
                                                         String palautusPvm,
                                                         String palautusAika,
                                                         boolean sahkoinenKorkeakoulunMassaposti) {
        List<HakijaDTO> kasiteltavatHakijat = hakijat.stream()
                .filter(hakija -> !vainTulosEmailinKieltaneet || !hakemukset.get(hakija.getHakemusOid()).getLupaTulosEmail())
                .filter(hakija -> asiointikieli == null || asiointikieli.equalsIgnoreCase(hakemukset.get(hakija.getHakemusOid()).getAsiointikieli()))
                .filter(hakija -> hakija.getHakutoiveet().stream()
                        .filter(hakutoive -> hakukohdeJossaHyvaksytty == null || hakukohdeJossaHyvaksytty.equals(hakutoive.getHakukohdeOid()))
                        .flatMap(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream())
                        .anyMatch(valintatapajono -> valintatapajono.getTila().isHyvaksytty()))
                .collect(Collectors.toList());
        Map<String, MetaHakukohde> hakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(kasiteltavatHakijat);
        return Observable.zip(
                vakiosisalto == null ? Observable.fromFuture(tarjontaAsyncResource.haeHakukohde(hakukohdeJossaHyvaksytty)).flatMap(this::haeHakukohteenVakiosisalto) : Observable.just(vakiosisalto),
                hakukohteidenHakutoimistojenOsoitteet(prosessi, hakukohteet, asiointikieli),
                (sisalto, osoitteet) -> HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                        koodistoCachedAsyncResource::haeKoodisto,
                        osoitteet,
                        hakukohteet,
                        kasiteltavatHakijat,
                        hakemukset,
                        null,
                        hakuOid,
                        Optional.ofNullable(asiointikieli),
                        sisalto,
                        tag,
                        templateName,
                        palautusPvm,
                        palautusAika,
                        sahkoinenKorkeakoulunMassaposti)
        ).flatMap(letterBatch -> letterBatchToViestintapalvelu(prosessi, letterBatch));
    }

    private Observable<String> haeHakukohteenVakiosisalto(HakukohdeV1RDTO hakukohde) {
        return Observable.fromFuture(
                viestintapalveluAsyncResource.haeKirjepohja(
                        hakukohde.getHakuOid(),
                        hakukohde.getTarjoajaOids().iterator().next(),
                        "hyvaksymiskirje",
                        KirjeetHakukohdeCache.getOpetuskieli(hakukohde.getOpetusKielet()),
                        hakukohde.getOid()
                ).thenCompose(kirjepohjat -> kirjepohjat.stream()
                        .filter(kirjepohja -> VAKIOTEMPLATE.equals(kirjepohja.getName()))
                        .flatMap(kirjepohja -> kirjepohja.getTemplateReplacements().stream().filter(tdd -> VAKIODETAIL.equals(tdd.getName())))
                        .map(TemplateDetail::getDefaultValue)
                        .map(CompletableFuture::completedFuture)
                        .findAny()
                        .orElse(CompletableFuture.failedFuture(new RuntimeException(String.format("Ei %s tai %s templateDetailia hakukohteelle %s", VAKIOTEMPLATE, VAKIODETAIL, hakukohde.getOid())))))
        );
    }

    private Observable<Map<String, Optional<Osoite>>> hakukohteidenHakutoimistojenOsoitteet(DokumenttiProsessi prosessi, Map<String, MetaHakukohde> hakukohteet, String asiointikieli) {
        if (prosessi.isKeskeytetty()) {
            return Observable.error(new RuntimeException("Kirjeiden muodostus keskeytetty"));
        }
        List<CompletableFuture<Pair<String, Optional<HakutoimistoDTO>>>> hakutoimistoFs = hakukohteet.values().stream().map(MetaHakukohde::getTarjoajaOid).distinct()
                .map(tarjoajaOid -> organisaatioAsyncResource.haeHakutoimisto(tarjoajaOid).thenApply(t -> Pair.of(tarjoajaOid, t)))
                .collect(Collectors.toList());
        return Observable.fromFuture(
                CompletableFuture.allOf(hakutoimistoFs.toArray(new CompletableFuture[] {}))
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
        if(StringUtils.trimToNull(specifiedPvm) == null && haunParametrit.opiskelijanPaikanVastaanottoPaattyy() != null) {
            return pvmMuoto.format(haunParametrit.opiskelijanPaikanVastaanottoPaattyy());
        }
        return specifiedPvm;
    }

    public String parsePalautusAika(String specifiedAika, ParametritParser haunParametrit) {
        if(StringUtils.trimToNull(specifiedAika) == null && haunParametrit.opiskelijanPaikanVastaanottoPaattyy() != null) {
            return kelloMuoto.format(haunParametrit.opiskelijanPaikanVastaanottoPaattyy());
        }
        return specifiedAika;
    }
}
