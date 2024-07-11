package fi.vm.sade.valinta.kooste.external.resource.koski.impl;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppijaQuery;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppijaQueryResult;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KoskiAsyncResourceImpl implements KoskiAsyncResource {
  private static final Logger LOG = LoggerFactory.getLogger(KoskiAsyncResourceImpl.class);
  private static final Type OUTPUT_TYPE = new TypeToken<KoskiOppija>() {}.getType();
  private static final Type MASSALUOVUTUS_OUTPUT_TYPE =
      new TypeToken<KoskiOppijaQueryResult>() {}.getType();

  private static final DateTimeFormatter DATE_LIMIT_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private final int MAX_POLL_INTERVAL_IN_SECONDS = 30;

  private final KoskiAsyncHttpClient httpClient;
  private final String koskiUsername, koskiPassword;
  private final int maxOppijatPostSize, maxQueryResultPolls;
  private final Map<String, String> basicAuthenticationCredentials;
  private final UrlConfiguration urlConfiguration;

  @Autowired
  public KoskiAsyncResourceImpl(
      UrlConfiguration urlConfiguration,
      @Qualifier("KoskiHttpClient") KoskiAsyncHttpClient httpClient,
      @Value("${valintalaskentakoostepalvelu.koski.username}") String koskiUsername,
      @Value("${valintalaskentakoostepalvelu.koski.password}") String koskiPassword,
      @Value("${valintalaskentakoostepalvelu.koski.max.oppijat.post.size:1000}")
          int maxOppijatPostSize,
      @Value("${valintalaskentakoostepalvelu.koski.massaluovutus.max.polls}")
          int maxQueryResultPolls) {
    this.httpClient = httpClient;
    this.koskiUsername = koskiUsername;
    this.koskiPassword = koskiPassword;
    this.maxOppijatPostSize = maxOppijatPostSize;
    this.maxQueryResultPolls = maxQueryResultPolls;
    this.urlConfiguration = urlConfiguration;
    this.basicAuthenticationCredentials =
        Map.of("Authorization", basicAuth(koskiUsername, koskiPassword));
  }

  private static String basicAuth(String username, String password) {
    return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
  }

  @Override
  public CompletableFuture<Set<KoskiOppija>> findKoskiOppijat(
      List<String> oppijanumerot, LocalDate maxDate) {
    List<List<String>> oidBatches = Lists.partition(oppijanumerot, maxOppijatPostSize);
    AtomicInteger batchNumber = new AtomicInteger(1);
    int nbrOfBatches = oidBatches.size();
    int totalNumber = oppijanumerot.size();
    String dateLimit = DATE_LIMIT_FORMATTER.format(maxDate);
    String massaluovutusUrl = urlConfiguration.url("koski.massaluovutus.post");
    List<CompletableFuture<List<String>>> queries =
        oidBatches.stream()
            .map(
                batch ->
                    launchMassaluovutusQuery(
                            new KoskiOppijaQuery(batch, dateLimit),
                            massaluovutusUrl,
                            batchNumber.getAndIncrement(),
                            nbrOfBatches,
                            totalNumber)
                        .thenCompose(pollUrl -> pollMassaluovutusResults(1, 0, pollUrl)))
            .toList();
    return CompletableFutureUtil.sequence(queries)
        .thenApply(
            (List<List<String>> allResults) -> allResults.stream().flatMap(List::stream).toList())
        .thenCompose(
            resultUrls -> {
              List<CompletableFuture<KoskiOppija>> resultFetchers =
                  resultUrls.stream().map(this::getMassaluovutusResultItem).toList();
              return CompletableFutureUtil.sequenceToSet(resultFetchers)
                  .whenComplete(debugLogOpiskeluoikeusVersiot());
            });
  }

  private CompletableFuture<String> launchMassaluovutusQuery(
      KoskiOppijaQuery query, String url, int batchNumber, int nbrOfBatches, int totalNbr) {
    LOG.info(
        "Launching massaluovutus query for batch {}/{} of total {} oppijaOids",
        batchNumber,
        nbrOfBatches,
        totalNbr);
    return httpClient
        .<KoskiOppijaQueryResult>postJson(
            url, query, MASSALUOVUTUS_OUTPUT_TYPE, basicAuthenticationCredentials, 5 * 60)
        .thenApply(
            result -> {
              if (result.isFailed() || StringUtils.isEmpty(result.getResultsUrl())) {
                LOG.error("Massaluovutus query failed");
                throw new RuntimeException("Internal error in massaluovutus query");
              }
              return result.getResultsUrl();
            });
  }

  private CompletableFuture<List<String>> pollMassaluovutusResults(
      int pollInterval, int pollCount, String urlToPoll) {
    LOG.debug("Polling URL {} with interval {}", urlToPoll, pollInterval);
    return httpClient
        .<KoskiOppijaQueryResult>getJsonAfterDelay(
            urlToPoll,
            MASSALUOVUTUS_OUTPUT_TYPE,
            Duration.ofSeconds(pollInterval),
            basicAuthenticationCredentials,
            60)
        .thenCompose(
            result -> {
              if (result.isFailed()) {
                LOG.error("Massaluovutus status-query failed");
                return CompletableFuture.failedFuture(
                    new RuntimeException("Internal error in massaluovutus query"));
              }
              if (result.isCompleted()) {
                return CompletableFuture.completedFuture(result.getFiles());
              } else if (pollCount + 1 >= maxQueryResultPolls) {
                LOG.error(
                    "Massaluovutus query not completed within configured max poll count of {}",
                    pollCount + 1);
                return CompletableFuture.failedFuture(
                    new RuntimeException(
                        "Massaluovutus results not completed in configured time limit"));
              } else {
                if (pollInterval == MAX_POLL_INTERVAL_IN_SECONDS) {
                  LOG.warn("Massaluovutuksen {} valmistuminen kestää pitkään", urlToPoll);
                }
                return pollMassaluovutusResults(
                    Math.min(pollInterval * 2, MAX_POLL_INTERVAL_IN_SECONDS),
                    pollCount + 1,
                    urlToPoll);
              }
            });
  }

  private CompletableFuture<KoskiOppija> getMassaluovutusResultItem(String resultUrl) {
    Realm realm =
        new Realm.Builder(koskiUsername, koskiPassword)
            .setUsePreemptiveAuth(false)
            .setScheme(Realm.AuthScheme.NTLM)
            .build();
    return httpClient.getJson(
        resultUrl, OUTPUT_TYPE, basicAuthenticationCredentials, 60, true, realm);
  }

  private BiConsumer<Set<KoskiOppija>, Throwable> debugLogOpiskeluoikeusVersiot() {
    return (koskiOppijat, throwable) -> {
      if (LOG.isDebugEnabled() && koskiOppijat != null) {
        koskiOppijat.forEach(this::debugLogOpiskeluoikeusVersiot);
      }
    };
  }

  private void debugLogOpiskeluoikeusVersiot(KoskiOppija oppija) {
    oppija
        .getOpiskeluoikeudet()
        .forEach(
            o -> {
              JsonObject opiskeluoikeus = o.getAsJsonObject();
              LOG.debug(
                  String.format(
                      "Oppijan %s opiskeluoikeus %s : aikaleima=%s , versionumero=%s",
                      oppija.getOppijaOid(),
                      opiskeluoikeus.get("oid").getAsString(),
                      opiskeluoikeus.get("aikaleima").getAsString(),
                      opiskeluoikeus.get("versionumero").getAsInt()));
            });
  }
}
