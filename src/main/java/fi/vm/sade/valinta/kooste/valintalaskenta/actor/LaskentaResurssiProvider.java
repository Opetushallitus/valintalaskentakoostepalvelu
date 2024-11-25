package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static java.util.Collections.emptyList;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoJarjestyskriteereillaDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloViiteDto;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.dao.ParametritDao;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.SuoritustiedotDTO;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

@Service
public class LaskentaResurssiProvider {

  private static final Logger LOG = LoggerFactory.getLogger(LaskentaResurssiProvider.class);

  private final ValintapisteAsyncResource valintapisteAsyncResource;
  private final AtaruAsyncResource ataruAsyncResource;
  private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
  private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
  private final OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource;
  private final TarjontaAsyncResource tarjontaAsyncResource;
  private final KoskiService koskiService;
  private final HakemuksetConverterUtil hakemuksetConverterUtil;
  private final OhjausparametritAsyncResource ohjausparametritAsyncResource;
  private final ParametritDao parametritDao;
  private final ExecutorService executor = Executors.newWorkStealingPool(256);

  private final int NO_LIMIT_PERMITS = Integer.MAX_VALUE;
  private final ConcurrencyLimiter parametritLimiter =
      new ConcurrencyLimiter(NO_LIMIT_PERMITS, "parametrit", this.executor);
  private final ConcurrencyLimiter hakuLimiter =
      new ConcurrencyLimiter(NO_LIMIT_PERMITS, "haku", this.executor);
  private final ConcurrencyLimiter hakukohderyhmatLimiter =
      new ConcurrencyLimiter(NO_LIMIT_PERMITS, "hakukohderyhmat", this.executor);
  private final ConcurrencyLimiter valintapisteetLimiter =
      new ConcurrencyLimiter(NO_LIMIT_PERMITS, "valintapisteet", this.executor);
  private final ConcurrencyLimiter hakijaryhmatLimiter =
      new ConcurrencyLimiter(NO_LIMIT_PERMITS, "hakijaryhmat", this.executor);
  private final ConcurrencyLimiter koskioppijatLimiter =
      new ConcurrencyLimiter(16, "koskioppijat", this.executor);
  private final ConcurrencyLimiter ataruhakemuksetLimiter =
      new ConcurrencyLimiter(16, "ataruhakemukset", this.executor);
  private final ConcurrencyLimiter valintaperusteetLimiter =
      new ConcurrencyLimiter(16, "valintaperusteet", this.executor);
  private final ConcurrencyLimiter suorituksetLimiter =
      new ConcurrencyLimiter(1000, "suoritukset", this.executor);

  private final Map<String, ConcurrencyLimiter> limiters =
      Map.of(
          "parametritLimiter", parametritLimiter,
          "hakuLimiter", hakuLimiter,
          "ataruhakemuksetLimiter", ataruhakemuksetLimiter,
          "hakukohderyhmatLimiter", hakukohderyhmatLimiter,
          "valintapisteetLimiter", valintapisteetLimiter,
          "hakijaryhmatLimiter", hakijaryhmatLimiter,
          "koskioppijatLimiter", koskioppijatLimiter,
          "valintaperusteetLimiter", valintaperusteetLimiter,
          "suorituksetLimiter", suorituksetLimiter);

  private final CloudWatchClient cloudWatchClient;

  private final String environmentName;

  @Autowired
  public LaskentaResurssiProvider(
      AtaruAsyncResource ataruAsyncResource,
      ValintaperusteetAsyncResource valintaperusteetAsyncResource,
      SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
      TarjontaAsyncResource tarjontaAsyncResource,
      ValintapisteAsyncResource valintapisteAsyncResource,
      KoskiService koskiService,
      HakemuksetConverterUtil hakemuksetConverterUtil,
      OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource,
      OhjausparametritAsyncResource ohjausparametritAsyncResource,
      CloudWatchClient cloudWatchClient,
      ParametritDao parametritDao,
      @Value("${environment.name}") String environmentName) {
    this.ataruAsyncResource = ataruAsyncResource;
    this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
    this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
    this.tarjontaAsyncResource = tarjontaAsyncResource;
    this.valintapisteAsyncResource = valintapisteAsyncResource;
    this.koskiService = koskiService;
    this.hakemuksetConverterUtil = hakemuksetConverterUtil;
    this.oppijanumerorekisteriAsyncResource = oppijanumerorekisteriAsyncResource;
    this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
    this.cloudWatchClient = cloudWatchClient;
    this.environmentName = environmentName;
    this.parametritDao = parametritDao;
    this.lueParametrit();
  }

  static class ConcurrencyLimiter {

    public static final Duration TIMEOUT = Duration.ofMillis(Long.MIN_VALUE + 1);
    public static final Duration ERROR = Duration.ofMillis(Long.MIN_VALUE + 2);

    private int maxPermits;
    private final String vaihe;
    private final Semaphore semaphore;
    private final ExecutorService executor;
    private final AtomicInteger waiting;
    private final AtomicInteger active;

    public ConcurrencyLimiter(int permits, String vaihe, ExecutorService executor) {
      this.vaihe = vaihe;
      this.maxPermits = permits;
      this.semaphore = new Semaphore(permits, true);
      this.executor = executor;
      this.waiting = new AtomicInteger(0);
      this.active = new AtomicInteger(0);
    }

    public void setMaxPermits(int newPermits) {
      this.maxPermits = newPermits;
      int dPermits = newPermits - this.maxPermits;
      if (dPermits == 0) {
        return;
      }
      if (dPermits > 0) {
        this.semaphore.release(dPermits);
      } else {
        this.executor.submit(() -> this.semaphore.acquireUninterruptibly(dPermits));
      }
    }

    public int getWaiting() {
      return this.waiting.get();
    }

    public int getActive() {
      return this.active.get();
    }

    public String getVaihe() {
      return this.vaihe;
    }

    public static String asLabel(Duration duration) {
      if (duration == TIMEOUT) {
        return "timeout";
      } else if (duration == ERROR) {
        return "error";
      }
      return duration.toMillis() + "";
    }

    public <T> CompletableFuture<T> withConcurrencyLimit(
        int permits,
        Map<String, Duration> waitDurations,
        Map<String, Duration> invokeDurations,
        Supplier<CompletableFuture<T>> supplier) {

      Instant waitStart = Instant.now();
      this.waiting.incrementAndGet();
      return CompletableFuture.supplyAsync(
          () -> {
            this.semaphore.acquireUninterruptibly(Math.min(this.maxPermits, permits));
            this.waiting.decrementAndGet();
            this.active.incrementAndGet();
            try {
              Instant invokeStart = Instant.now();
              waitDurations.put(this.vaihe, Duration.between(waitStart, invokeStart));
              T result =
                  supplier
                      .get()
                      .exceptionallyAsync(
                          e -> {
                            if (e instanceof TimeoutException) {
                              invokeDurations.put(this.vaihe, TIMEOUT);
                            } else {
                              invokeDurations.put(this.vaihe, ERROR);
                            }
                            throw new CompletionException(e);
                          },
                          this.executor)
                      .join();
              invokeDurations.put(this.vaihe, Duration.between(invokeStart, Instant.now()));
              return result;
            } finally {
              semaphore.release(permits);
              this.active.decrementAndGet();
            }
          },
          this.executor);
    }
  }

  @Scheduled(initialDelay = 15, fixedDelay = 15, timeUnit = TimeUnit.SECONDS)
  public void lueParametrit() {
    this.executor.submit(
        () -> {
          this.parametritDao
              .lueParametrit()
              .forEach(
                  (k, v) -> {
                    if (this.limiters.containsKey(k)) {
                      this.limiters.get(k).setMaxPermits(Integer.parseInt(v));
                    }
                  });
        });
  }

  private void tallennaLokitJaMetriikat(
      String hakukohdeOid,
      Map<String, Duration> waitDurations,
      Map<String, Duration> invokeDurations) {
    Collection<MetricDatum> datums = new ArrayList<>();
    datums.addAll(
        waitDurations.entrySet().stream()
            .map(
                e ->
                    MetricDatum.builder()
                        .metricName("odotus")
                        .value((double) e.getValue().toMillis())
                        .storageResolution(60)
                        .dimensions(
                            List.of(Dimension.builder().name("vaihe").value(e.getKey()).build()))
                        .timestamp(Instant.now())
                        .unit(StandardUnit.MILLISECONDS)
                        .build())
            .collect(Collectors.toList()));

    datums.addAll(
        invokeDurations.entrySet().stream()
            .map(
                e -> {
                  if (e.getValue() == ConcurrencyLimiter.ERROR) {
                    return MetricDatum.builder()
                        .metricName("errors")
                        .value(1.0)
                        .storageResolution(60)
                        .dimensions(
                            List.of(Dimension.builder().name("vaihe").value(e.getKey()).build()))
                        .timestamp(Instant.now())
                        .unit(StandardUnit.COUNT)
                        .build();
                  } else if (e.getValue() == ConcurrencyLimiter.TIMEOUT) {
                    return MetricDatum.builder()
                        .metricName("timeouts")
                        .value(1.0)
                        .storageResolution(60)
                        .dimensions(
                            List.of(Dimension.builder().name("vaihe").value(e.getKey()).build()))
                        .timestamp(Instant.now())
                        .unit(StandardUnit.COUNT)
                        .build();
                  } else {
                    return MetricDatum.builder()
                        .metricName("kesto")
                        .value((double) e.getValue().toMillis())
                        .storageResolution(60)
                        .dimensions(
                            List.of(Dimension.builder().name("vaihe").value(e.getKey()).build()))
                        .timestamp(Instant.now())
                        .unit(StandardUnit.MILLISECONDS)
                        .build();
                  }
                })
            .collect(Collectors.toList()));

    CompletableFuture.supplyAsync(
        () ->
            this.cloudWatchClient.putMetricData(
                PutMetricDataRequest.builder()
                    .namespace(this.environmentName + "-valintalaskenta")
                    .metricData(datums)
                    .build()),
        this.executor);

    LOG.info(
        "Odotukset: Hakukohde: "
            + hakukohdeOid
            + ": "
            + waitDurations.entrySet().stream()
                .map(e -> e.getKey() + ":" + ConcurrencyLimiter.asLabel(e.getValue()))
                .collect(Collectors.joining(", ")));

    LOG.info(
        "Kestot: Hakukohde: "
            + hakukohdeOid
            + ": "
            + invokeDurations.entrySet().stream()
                .map(e -> e.getKey() + ":" + ConcurrencyLimiter.asLabel(e.getValue()))
                .collect(Collectors.joining(", ")));
  }

  @Scheduled(initialDelay = 15, fixedDelay = 15, timeUnit = TimeUnit.SECONDS)
  public void tallennaMaarat() {
    Collection<MetricDatum> datums = new ArrayList<>();

    datums.addAll(
        this.limiters.values().stream()
            .filter(limiter -> limiter.getActive() > 0)
            .map(
                limiter ->
                    MetricDatum.builder()
                        .metricName("active")
                        .value((double) limiter.getActive())
                        .storageResolution(1)
                        .dimensions(
                            List.of(
                                Dimension.builder()
                                    .name("vaihe")
                                    .value(limiter.getVaihe())
                                    .build()))
                        .timestamp(Instant.now())
                        .unit(StandardUnit.COUNT)
                        .build())
            .toList());

    datums.addAll(
        this.limiters.values().stream()
            .filter(limiter -> limiter.getWaiting() > 0)
            .map(
                limiter ->
                    MetricDatum.builder()
                        .metricName("waiting")
                        .value((double) limiter.getWaiting())
                        .storageResolution(1)
                        .dimensions(
                            List.of(
                                Dimension.builder()
                                    .name("vaihe")
                                    .value(limiter.getVaihe())
                                    .build()))
                        .timestamp(Instant.now())
                        .unit(StandardUnit.COUNT)
                        .build())
            .toList());

    if (!datums.isEmpty()) {
      CompletableFuture.supplyAsync(
          () ->
              this.cloudWatchClient.putMetricData(
                  PutMetricDataRequest.builder()
                      .namespace(this.environmentName + "-valintalaskenta")
                      .metricData(datums)
                      .build()),
          this.executor);
    }
  }

  private CompletableFuture<LaskeDTO> getLaskeDtoFuture(
      String uuid,
      CompletableFuture<Haku> haku,
      String hakukohdeOid,
      boolean isErillishaku,
      CompletableFuture<ParametritDTO> parametritDTO,
      boolean withHakijaRyhmat,
      CompletableFuture<List<ValintaperusteetDTO>> valintaperusteetF,
      CompletableFuture<List<Oppija>> oppijatF,
      CompletableFuture<Map<String, List<String>>> hakukohdeRyhmasForHakukohdesF,
      CompletableFuture<PisteetWithLastModified> valintapisteetForHakukohdesF,
      CompletableFuture<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmatF,
      CompletableFuture<List<HakemusWrapper>> hakemuksetF,
      CompletableFuture<Map<String, KoskiOppija>> koskiOppijaByOppijaOidF) {
    return CompletableFuture.allOf(
            haku,
            parametritDTO,
            valintapisteetForHakukohdesF,
            hakijaryhmatF,
            valintaperusteetF,
            hakemuksetF,
            oppijatF,
            hakukohdeRyhmasForHakukohdesF,
            koskiOppijaByOppijaOidF)
        .thenApplyAsync(
            x -> {
              List<ValintaperusteetDTO> valintaperusteet = valintaperusteetF.join();
              verifyJonokriteeritOrThrowError(uuid, hakukohdeOid, valintaperusteet);
              LOG.info(
                  "(Uuid: {}) Kaikki resurssit hakukohteelle {} saatu. Kootaan ja palautetaan LaskeDTO.",
                  uuid,
                  hakukohdeOid);

              Map<String, List<String>> ryhmatHakukohteittain =
                  hakukohdeRyhmasForHakukohdesF.join();
              PisteetWithLastModified pisteetWithLastModified = valintapisteetForHakukohdesF.join();
              List<HakemusWrapper> hakemukset = hakemuksetF.join();
              List<Oppija> oppijat = oppijatF.join();
              Map<String, KoskiOppija> koskiOppijatOppijanumeroittain =
                  koskiOppijaByOppijaOidF.join();
              koskiOppijatOppijanumeroittain.forEach(
                  (k, v) -> {
                    LOG.debug(String.format("Koskesta löytyi oppijalle %s datat: %s", k, v));
                  });

              if (!withHakijaRyhmat) {
                return new LaskeDTO(
                    uuid,
                    haku.join().isKorkeakouluhaku(),
                    isErillishaku,
                    hakukohdeOid,
                    hakemuksetConverterUtil.muodostaHakemuksetDTOfromHakemukset(
                        haku.join(),
                        hakukohdeOid,
                        ryhmatHakukohteittain,
                        hakemukset,
                        pisteetWithLastModified.valintapisteet,
                        oppijat,
                        parametritDTO.join(),
                        true,
                        true),
                    valintaperusteet);

              } else {
                return new LaskeDTO(
                    uuid,
                    haku.join().isKorkeakouluhaku(),
                    isErillishaku,
                    hakukohdeOid,
                    hakemuksetConverterUtil.muodostaHakemuksetDTOfromHakemukset(
                        haku.join(),
                        hakukohdeOid,
                        ryhmatHakukohteittain,
                        hakemukset,
                        pisteetWithLastModified.valintapisteet,
                        oppijat,
                        parametritDTO.join(),
                        true,
                        true),
                    valintaperusteet,
                    hakijaryhmatF.join());
              }
            },
            this.executor);
  }

  private boolean isValintalaskentaKaytossa(List<ValintaperusteetDTO> valintaperusteetList) {
    boolean jokinValintatapajonoKayttaaValintalaskentaa =
        valintaperusteetList.stream()
            .map(ValintaperusteetDTO::getValinnanVaihe)
            .flatMap(v -> v.getValintatapajono().stream())
            .anyMatch(ValintatapajonoJarjestyskriteereillaDTO::getKaytetaanValintalaskentaa);

    return jokinValintatapajonoKayttaaValintalaskentaa;
  }

  private void verifyJonokriteeritOrThrowError(
      String uuid, String hakukohdeOid, List<ValintaperusteetDTO> valintaperusteetList) {
    Predicate<? super ValintatapajonoJarjestyskriteereillaDTO>
        valintatapajonoHasPuuttuvaJonokriteeri =
            new Predicate<>() {
              @Override
              public boolean test(ValintatapajonoJarjestyskriteereillaDTO valintatapajono) {
                boolean kaytetaanValintalaskentaa = valintatapajono.getKaytetaanValintalaskentaa();
                boolean hasJarjestyskriteerit = !valintatapajono.getJarjestyskriteerit().isEmpty();

                return (kaytetaanValintalaskentaa && !hasJarjestyskriteerit)
                    || (!kaytetaanValintalaskentaa && hasJarjestyskriteerit);
              }
            };
    Optional<ValintatapajonoJarjestyskriteereillaDTO>
        valintatapajonoPuutteellisellaJonokriteerilla =
            valintaperusteetList.stream()
                .map(ValintaperusteetDTO::getValinnanVaihe)
                .flatMap(v -> v.getValintatapajono().stream())
                .filter(valintatapajonoHasPuuttuvaJonokriteeri)
                .findFirst();

    if (valintatapajonoPuutteellisellaJonokriteerilla.isPresent()) {
      ValintatapajonoJarjestyskriteereillaDTO valintatapajono =
          valintatapajonoPuutteellisellaJonokriteerilla.get();
      String errorMessage =
          String.format(
              "(Uuid: %s) Hakukohteen %s valintatapajonolla %s on joko valintalaskenta ilman jonokriteereitä tai jonokriteereitä ilman valintalaskentaa, joten valintalaskentaa ei voida jatkaa ja se keskeytetään",
              uuid, hakukohdeOid, valintatapajono.getOid());
      LOG.error(errorMessage);
      throw new RuntimeException(errorMessage);
    }
  }

  public CompletableFuture<LaskeDTO> fetchResourcesForOneLaskenta(
      final String uuid,
      final String hakuOid,
      final String hakukohdeOid,
      final Integer valinnanVaihe,
      AuditSession auditSession,
      boolean isErillishaku,
      boolean retryHakemuksetAndOppijat,
      boolean withHakijaRyhmat,
      Date nyt) {

    Instant start = Instant.now();
    Map<String, Duration> waitDurations = new ConcurrentHashMap<>();
    Map<String, Duration> invokeDurations = new ConcurrentHashMap<>();

    final CompletableFuture<ParametritDTO> parametritDTOFuture =
        this.parametritLimiter.withConcurrencyLimit(
            1,
            waitDurations,
            invokeDurations,
            () -> ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid));
    final CompletableFuture<Haku> hakuFuture =
        this.hakuLimiter.withConcurrencyLimit(
            1, waitDurations, invokeDurations, () -> tarjontaAsyncResource.haeHaku(hakuOid));

    SuoritustiedotDTO suoritustiedotDTO = new SuoritustiedotDTO();

    LaskentaResurssinhakuWrapper.PyynnonTunniste tunniste =
        new LaskentaResurssinhakuWrapper.PyynnonTunniste(
            "Please put individual resource source identifier here!", uuid, hakukohdeOid);

    CompletableFuture<List<ValintaperusteetDTO>> valintaperusteet =
        this.valintaperusteetLimiter.withConcurrencyLimit(
            1,
            waitDurations,
            invokeDurations,
            () ->
                createResurssiFuture(
                        tunniste,
                        "valintaperusteetAsyncResource.haeValintaperusteet",
                        () ->
                            valintaperusteetAsyncResource.haeValintaperusteet(
                                hakukohdeOid, valinnanVaihe))
                    .thenApplyAsync(
                        vp -> {
                          if (!isValintalaskentaKaytossa(vp)) {
                            throw new RuntimeException(
                                "Valintalaskenta ei ole käytössä hakukohteelle " + hakukohdeOid);
                          }
                          return vp;
                        },
                        this.executor));

    CompletableFuture<List<HakemusWrapper>> hakemukset =
        hakuFuture.thenComposeAsync(
            haku -> {
              if (haku.isHakemuspalvelu()) {
                boolean haetaanHarkinnanvaraisuudet =
                    haku.isAmmatillinenJaLukio() && haku.isKoutaHaku();
                return this.ataruhakemuksetLimiter.withConcurrencyLimit(
                    1,
                    waitDurations,
                    invokeDurations,
                    () ->
                        createResurssiFuture(
                            tunniste,
                            "applicationAsyncResource.getApplications",
                            () ->
                                ataruAsyncResource.getApplicationsByHakukohde(
                                    hakukohdeOid, haetaanHarkinnanvaraisuudet),
                            retryHakemuksetAndOppijat));
              } else {
                throw new RuntimeException(
                    "HakuApp lähtötietoja ei tueta enää: hakukohde " + hakukohdeOid);
              }
            },
            this.executor);

    CompletableFuture<List<HenkiloViiteDto>> henkiloViitteet =
        hakemukset.thenComposeAsync(
            hws -> {
              List<HenkiloViiteDto> viitteet =
                  hws.stream()
                      .map(
                          hw ->
                              new HenkiloViiteDto(hw.getApplicationPersonOid(), hw.getPersonOid()))
                      .collect(Collectors.toList());
              return CompletableFuture.completedFuture(viitteet);
            },
            this.executor);

    CompletableFuture<List<Oppija>> oppijasForOidsFromHakemukses =
        henkiloViitteet.thenComposeAsync(
            hws ->
                this.suorituksetLimiter.withConcurrencyLimit(
                    hws.size() + 50,
                    waitDurations,
                    invokeDurations,
                    () -> {
                      LOG.info(
                          "Haetaan suoritukset hakukohteen "
                              + hakukohdeOid
                              + " "
                              + hws.size()
                              + " oppijalle");

                      LOG.info("Got henkiloViittees: {}", hws);
                      Map<String, String> masterToOriginal =
                          hws.stream()
                              .collect(
                                  Collectors.toMap(
                                      HenkiloViiteDto::getMasterOid,
                                      HenkiloViiteDto::getHenkiloOid));
                      List<String> oppijaOids = new ArrayList<>(masterToOriginal.keySet());
                      LOG.info(
                          "Got personOids from hakemukses and getting Oppijas for these: {} for hakukohde {}",
                          oppijaOids,
                          hakukohdeOid);
                      return createResurssiFuture(
                              tunniste,
                              "suoritusrekisteriAsyncResource.getSuorituksetByOppijas",
                              () ->
                                  suoritusrekisteriAsyncResource.getSuorituksetByOppijas(
                                      oppijaOids, hakuOid, true),
                              retryHakemuksetAndOppijat)
                          .thenApply(
                              oppijat -> {
                                oppijat.forEach(
                                    oppija ->
                                        oppija.setOppijanumero(
                                            masterToOriginal.get(oppija.getOppijanumero())));
                                return oppijat;
                              });
                    }),
            this.executor);

    CompletableFuture<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes =
        this.hakukohderyhmatLimiter.withConcurrencyLimit(
            1,
            waitDurations,
            invokeDurations,
            () ->
                createResurssiFuture(
                    tunniste,
                    "tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes",
                    () -> tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(hakuOid)));

    CompletableFuture<PisteetWithLastModified> valintapisteetHakemuksille =
        hakemukset.thenComposeAsync(
            hakemusWrappers -> {
              List<String> hakemusOids =
                  hakemusWrappers.stream().map(HakemusWrapper::getOid).collect(Collectors.toList());
              return this.valintapisteetLimiter.withConcurrencyLimit(
                  1,
                  waitDurations,
                  invokeDurations,
                  () ->
                      createResurssiFuture(
                          tunniste,
                          "valintapisteAsyncResource.getValintapisteetWithHakemusOidsAsFuture",
                          () ->
                              valintapisteAsyncResource.getValintapisteetWithHakemusOidsAsFuture(
                                  hakemusOids, auditSession),
                          retryHakemuksetAndOppijat));
            },
            this.executor);

    CompletableFuture<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmat =
        withHakijaRyhmat
            ? this.hakijaryhmatLimiter.withConcurrencyLimit(
                1,
                waitDurations,
                invokeDurations,
                () ->
                    createResurssiFuture(
                        tunniste,
                        "valintaperusteetAsyncResource.haeHakijaryhmat",
                        () -> valintaperusteetAsyncResource.haeHakijaryhmat(hakukohdeOid)))
            : CompletableFuture.completedFuture(emptyList());
    CompletableFuture<Map<String, KoskiOppija>> koskiOppijaByOppijaOid =
        CompletableFuture.allOf(valintaperusteet, hakemukset)
            .thenComposeAsync(
                unused ->
                    this.koskioppijatLimiter.withConcurrencyLimit(
                        1,
                        waitDurations,
                        invokeDurations,
                        () ->
                            createResurssiFuture(
                                tunniste,
                                "koskiService.haeKoskiOppijat",
                                () ->
                                    koskiService.haeKoskiOppijat(
                                        hakukohdeOid,
                                        valintaperusteet,
                                        hakemukset,
                                        suoritustiedotDTO,
                                        nyt))),
                this.executor);

    LOG.info(
        "(Uuid: {}) Odotetaan kaikkien resurssihakujen valmistumista hakukohteelle {}, jotta voidaan palauttaa ne yhtenä pakettina.",
        uuid,
        hakukohdeOid);
    return getLaskeDtoFuture(
            uuid,
            hakuFuture,
            hakukohdeOid,
            isErillishaku,
            parametritDTOFuture,
            withHakijaRyhmat,
            valintaperusteet,
            oppijasForOidsFromHakemukses,
            hakukohdeRyhmasForHakukohdes,
            valintapisteetHakemuksille,
            hakijaryhmat,
            hakemukset,
            koskiOppijaByOppijaOid)
        .thenApplyAsync(
            laskeDTO -> {
              laskeDTO.populoiSuoritustiedotHakemuksille(suoritustiedotDTO);
              invokeDurations.put("Total", Duration.between(start, Instant.now()));
              LOG.info(
                  "Haettiin lähtötiedot hakukohteelle "
                      + hakukohdeOid
                      + ", start: "
                      + start
                      + ", end: "
                      + Instant.now());
              this.tallennaLokitJaMetriikat(hakukohdeOid, waitDurations, invokeDurations);
              return laskeDTO;
            },
            this.executor)
        .orTimeout(9 * 60 * 1000l, TimeUnit.MILLISECONDS)
        .exceptionally(
            ex -> {
              if (ex instanceof TimeoutException) {
                invokeDurations.put("Total (timeout)", Duration.between(start, Instant.now()));
                this.tallennaLokitJaMetriikat(hakukohdeOid, waitDurations, invokeDurations);
              }
              throw new RuntimeException(ex);
            });
  }

  private <T> CompletableFuture<T> createResurssiFuture(
      LaskentaResurssinhakuWrapper.PyynnonTunniste tunniste,
      String resurssi,
      Supplier<CompletableFuture<T>> sourceFuture,
      boolean retry) {
    return LaskentaResurssinhakuWrapper.luoLaskentaResurssinHakuFuture(
        sourceFuture, tunniste.withNimi(resurssi), retry);
  }

  private <T> CompletableFuture<T> createResurssiFuture(
      LaskentaResurssinhakuWrapper.PyynnonTunniste tunniste,
      String resurssi,
      Supplier<CompletableFuture<T>> sourceFuture) {
    return createResurssiFuture(tunniste, resurssi, sourceFuture, false);
  }
}
