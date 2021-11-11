package fi.vm.sade.valinta.kooste.util;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryUtil {
  private static final Logger LOG = LoggerFactory.getLogger(RetryUtil.class);

  public static <O> CompletableFuture<O> executeWithRetry(
      Supplier<CompletableFuture<O>> action,
      String tunniste,
      int retries,
      int secondsToWaitMultiplier) {
    return action
        .get()
        .handleAsync(
            (r, t) -> {
              if (t != null) {
                return retry(action, t, 0, retries, secondsToWaitMultiplier, tunniste);
              } else {
                return CompletableFuture.completedFuture(r);
              }
            })
        .thenCompose(java.util.function.Function.identity())
        .whenComplete(
            (r, t) -> {
              if (t != null) {
                LOG.info(
                    String.format(
                        "%s : Kaikki uudelleenyritykset (%s kpl) on käytetty, ei yritetä enää. Virhe: %s",
                        tunniste, retries, t.getMessage()));
              }
            });
  }

  private static <O> CompletableFuture<O> retry(
      Supplier<CompletableFuture<O>> action,
      Throwable throwable,
      int retry,
      int maxRetries,
      int secondsToWaitMultiplier,
      String tunniste) {
    if (retry >= maxRetries) return CompletableFuture.failedFuture(throwable);
    return action
        .get()
        .handleAsync(
            (r, t) -> {
              if (t != null) {
                throwable.addSuppressed(t);
                LOG.info(
                    String.format(
                        "%s : Resurssin haussa tapahtui virhe %s, uudelleenyritys # %s",
                        tunniste, t.getMessage(), retry));
                Executor delayedExecutor =
                    CompletableFuture.delayedExecutor(retry * secondsToWaitMultiplier, SECONDS);
                return CompletableFuture.supplyAsync(() -> "OK", delayedExecutor)
                    .thenComposeAsync(
                        x ->
                            retry(
                                action,
                                throwable,
                                retry + 1,
                                maxRetries,
                                secondsToWaitMultiplier,
                                tunniste));
              }
              return CompletableFuture.completedFuture(r);
            })
        .thenCompose(java.util.function.Function.identity());
  }
}
