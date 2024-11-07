package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static java.util.Collections.emptyList;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoJarjestyskriteereillaDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
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
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.SuoritustiedotDTO;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LaskentaResurssiProvider {

  private static final Logger LOG = LoggerFactory.getLogger(LaskentaResurssiProvider.class);

  private final ValintapisteAsyncResource valintapisteAsyncResource;
  private final ApplicationAsyncResource applicationAsyncResource;
  private final AtaruAsyncResource ataruAsyncResource;
  private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
  private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
  private final OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource;
  private final TarjontaAsyncResource tarjontaAsyncResource;
  private final KoskiService koskiService;
  private final HakemuksetConverterUtil hakemuksetConverterUtil;
  private final OhjausparametritAsyncResource ohjausparametritAsyncResource;
  private final ExecutorService executor = Executors.newWorkStealingPool();

  @Autowired
  public LaskentaResurssiProvider(
      @Value("${valintalaskentakoostepalvelu.laskennan.splittaus:1}") int splittaus,
      ApplicationAsyncResource applicationAsyncResource,
      AtaruAsyncResource ataruAsyncResource,
      ValintaperusteetAsyncResource valintaperusteetAsyncResource,
      SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
      TarjontaAsyncResource tarjontaAsyncResource,
      ValintapisteAsyncResource valintapisteAsyncResource,
      KoskiService koskiService,
      HakemuksetConverterUtil hakemuksetConverterUtil,
      OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource,
      OhjausparametritAsyncResource ohjausparametritAsyncResource) {
    this.applicationAsyncResource = applicationAsyncResource;
    this.ataruAsyncResource = ataruAsyncResource;
    this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
    this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
    this.tarjontaAsyncResource = tarjontaAsyncResource;
    this.valintapisteAsyncResource = valintapisteAsyncResource;
    this.koskiService = koskiService;
    this.hakemuksetConverterUtil = hakemuksetConverterUtil;
    this.oppijanumerorekisteriAsyncResource = oppijanumerorekisteriAsyncResource;
    this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
  }

  private static <T> CompletableFuture<T> storeDuration(
      CompletableFuture<T> future, String name, Map<String, String> durations) {
    Instant start = Instant.now();
    durations.put(name, "not finished");
    return future.thenApply(
        result -> {
          durations.put(name, Duration.between(start, Instant.now()).toMillis() + "");
          return result;
        });
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
            });
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

  public LaskentaResurssiProvider() {
    super();
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
    Map<String, String> durations = new HashMap<>();

    final CompletableFuture<ParametritDTO> parametritDTOFuture =
        storeDuration(
            ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid),
            "parametrit",
            durations);
    final CompletableFuture<Haku> hakuFuture =
        storeDuration(tarjontaAsyncResource.haeHaku(hakuOid), "haku", durations);

    SuoritustiedotDTO suoritustiedotDTO = new SuoritustiedotDTO();

    LaskentaResurssinhakuWrapper.PyynnonTunniste tunniste =
        new LaskentaResurssinhakuWrapper.PyynnonTunniste(
            "Please put individual resource source identifier here!", uuid, hakukohdeOid);

    CompletableFuture<List<ValintaperusteetDTO>> valintaperusteet;

    synchronized (this.valintaperusteetAsyncResource) {
      valintaperusteet =
          storeDuration(
              createResurssiFuture(
                  tunniste,
                  "valintaperusteetAsyncResource.haeValintaperusteet",
                  () ->
                      valintaperusteetAsyncResource.haeValintaperusteet(
                          hakukohdeOid, valinnanVaihe)),
              "valintaperusteet",
              durations);

      if (!isValintalaskentaKaytossa(valintaperusteet.join())) {
        throw new RuntimeException("Valintalaskenta ei ole käytössä hakukohteelle " + hakukohdeOid);
      }
    }

    CompletableFuture<List<HakemusWrapper>> hakemukset =
        hakuFuture.thenCompose(
            haku -> {
              if (haku.isHakemuspalvelu()) {
                boolean haetaanHarkinnanvaraisuudet =
                    haku.isAmmatillinenJaLukio() && haku.isKoutaHaku();
                return storeDuration(
                    createResurssiFuture(
                        tunniste,
                        "applicationAsyncResource.getApplications",
                        () ->
                            ataruAsyncResource.getApplicationsByHakukohde(
                                hakukohdeOid, haetaanHarkinnanvaraisuudet),
                        retryHakemuksetAndOppijat),
                    "ataruhakemukset",
                    durations);
              } else {
                return storeDuration(
                    createResurssiFuture(
                        tunniste,
                        "applicationAsyncResource.getApplicationsByOid",
                        () ->
                            applicationAsyncResource.getApplicationsByOids(
                                hakuOid, Collections.singletonList(hakukohdeOid)),
                        retryHakemuksetAndOppijat),
                    "hakuapphakemukset",
                    durations);
              }
            });

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
            });

    CompletableFuture<List<Oppija>> oppijasForOidsFromHakemukses =
        CompletableFuture.supplyAsync(
            () -> {
              synchronized (this.suoritusrekisteriAsyncResource) {
                CompletableFuture<List<Oppija>> oppijatF =
                    henkiloViitteet.thenComposeAsync(
                        hws -> {
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
                          return storeDuration(
                              createResurssiFuture(
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
                                                    masterToOriginal.get(
                                                        oppija.getOppijanumero())));
                                        return oppijat;
                                      }),
                              "suoritukset",
                              durations);
                        });
                return oppijatF.join();
              }
            });

    CompletableFuture<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes =
        storeDuration(
            createResurssiFuture(
                tunniste,
                "tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes",
                () -> tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(hakuOid)),
            "hakukohderyhmat",
            durations);
    CompletableFuture<PisteetWithLastModified> valintapisteetHakemuksille =
        hakemukset.thenComposeAsync(
            hakemusWrappers -> {
              List<String> hakemusOids =
                  hakemusWrappers.stream().map(HakemusWrapper::getOid).collect(Collectors.toList());
              return storeDuration(
                  createResurssiFuture(
                      tunniste,
                      "valintapisteAsyncResource.getValintapisteetWithHakemusOidsAsFuture",
                      () ->
                          valintapisteAsyncResource.getValintapisteetWithHakemusOidsAsFuture(
                              hakemusOids, auditSession),
                      retryHakemuksetAndOppijat),
                  "valintapisteet",
                  durations);
            });
    CompletableFuture<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmat =
        withHakijaRyhmat
            ? storeDuration(
                createResurssiFuture(
                    tunniste,
                    "valintaperusteetAsyncResource.haeHakijaryhmat",
                    () -> valintaperusteetAsyncResource.haeHakijaryhmat(hakukohdeOid)),
                "valintaperusteet",
                durations)
            : CompletableFuture.completedFuture(emptyList());
    CompletableFuture<Map<String, KoskiOppija>> koskiOppijaByOppijaOid =
        createResurssiFuture(
            tunniste,
            "koskiService.haeKoskiOppijat",
            () ->
                storeDuration(
                    koskiService.haeKoskiOppijat(
                        hakukohdeOid, valintaperusteet, hakemukset, suoritustiedotDTO, nyt),
                    "koskioppijat",
                    durations));

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
        .thenApply(
            laskeDTO -> {
              durations.put("Total", Duration.between(start, Instant.now()).toMillis() + "");
              LOG.info(
                  "Kestot: Hakukohde: "
                      + hakukohdeOid
                      + ": "
                      + durations.entrySet().stream()
                          .map(e -> e.getKey() + ":" + e.getValue())
                          .collect(Collectors.joining(", ")));

              laskeDTO.populoiSuoritustiedotHakemuksille(suoritustiedotDTO);
              return laskeDTO;
            })
        .orTimeout(9 * 60 * 1000l, TimeUnit.MILLISECONDS)
        .exceptionally(
            ex -> {
              if (ex instanceof TimeoutException) {
                durations.put("Total", Duration.between(start, Instant.now()).toMillis() + "");
                LOG.error(
                    "Kestot: (Timeout) Hakukohde: "
                        + hakukohdeOid
                        + ": "
                        + durations.entrySet().stream()
                            .map(e -> e.getKey() + ":" + e.getValue())
                            .collect(Collectors.joining(", ")));
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
