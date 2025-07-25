package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource.VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA;
import static fi.vm.sade.valinta.kooste.util.ExecutorUtil.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tunniste;
import fi.vm.sade.valinta.kooste.viestintapalvelu.Hakijapalvelu;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.JalkiohjauskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateDetail;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.JalkiohjauskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.model.types.ContentStructureType;
import fi.vm.sade.valinta.kooste.viestintapalvelu.model.types.KirjeenVastaanottaja;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeetService;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.SyotettyArvoDTO;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HyvaksymiskirjeetServiceImpl implements HyvaksymiskirjeetService {
  private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetServiceImpl.class);
  private static final String VAKIOTEMPLATE = "default";
  private static final String VAKIODETAIL = "sisalto";

  private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
  private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
  private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
  private final ApplicationAsyncResource applicationAsyncResource;
  private final AtaruAsyncResource ataruAsyncResource;
  private final TarjontaAsyncResource tarjontaAsyncResource;
  private final OrganisaatioAsyncResource organisaatioAsyncResource;
  private final HakuParametritService hakuParametritService;
  private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
  private final DokumenttiAsyncResource dokumenttiAsyncResource;
  private final DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;
  private final KirjeetHakukohdeCache kirjeetHakukohdeCache;
  private final ExecutorService smallBatchExecutor;
  private final ExecutorService bigBatchExecutor;
  private final int pollingIntervalMillis;
  private final int syotettavatArvotThreadpoolMaxSize;

  @Autowired
  public HyvaksymiskirjeetServiceImpl(
      ViestintapalveluAsyncResource viestintapalveluAsyncResource,
      ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
      ValintalaskentaAsyncResource valintalaskentaAsyncResource,
      ApplicationAsyncResource applicationAsyncResource,
      AtaruAsyncResource ataruAsyncResource,
      TarjontaAsyncResource tarjontaAsyncResource,
      OrganisaatioAsyncResource organisaatioAsyncResource,
      HakuParametritService hakuParametritService,
      KoodistoCachedAsyncResource koodistoCachedAsyncResource,
      DokumenttiAsyncResource dokumenttiAsyncResource,
      DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti,
      KirjeetHakukohdeCache kirjeetHakukohdeCache,
      @Value("${valintalaskentakoostepalvelu.kirjeet.smallBatchMaxConcurrency:6}")
          int smallBatchMaxConcurrency,
      @Value("${valintalaskentakoostepalvelu.kirjeet.bigBatchMaxConcurrency:1}")
          int bigBatchMaxConcurrency,
      @Value("${valintalaskentakoostepalvelu.kirjeet.polling.interval.millis:10000}")
          int pollingIntervalMillis,
      @Value("${valintalaskentakoostepalvelu.kirjeet.syotettavatArvotThreadpoolMaxSize:2}")
          int syotettavatArvotThreadpoolMaxSize) {
    this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
    this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
    this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
    this.applicationAsyncResource = applicationAsyncResource;
    this.ataruAsyncResource = ataruAsyncResource;
    this.tarjontaAsyncResource = tarjontaAsyncResource;
    this.organisaatioAsyncResource = organisaatioAsyncResource;
    this.hakuParametritService = hakuParametritService;
    this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    this.dokumenttiAsyncResource = dokumenttiAsyncResource;
    this.dokumenttiProsessiKomponentti = dokumenttiProsessiKomponentti;
    this.kirjeetHakukohdeCache = kirjeetHakukohdeCache;
    this.smallBatchExecutor =
        createExecutorService(smallBatchMaxConcurrency, "kirjeet-small-batch");
    this.bigBatchExecutor = createExecutorService(bigBatchMaxConcurrency, "kirjeet-big-batch");
    this.pollingIntervalMillis = pollingIntervalMillis;
    this.syotettavatArvotThreadpoolMaxSize = syotettavatArvotThreadpoolMaxSize;
  }

  @Override
  public ProsessiId hyvaksymiskirjeetHakemuksille(
      HyvaksymiskirjeDTO hyvaksymiskirjeDTO, List<String> hakemusOids) {
    String hakuOid = hyvaksymiskirjeDTO.getHakuOid();
    String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();
    return this.yhdenKirjeeranProsessi(
        this.smallBatchExecutor,
        prosessi -> {
          CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF =
              tarjontaAsyncResource
                  .haeHaku(hakuOid)
                  .thenComposeAsync(haku -> hakemuksetByOids(haku, hakemusOids));
          CompletableFuture<List<HakijaDTO>> hakijatF =
              hakijatByHakemusOids(hakuOid, hakemusOids)
                  .thenComposeAsync(
                      hakijat ->
                          hakemuksetF.thenApplyAsync(
                              hakemukset ->
                                  hyvaksytytHakijat(
                                      hakijat,
                                      hakemukset,
                                      hakukohdeOid,
                                      null,
                                      hyvaksymiskirjeDTO.getVainTulosEmailinKieltaneet())));
          return muodostaHyvaksymiskirjeet(
              prosessi,
              hakuParametritService.getParametritForHakuAsync(hakuOid),
              hakijatF,
              hakemuksetF,
              hyvaksymiskirjeDTO,
              null,
              false,
              Collections.singletonList(ContentStructureType.letter),
              KirjeenVastaanottaja.HAKIJA);
        },
        String.format(
            "Aloitetaan hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle",
            hakukohdeOid, hakemusOids.size()),
        String.format(
            "Hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle valmistui",
            hakukohdeOid, hakemusOids.size()),
        String.format(
            "Hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle epäonnistui",
            hakukohdeOid, hakemusOids.size()));
  }

  @Override
  public ProsessiId jalkiohjauskirjeetHakemuksille(
      JalkiohjauskirjeDTO jalkiohjauskirjeDTO, List<String> hakemusOids) {
    String hakuOid = jalkiohjauskirjeDTO.getHakuOid();
    return this.yhdenKirjeeranProsessi(
        this.smallBatchExecutor,
        prosessi ->
            tarjontaAsyncResource
                .haeHaku(hakuOid)
                .thenComposeAsync(
                    haku -> {
                      CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF =
                          hakemuksetByOids(haku, hakemusOids);
                      CompletableFuture<List<HakijaDTO>> hakijatF =
                          hakijatByHakemusOids(hakuOid, hakemusOids)
                              .thenComposeAsync(
                                  hakijat ->
                                      hakemuksetF.thenApplyAsync(
                                          hakemukset ->
                                              hylatytHakijat(
                                                  hakijat,
                                                  hakemukset,
                                                  jalkiohjauskirjeDTO.getKielikoodi())));
                      return muodostaJalkiohjauskirjeet(
                          prosessi,
                          hakijatF,
                          hakemuksetF,
                          jalkiohjauskirjeDTO,
                          false,
                          haku.isKorkeakouluhaku(),
                          Collections.singletonList(ContentStructureType.letter),
                          KirjeenVastaanottaja.HAKIJA);
                    }),
        String.format(
            "Aloitetaan jälkiohjauskirjeiden muodostaminen %d hakemukselle", hakemusOids.size()),
        String.format(
            "Kälkiohjauskirjeiden muodostaminen %d hakemukselle valmistui", hakemusOids.size()),
        String.format(
            "Jälkiohjauskirjeiden muodostaminen %d hakemukselle epäonnistui", hakemusOids.size()));
  }

  @Override
  public ProsessiId jalkiohjauskirjeHakukohteelle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
    String hakuOid = hyvaksymiskirjeDTO.getHakuOid();
    String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();
    return this.yhdenKirjeeranProsessi(
        this.smallBatchExecutor,
        prosessi -> {
          CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF =
              hakemuksetByHakukohde(hakuOid, hakukohdeOid);
          CompletableFuture<List<HakijaDTO>> hakijatF =
              valintaTulosServiceAsyncResource
                  .getKaikkiHakijat(hakuOid, hakukohdeOid)
                  .thenComposeAsync(
                      hakijat ->
                          hakemuksetF.thenApplyAsync(
                              hakemukset -> hylatytHakijat(hakijat, hakemukset, null)));
          CompletableFuture<ParametritParser> haunParametritF =
              CompletableFuture.completedFuture(new ParametritParser(new ParametritDTO(), ""));
          return muodostaHyvaksymiskirjeet(
              prosessi,
              haunParametritF,
              hakijatF,
              hakemuksetF,
              hyvaksymiskirjeDTO,
              null,
              false,
              Collections.singletonList(ContentStructureType.letter),
              KirjeenVastaanottaja.HAKIJA);
        },
        String.format("Aloitetaan hakukohteen %s jälkiohjauskirjeiden muodostaminen", hakukohdeOid),
        String.format("Hakukohteen %s jälkiohjauskirjeiden muodostaminen valmistui", hakukohdeOid),
        String.format(
            "Hakukohteen %s jälkiohjauskirjeiden muodostaminen epäonnistui", hakukohdeOid));
  }

  @Override
  public ProsessiId hyvaksymiskirjeetHakukohteelle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
    String hakuOid = hyvaksymiskirjeDTO.getHakuOid();
    String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();
    return this.yhdenKirjeeranProsessi(
        this.smallBatchExecutor,
        prosessi -> {
          CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF =
              hakemuksetByHakukohde(hakuOid, hakukohdeOid);
          CompletableFuture<List<HakijaDTO>> hakijatF =
              valintaTulosServiceAsyncResource
                  .getKoulutuspaikalliset(hakuOid, hakukohdeOid)
                  .thenComposeAsync(
                      hakijat ->
                          hakemuksetF.thenApplyAsync(
                              hakemukset ->
                                  hyvaksytytHakijat(
                                      hakijat,
                                      hakemukset,
                                      hakukohdeOid,
                                      null,
                                      hyvaksymiskirjeDTO.getVainTulosEmailinKieltaneet())));
          return muodostaHyvaksymiskirjeet(
                  prosessi,
                  hakuParametritService.getParametritForHakuAsync(hakuOid),
                  hakijatF,
                  hakemuksetF,
                  hyvaksymiskirjeDTO,
                  null,
                  false,
                  Collections.singletonList(ContentStructureType.letter),
                  KirjeenVastaanottaja.HAKIJA)
              .thenComposeAsync(
                  batchId ->
                      dokumenttiAsyncResource
                          .uudelleenNimea(batchId, "hyvaksymiskirje_" + hakukohdeOid + ".pdf")
                          .thenApplyAsync(v -> batchId));
        },
        String.format("Aloitetaan hakukohteen %s hyväksymiskirjeiden muodostaminen", hakukohdeOid),
        String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen valmistui", hakukohdeOid),
        String.format(
            "Hakukohteen %s hyväksymiskirjeiden muodostaminen epäonnistui", hakukohdeOid));
  }

  @Override
  public ProsessiId hyvaksymiskirjeetHaulle(
      HyvaksymiskirjeDTO hyvaksymiskirjeDTO,
      String asiointikieli,
      KirjeenVastaanottaja kirjeenVastaanottaja) {
    String hakuOid = hyvaksymiskirjeDTO.getHakuOid();
    return this.yhdenKirjeeranProsessi(
        this.bigBatchExecutor,
        prosessi -> {
          CompletableFuture<List<HakijaDTO>> kaikkiHakijatF =
              valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid);
          CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF =
              kaikkiHakijatF.thenComposeAsync(
                  hakijat ->
                      tarjontaAsyncResource
                          .haeHaku(hakuOid)
                          .thenComposeAsync(
                              haku ->
                                  hakemuksetByOids(
                                      haku,
                                      hakijat.stream()
                                          .map(HakijaDTO::getHakemusOid)
                                          .collect(Collectors.toList()))));
          CompletableFuture<List<HakijaDTO>> hakijatF =
              kaikkiHakijatF.thenComposeAsync(
                  hakijat ->
                      hakemuksetF.thenApplyAsync(
                          hakemukset ->
                              hyvaksytytHakijat(hakijat, hakemukset, null, asiointikieli, false)));
          return muodostaHyvaksymiskirjeet(
              prosessi,
              hakuParametritService.getParametritForHakuAsync(hakuOid),
              hakijatF,
              hakemuksetF,
              hyvaksymiskirjeDTO,
              asiointikieli,
              true,
              Arrays.asList(ContentStructureType.accessibleHtml),
              kirjeenVastaanottaja);
        },
        String.format(
            "Aloitetaan haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s",
            hakuOid, asiointikieli),
        String.format(
            "Haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s valmistui",
            hakuOid, asiointikieli),
        String.format(
            "Haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s epäonnistui",
            hakuOid, asiointikieli),
        3,
        TimeUnit.HOURS);
  }

  @Override
  public ProsessiId jalkiohjauskirjeetHaulle(
      JalkiohjauskirjeDTO jalkiohjauskirjeDTO, KirjeenVastaanottaja kirjeenVastaanottaja) {
    String hakuOid = jalkiohjauskirjeDTO.getHakuOid();
    String asiointikieli = jalkiohjauskirjeDTO.getKielikoodi();
    return this.yhdenKirjeeranProsessi(
        this.bigBatchExecutor,
        prosessi ->
            tarjontaAsyncResource
                .haeHaku(hakuOid)
                .thenComposeAsync(
                    haku -> {
                      CompletableFuture<List<HakijaDTO>> kaikkiHakijatF =
                          valintaTulosServiceAsyncResource.getHakijatIlmanKoulutuspaikkaa(hakuOid);
                      CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF =
                          kaikkiHakijatF.thenComposeAsync(
                              hakijat ->
                                  hakemuksetByOids(
                                      haku,
                                      hakijat.stream()
                                          .map(HakijaDTO::getHakemusOid)
                                          .collect(Collectors.toList())));
                      CompletableFuture<List<HakijaDTO>> hakijatF =
                          kaikkiHakijatF.thenComposeAsync(
                              hakijat ->
                                  hakemuksetF.thenApplyAsync(
                                      hakemukset ->
                                          hylatytHakijat(hakijat, hakemukset, asiointikieli)));
                      return muodostaJalkiohjauskirjeet(
                          prosessi,
                          hakijatF,
                          hakemuksetF,
                          jalkiohjauskirjeDTO,
                          true,
                          haku.isKorkeakouluhaku(),
                          getContentStructureTypesForHaunJalkiohjauskirjeet(
                              haku.isKorkeakouluhaku()),
                          kirjeenVastaanottaja);
                    }),
        String.format(
            "Aloitetaan haun %s jälkiohjauskirjeiden muodostaminen asiointikielelle %s",
            hakuOid, asiointikieli),
        String.format(
            "Haun %s jälkiohjauskirjeiden muodostaminen asiointikielelle %s valmistui",
            hakuOid, asiointikieli),
        String.format(
            "Haun %s jälkiohjauskirjeiden muodostaminen asiointikielelle %s epäonnistui",
            hakuOid, asiointikieli),
        3,
        TimeUnit.HOURS);
  }

  public ProsessiId hyvaksymiskirjeetHaulleHakukohteittain(HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
    String hakuOid = hyvaksymiskirjeDTO.getHakuOid();
    DokumenttiProsessi prosessi = new DokumenttiProsessi("", "", "", Collections.emptyList());
    dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);

    this.bigBatchExecutor.execute(
        () -> {
          try {
            LOG.info(
                String.format(
                    "Aloitetaan haun %s hyväksymiskirjeiden muodostaminen hakukohteittain",
                    hakuOid));

            CompletableFuture<Map<String, Koodi>> maatjavaltiot1F =
                koodistoCachedAsyncResource.haeKoodistoAsync(
                    KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
            CompletableFuture<Map<String, Koodi>> postinumerotF =
                koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.POSTI);
            CompletableFuture<ParametritParser> haunParametritF =
                hakuParametritService.getParametritForHakuAsync(hakuOid);
            CompletableFuture<List<HakijaDTO>> hakijatF =
                valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid);
            CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF =
                hakijatF.thenComposeAsync(
                    hakijat ->
                        tarjontaAsyncResource
                            .haeHaku(hakuOid)
                            .thenComposeAsync(
                                haku ->
                                    hakemuksetByOids(
                                        haku,
                                        hakijat.stream()
                                            .map(HakijaDTO::getHakemusOid)
                                            .collect(Collectors.toList()))));
            CompletableFuture<Map<String, Map<String, List<SyotettyArvoDTO>>>> syotetytArvotF =
                hakijatF.thenComposeAsync(this::hakijoidenSyotetytArvot);
            CompletableFuture<Map<String, MetaHakukohde>> hakukohteetF =
                hakijatF.thenComposeAsync(this::kiinnostavatHakukohteet);
            CompletableFuture<Map<String, Optional<Osoite>>> osoitteetF =
                CompletableFuture.completedFuture(Collections.emptyMap());

            CompletableFuture.allOf(
                    maatjavaltiot1F,
                    postinumerotF,
                    haunParametritF,
                    hakijatF,
                    hakemuksetF,
                    syotetytArvotF,
                    hakukohteetF,
                    osoitteetF)
                .get(1, TimeUnit.HOURS);
            List<Pair<String, Future<String>>> fs =
                hakijatF.join().stream()
                    .flatMap(hakija -> hakija.getHakutoiveet().stream())
                    .filter(
                        hakutoive ->
                            hakutoive.getHakutoiveenValintatapajonot().stream()
                                .anyMatch(
                                    valintatapajono -> valintatapajono.getTila().isHyvaksytty()))
                    .map(HakutoiveDTO::getHakukohdeOid)
                    .distinct()
                    .map(
                        hakukohdeOid ->
                            Pair.of(
                                hakukohdeOid,
                                this.smallBatchExecutor.submit(
                                    () -> {
                                      try {
                                        LOG.info(
                                            String.format(
                                                "Aloitetaan haun %s hakukohteen %s hyväksymiskirjeiden muodostaminen",
                                                hakuOid, hakukohdeOid));
                                        return haeHakukohteenVakiosisalto(
                                                hyvaksymiskirjeDTO.getSisalto(), hakukohdeOid)
                                            .thenApplyAsync(
                                                vakiosisalto ->
                                                    HyvaksymiskirjeetKomponentti
                                                        .teeHyvaksymiskirjeet(
                                                            maatjavaltiot1F.join(),
                                                            postinumerotF.join(),
                                                            osoitteetF.join(),
                                                            hakukohteetF.join(),
                                                            hyvaksytytHakijat(
                                                                hakijatF.join(),
                                                                hakemuksetF.join(),
                                                                hakukohdeOid,
                                                                null,
                                                                false),
                                                            hakemuksetF.join(),
                                                            syotetytArvotF.join(),
                                                            hakukohdeOid,
                                                            hakuOid,
                                                            Optional.empty(),
                                                            vakiosisalto,
                                                            hakuOid,
                                                            hyvaksymiskirjeDTO.getTemplateName(),
                                                            parsePalautusPvm(
                                                                null, haunParametritF.join()),
                                                            parsePalautusAika(
                                                                null, haunParametritF.join()),
                                                            false,
                                                            Collections.singletonList(
                                                                ContentStructureType.letter),
                                                            KirjeenVastaanottaja.HAKIJA))
                                            .thenComposeAsync(
                                                letterBatch ->
                                                    letterBatchToViestintapalvelu(
                                                        prosessi, letterBatch))
                                            .thenComposeAsync(
                                                batchId ->
                                                    dokumenttiAsyncResource
                                                        .uudelleenNimea(
                                                            batchId,
                                                            "hyvaksymiskirje_"
                                                                + hakukohdeOid
                                                                + ".pdf")
                                                        .thenApplyAsync(v -> batchId))
                                            .get(1, TimeUnit.HOURS);
                                      } catch (Exception e) {
                                        throw new RuntimeException(e);
                                      }
                                    })))
                    .collect(Collectors.toList());
            prosessi.setKokonaistyo(fs.size());
            AtomicReference<String> exampleBatchId = new AtomicReference<>(null);
            List<Poikkeus> poikkeukset =
                fs.stream()
                    .flatMap(
                        p -> {
                          String hakukohdeOid = p.getLeft();
                          try {
                            String batchId = p.getRight().get(1, TimeUnit.HOURS);
                            LOG.info(
                                String.format(
                                    "Haun %s hakukohteen %s hyväksymiskirjeiden muodostaminen valmistui",
                                    hakuOid, hakukohdeOid));
                            prosessi.inkrementoiTehtyjaToita();
                            exampleBatchId.compareAndSet(null, batchId);
                            return Stream.empty();
                          } catch (Exception e) {
                            String msg =
                                String.format(
                                    "Haun %s hakukohteen %s hyväksymiskirjeiden muodostaminen epäonnistui",
                                    hakuOid, hakukohdeOid);
                            LOG.error(msg, e);
                            prosessi.inkrementoiOhitettujaToita();
                            return Stream.of(
                                Poikkeus.koostepalvelupoikkeus(
                                    msg,
                                    Collections.singletonList(
                                        new Tunniste(hakukohdeOid, Poikkeus.HAKUKOHDEOID))));
                          }
                        })
                    .collect(Collectors.toList());
            if (poikkeukset.isEmpty()) {
              LOG.info(
                  String.format(
                      "Haun %s hyväksymiskirjeiden muodostaminen hakukohteittain valmistui",
                      hakuOid));
              prosessi.setDokumenttiId(exampleBatchId.get());
            } else {
              LOG.error(
                  String.format(
                      "Haun %s hyväksymiskirjeiden muodostaminen hakukohteittain epäonnistui",
                      hakuOid));
              prosessi.getPoikkeukset().addAll(poikkeukset);
            }
          } catch (Exception e) {
            String msg =
                String.format(
                    "Haun %s hyväksymiskirjeiden muodostaminen hakukohteittain epäonnistui",
                    hakuOid);
            LOG.error(msg, e);
            prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(msg));
          }
        });

    return prosessi.toProsessiId();
  }

  private CompletableFuture<List<HakijaDTO>> hakijatByHakemusOids(
      String hakuOid, List<String> hakemusOids) {
    return CompletableFutureUtil.sequence(
        hakemusOids.stream()
            .map(
                hakemusOid ->
                    valintaTulosServiceAsyncResource.getHakijaByHakemus(hakuOid, hakemusOid))
            .collect(Collectors.toList()));
  }

  private CompletableFuture<Map<String, HakemusWrapper>> hakemuksetByHakukohde(
      String hakuOid, String hakukohdeOid) {
    return tarjontaAsyncResource
        .haeHaku(hakuOid)
        .thenComposeAsync(
            haku ->
                haku.isHakemuspalvelu()
                    ? ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid)
                    : applicationAsyncResource.getApplicationsByOidsWithPOST(
                        hakuOid, Collections.singletonList(hakukohdeOid)))
        .thenApplyAsync(
            hakemukset ->
                hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h)));
  }

  private CompletableFuture<Map<String, HakemusWrapper>> hakemuksetByOids(
      Haku haku, List<String> hakemusOids) {
    return (haku.isHakemuspalvelu()
            ? ataruAsyncResource.getApplicationsByOids(hakemusOids)
            : applicationAsyncResource.getApplicationsByhakemusOidsInParts(
                haku.oid, hakemusOids, ApplicationAsyncResource.DEFAULT_KEYS))
        .thenApplyAsync(
            hakemukset ->
                hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h)));
  }

  private CompletableFuture<Map<String, List<SyotettyArvoDTO>>> syotetytArvotByHakukohde(
      String hakukohdeOid) {
    return this.valintalaskentaAsyncResource
        .laskennantulokset(hakukohdeOid)
        .thenApplyAsync(
            valinnanvaiheet ->
                valinnanvaiheet.stream()
                    .flatMap(valinnanvaihe -> valinnanvaihe.getValintatapajonot().stream())
                    .flatMap(valintatapajono -> valintatapajono.getJonosijat().stream())
                    .collect(
                        Collectors.toMap(
                            JonosijaDTO::getHakemusOid,
                            JonosijaDTO::getSyotetytArvot,
                            (l, ll) -> {
                              l.addAll(ll);
                              return l;
                            })));
  }

  private CompletableFuture<Map<String, Map<String, List<SyotettyArvoDTO>>>>
      hakijoidenSyotetytArvot(List<HakijaDTO> hakijat) {
    CompletableFuture<Map<String, Map<String, List<SyotettyArvoDTO>>>> resultFuture =
        new CompletableFuture<>();

    Executors.newSingleThreadExecutor()
        .submit(
            () -> {
              long start = System.currentTimeMillis();
              Map<String, Map<String, List<SyotettyArvoDTO>>> results = new ConcurrentHashMap<>();
              List<String> hakukohdeOids =
                  hakijat.stream()
                      .flatMap(hakija -> hakija.getHakutoiveet().stream())
                      .map(HakutoiveDTO::getHakukohdeOid)
                      .distinct()
                      .collect(Collectors.toList());
              LOG.info(
                  "Aloitetaan valintalaskennan tulosten haku {} hakijan {} hakukohteelle",
                  hakijat.size(),
                  hakukohdeOids.size());
              hakukohdeOids.forEach(
                  oid -> {
                    try {
                      results.put(oid, syotetytArvotByHakukohde(oid).get());
                    } catch (Exception e) {
                      LOG.error("Virhe haettaessa hakijoiden syötettyjä arvoja", e);
                      resultFuture.completeExceptionally(e);
                      throw new RuntimeException(e);
                    }
                  });
              LOG.info(
                  "Valintalaskennan tulosten haku {} hakijan {} hakukohteelle valmistui, kesto {}ms",
                  hakijat.size(),
                  hakukohdeOids.size(),
                  System.currentTimeMillis() - start);
              resultFuture.complete(results);
            });

    return resultFuture;
  }

  private CompletableFuture<String> muodostaHyvaksymiskirjeet(
      DokumenttiProsessi prosessi,
      CompletableFuture<ParametritParser> haunParametritF,
      CompletableFuture<List<HakijaDTO>> hakijatF,
      CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF,
      HyvaksymiskirjeDTO hyvaksymiskirjeDTO,
      String asiointikieli,
      boolean sahkoinenMassaposti,
      List<ContentStructureType> sisaltotyypit,
      KirjeenVastaanottaja kirjeenVastaanottaja) {
    CompletableFuture<Map<String, MetaHakukohde>> hakukohteetF =
        hakijatF.thenComposeAsync(this::kiinnostavatHakukohteet);
    CompletableFuture<Map<String, Koodi>> maatjavaltiot1F =
        koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
    CompletableFuture<Map<String, Koodi>> postinumerotF =
        koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.POSTI);
    CompletableFuture<Map<String, Optional<Osoite>>> osoitteetF =
        CompletableFuture.completedFuture(Collections.emptyMap());
    CompletableFuture<String> vakiosisaltoF =
        this.haeHakukohteenVakiosisalto(
            hyvaksymiskirjeDTO.getSisalto(), hyvaksymiskirjeDTO.getHakukohdeOid());

    CompletableFuture<Haku> hakuF = tarjontaAsyncResource.haeHaku(hyvaksymiskirjeDTO.getHakuOid());
    CompletableFuture<Map<String, Map<String, List<SyotettyArvoDTO>>>> syotetytArvotF =
        hakuF.thenComposeAsync(
            haku -> {
              if (haku.isKorkeakouluhaku()) {
                LOG.info(
                    "Hyväksymiskirjeiden muodostus: haku {} on korkeakouluhaku, joten ei haeta valintalaskennasta syötettyjä arvoja",
                    haku.oid);
                return CompletableFuture.completedFuture(Collections.emptyMap());
              } else {
                return hakijatF.thenComposeAsync(this::hakijoidenSyotetytArvot);
              }
            });

    return CompletableFuture.allOf(
            maatjavaltiot1F,
            postinumerotF,
            haunParametritF,
            hakijatF,
            hakemuksetF,
            syotetytArvotF,
            hakukohteetF,
            osoitteetF,
            vakiosisaltoF)
        .thenApplyAsync(
            v ->
                HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                    maatjavaltiot1F.join(),
                    postinumerotF.join(),
                    osoitteetF.join(),
                    hakukohteetF.join(),
                    hakijatF.join(),
                    hakemuksetF.join(),
                    syotetytArvotF.join(),
                    hyvaksymiskirjeDTO.getHakukohdeOid(),
                    hyvaksymiskirjeDTO.getHakuOid(),
                    Optional.ofNullable(asiointikieli),
                    vakiosisaltoF.join(),
                    hyvaksymiskirjeDTO.getTag(),
                    hyvaksymiskirjeDTO.getTemplateName(),
                    parsePalautusPvm(hyvaksymiskirjeDTO.getPalautusPvm(), haunParametritF.join()),
                    parsePalautusAika(hyvaksymiskirjeDTO.getPalautusAika(), haunParametritF.join()),
                    sahkoinenMassaposti,
                    sisaltotyypit,
                    kirjeenVastaanottaja))
        .thenComposeAsync(letterBatch -> letterBatchToViestintapalvelu(prosessi, letterBatch));
  }

  private CompletableFuture<String> muodostaJalkiohjauskirjeet(
      DokumenttiProsessi prosessi,
      CompletableFuture<List<HakijaDTO>> hakijatF,
      CompletableFuture<Map<String, HakemusWrapper>> hakemuksetF,
      JalkiohjauskirjeDTO jalkiohjauskirjeDTO,
      boolean sahkoinenMassaposti,
      boolean isKkHaku,
      List<ContentStructureType> sisaltotyypit,
      KirjeenVastaanottaja kirjeenVastaanottaja) {
    CompletableFuture<Map<String, MetaHakukohde>> hakukohteetF =
        hakijatF.thenComposeAsync(this::kiinnostavatHakukohteet);
    CompletableFuture<Map<String, Koodi>> maatjavaltiot1F =
        koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
    CompletableFuture<Map<String, Koodi>> postinumerotF =
        koodistoCachedAsyncResource.haeKoodistoAsync(KoodistoCachedAsyncResource.POSTI);

    CompletableFuture<Haku> hakuF = tarjontaAsyncResource.haeHaku(jalkiohjauskirjeDTO.getHakuOid());
    CompletableFuture<Map<String, Map<String, List<SyotettyArvoDTO>>>> syotetytArvotF =
        hakuF.thenComposeAsync(
            haku -> {
              if (haku.isKorkeakouluhaku()) {
                LOG.info(
                    "Jälkiohjauskirjeiden muodostus: haku {} on korkeakouluhaku, joten ei haeta valintalaskennasta syötettyjä arvoja",
                    haku.oid);
                return CompletableFuture.completedFuture(Collections.emptyMap());
              } else {
                return hakijatF.thenComposeAsync(this::hakijoidenSyotetytArvot);
              }
            });

    return CompletableFuture.allOf(
            maatjavaltiot1F, postinumerotF, hakijatF, hakemuksetF, hakukohteetF, syotetytArvotF)
        .thenApplyAsync(
            v ->
                JalkiohjauskirjeetKomponentti.teeJalkiohjauskirjeet(
                    maatjavaltiot1F.join(),
                    postinumerotF.join(),
                    jalkiohjauskirjeDTO.getKielikoodi(),
                    hakijatF.join(),
                    hakemuksetF.join(),
                    syotetytArvotF.join(),
                    hakukohteetF.join(),
                    jalkiohjauskirjeDTO.getHakuOid(),
                    jalkiohjauskirjeDTO.getTemplateName(),
                    jalkiohjauskirjeDTO.getSisalto(),
                    jalkiohjauskirjeDTO.getTag(),
                    sahkoinenMassaposti,
                    isKkHaku,
                    sisaltotyypit,
                    kirjeenVastaanottaja))
        .thenComposeAsync(letterBatch -> letterBatchToViestintapalvelu(prosessi, letterBatch));
  }

  private ProsessiId yhdenKirjeeranProsessi(
      ExecutorService executor,
      Function<DokumenttiProsessi, CompletableFuture<String>> task,
      String startMessage,
      String successMessage,
      String errorMessage) {
    return yhdenKirjeeranProsessi(
        executor, task, startMessage, successMessage, errorMessage, 1, TimeUnit.HOURS);
  }

  private ProsessiId yhdenKirjeeranProsessi(
      ExecutorService executor,
      Function<DokumenttiProsessi, CompletableFuture<String>> task,
      String startMessage,
      String successMessage,
      String errorMessage,
      long timeout,
      TimeUnit timeoutUnit) {
    DokumenttiProsessi prosessi = new DokumenttiProsessi("", "", "", Collections.emptyList());
    dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
    prosessi.setKokonaistyo(1);

    executor.execute(
        () -> {
          try {
            LOG.info(startMessage);
            String batchId = task.apply(prosessi).get(timeout, timeoutUnit);
            LOG.info(successMessage);
            prosessi.inkrementoiTehtyjaToita();
            prosessi.setDokumenttiId(batchId);
          } catch (Exception e) {
            LOG.error(errorMessage, e);
            prosessi.inkrementoiOhitettujaToita();
            prosessi
                .getPoikkeukset()
                .add(
                    Poikkeus.koostepalvelupoikkeus(
                        e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
          }
        });

    return prosessi.toProsessiId();
  }

  private static List<HakijaDTO> hyvaksytytHakijat(
      List<HakijaDTO> hakijat,
      Map<String, HakemusWrapper> hakemukset,
      String hakukohdeJossaHyvaksytty,
      String asiointikieli,
      boolean vainTulosEmailinKieltaneet) {
    List<HakijaDTO> l =
        hakijat.stream()
            .filter(hakija -> hakemukset.containsKey(hakija.getHakemusOid()))
            .filter(
                hakija ->
                    !vainTulosEmailinKieltaneet
                        || !hakemukset.get(hakija.getHakemusOid()).getLupaTulosEmail())
            .filter(
                hakija ->
                    asiointikieli == null
                        || asiointikieli.equalsIgnoreCase(
                            hakemukset.get(hakija.getHakemusOid()).getAsiointikieli()))
            .filter(
                hakija ->
                    hakija.getHakutoiveet().stream()
                        .filter(
                            hakutoive ->
                                hakukohdeJossaHyvaksytty == null
                                    || hakukohdeJossaHyvaksytty.equals(hakutoive.getHakukohdeOid()))
                        .flatMap(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream())
                        .anyMatch(valintatapajono -> valintatapajono.getTila().isHyvaksytty()))
            .collect(Collectors.toList());
    if (l.isEmpty()) {
      // Palauta lokalisointi-avain virheviestille käyttöliittymää varten
      throw new IllegalStateException("valintalaskenta.hyvaksymiskirjeet.eihakijoita");
    }
    return l;
  }

  private static List<HakijaDTO> hylatytHakijat(
      List<HakijaDTO> hakijat, Map<String, HakemusWrapper> hakemukset, String asiointikieli) {
    List<HakijaDTO> l =
        hakijat.stream()
            .filter(hakija -> hakemukset.containsKey(hakija.getHakemusOid()))
            .filter(
                hakija ->
                    asiointikieli == null
                        || asiointikieli.equalsIgnoreCase(
                            hakemukset.get(hakija.getHakemusOid()).getAsiointikieli()))
            .filter(
                hakija ->
                    hakija.getHakutoiveet().stream()
                        .flatMap(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream())
                        .noneMatch(
                            valintatapajono ->
                                valintatapajono.getTila().isHyvaksytty()
                                    || valintatapajono.getTila() == HakemuksenTila.PERUNUT))
            .collect(Collectors.toList());
    if (l.isEmpty()) {
      throw new IllegalStateException("Ei hylättyjä hakijoita");
    }
    return l;
  }

  private CompletableFuture<Map<String, MetaHakukohde>> kiinnostavatHakukohteet(
      List<HakijaDTO> hakijat) {
    return CompletableFutureUtil.sequence(
        hakijat.stream()
            .flatMap(hakija -> hakija.getHakutoiveet().stream())
            .map(HakutoiveDTO::getHakukohdeOid)
            .distinct()
            .collect(
                Collectors.toMap(
                    hakukohdeOid -> hakukohdeOid, kirjeetHakukohdeCache::haeHakukohdeAsync)));
  }

  private CompletableFuture<String> haeHakukohteenVakiosisalto(
      String annettuVakiosisalto, String hakukohdeOid) {
    if (annettuVakiosisalto != null) {
      return CompletableFuture.completedFuture(annettuVakiosisalto);
    }
    return this.tarjontaAsyncResource
        .haeHakukohde(hakukohdeOid)
        .thenComposeAsync(
            hakukohde ->
                CompletableFutureUtil.sequence(
                        hakukohde.toteutusOids.stream()
                            .map(tarjontaAsyncResource::haeToteutus)
                            .collect(Collectors.toList()))
                    .thenComposeAsync(
                        toteutukset ->
                            viestintapalveluAsyncResource.haeKirjepohja(
                                hakukohde.hakuOid,
                                hakukohde.tarjoajaOids.iterator().next(),
                                "hyvaksymiskirje",
                                KirjeetHakukohdeCache.getOpetuskieli(
                                    toteutukset.stream()
                                        .flatMap(toteutus -> toteutus.opetuskielet.stream())
                                        .collect(Collectors.toList())),
                                hakukohde.oid)))
        .thenComposeAsync(
            kirjepohjat ->
                kirjepohjat.stream()
                    .filter(kirjepohja -> VAKIOTEMPLATE.equals(kirjepohja.getName()))
                    .flatMap(kirjepohja -> kirjepohja.getTemplateReplacements().stream())
                    .filter(tdd -> VAKIODETAIL.equals(tdd.getName()))
                    .map(TemplateDetail::getDefaultValue)
                    .map(CompletableFuture::completedFuture)
                    .findAny()
                    .orElse(
                        CompletableFuture.failedFuture(
                            new RuntimeException(
                                String.format(
                                    "Ei %s tai %s templateDetailia hakukohteelle %s",
                                    VAKIOTEMPLATE, VAKIODETAIL, hakukohdeOid)))));
  }

  private CompletableFuture<Map<String, Optional<Osoite>>> hakukohteidenHakutoimistojenOsoitteet(
      Map<String, MetaHakukohde> hakukohteet, String asiointikieli) {
    return CompletableFutureUtil.sequence(
            hakukohteet.values().stream()
                .map(MetaHakukohde::getTarjoajaOid)
                .distinct()
                .collect(
                    Collectors.toMap(
                        tarjoajaOid -> tarjoajaOid, organisaatioAsyncResource::haeHakutoimisto)))
        .thenApplyAsync(
            hakutoimistot ->
                hakukohteet.entrySet().stream()
                    .collect(
                        Collectors.toMap(
                            Map.Entry::getKey,
                            e -> {
                              MetaHakukohde hakukohde = e.getValue();
                              String kieli =
                                  asiointikieli == null
                                      ? hakukohde.getHakukohteenKieli()
                                      : asiointikieli;
                              return hakutoimistot
                                  .getOrDefault(hakukohde.getTarjoajaOid(), Optional.empty())
                                  .flatMap(t -> Hakijapalvelu.osoite(t, kieli));
                            })));
  }

  private CompletableFuture<String> letterBatchToViestintapalvelu(
      DokumenttiProsessi prosessi, LetterBatch letterBatch) {
    return vieLetterBatch(prosessi, letterBatch)
        .thenComposeAsync(
            batchId -> {
              CompletableFuture<String> f = new CompletableFuture<>();
              Disposable s =
                  Observable.interval(pollingIntervalMillis, MILLISECONDS)
                      .flatMap(i -> letterBatchProcessingStatus(batchId))
                      .firstOrError()
                      .timeout(VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA.toMillis(), MILLISECONDS)
                      .subscribe(b -> f.complete(batchId), f::completeExceptionally);
              f.whenComplete((b, e) -> s.dispose());
              return f;
            });
  }

  private CompletableFuture<String> vieLetterBatch(
      DokumenttiProsessi prosessi, LetterBatch letterBatch) {
    if (prosessi.isKeskeytetty()) {
      throw new RuntimeException("Kirjeiden muodostus keskeytetty");
    }
    return viestintapalveluAsyncResource
        .vieLetterBatch(letterBatch)
        .thenApplyAsync(
            response -> {
              if (response.getStatus().equals(LetterResponse.STATUS_SUCCESS)) {
                return response.getBatchId();
              }
              throw new RuntimeException(
                  response.getErrors().entrySet().stream()
                      .map(e -> e.getKey() + ": " + e.getValue())
                      .collect(Collectors.joining("\n")));
            });
  }

  private Observable<String> letterBatchProcessingStatus(String batchId) {
    return Observable.fromFuture(viestintapalveluAsyncResource.haeLetterBatchStatus(batchId))
        .flatMap(
            response -> {
              if ("ready".equals(response.getStatus())) {
                return Observable.just(batchId);
              }
              if ("error".equals(response.getStatus())) {
                return Observable.error(
                    new RuntimeException("Kirjeiden vienti viestintäpalveluun epäonnistui"));
              }
              return Observable.empty();
            });
  }

  private List<ContentStructureType> getContentStructureTypesForHaunJalkiohjauskirjeet(
      boolean isKkHaku) {
    if (isKkHaku) {
      return Arrays.asList(ContentStructureType.accessibleHtml);
    } else {
      return Arrays.asList(ContentStructureType.letter, ContentStructureType.accessibleHtml);
    }
  }

  private static String parsePalautusPvm(String specifiedPvm, ParametritParser haunParametrit) {
    if (StringUtils.trimToNull(specifiedPvm) == null
        && haunParametrit.opiskelijanPaikanVastaanottoPaattyy() != null) {
      return new SimpleDateFormat("dd.MM.yyyy")
          .format(haunParametrit.opiskelijanPaikanVastaanottoPaattyy());
    }
    return specifiedPvm;
  }

  private static String parsePalautusAika(String specifiedAika, ParametritParser haunParametrit) {
    if (StringUtils.trimToNull(specifiedAika) == null
        && haunParametrit.opiskelijanPaikanVastaanottoPaattyy() != null) {
      return new SimpleDateFormat("HH.mm")
          .format(haunParametrit.opiskelijanPaikanVastaanottoPaattyy());
    }
    return specifiedAika;
  }
}
