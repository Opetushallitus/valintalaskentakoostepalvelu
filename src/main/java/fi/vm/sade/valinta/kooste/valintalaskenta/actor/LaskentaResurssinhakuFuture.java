package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class LaskentaResurssinhakuFuture<R> {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaResurssinhakuFuture.class);
    private static final int MAX_RETRIES = 5;
    private final CompletableFuture<R> future;

    public LaskentaResurssinhakuFuture(Supplier<CompletableFuture<R>> source, PyynnonTunniste tunniste, boolean retry) {
        CompletableFuture<R> f;
        if (retry) {
            f = executeWithRetry(source, tunniste);
        } else {
            f = source.get();
        }

        long starTimeMillis = System.currentTimeMillis();

        this.future = f.whenComplete(lopputuloksenKasittelija(tunniste, starTimeMillis));
    }

    public static <R> CompletableFuture<R> executeWithRetry(Supplier<CompletableFuture<R>> action, PyynnonTunniste tunniste) {
        return action
            .get()
            .handleAsync((r, t) -> {
                if (t != null) {
                    return retry(action, t, 0, tunniste);
                } else {
                    return CompletableFuture.completedFuture(r);
                }
            }).thenCompose(java.util.function.Function.identity())
            .whenComplete((r, t) -> {
                if (t != null) {
                    LOG.info(String.format("%s : Kaikki uudelleenyritykset (%s kpl) on käytetty, ei yritetä enää. Virhe: %s", tunniste, MAX_RETRIES, t.getMessage()));
                }
            });
    }

    private static <R> CompletableFuture<R> retry(Supplier<CompletableFuture<R>> action, Throwable throwable, int retry, PyynnonTunniste tunniste) {
        int secondsToWaitMultiplier = 10;
        if (retry >= MAX_RETRIES) return CompletableFuture.failedFuture(throwable);
        return action
            .get()
            .handleAsync((r, t) -> {
                if (t != null) {
                    throwable.addSuppressed(t);
                    LOG.info(String.format("%s : Resurssin haussa tapahtui virhe %s, uudelleenyritys # %s", tunniste, t.getMessage(), retry));
                    Executor delayedExecutor = CompletableFuture.delayedExecutor(retry * secondsToWaitMultiplier, SECONDS);
                    return CompletableFuture.supplyAsync(() -> "OK", delayedExecutor)
                        .thenComposeAsync(x -> LaskentaResurssinhakuFuture.retry(action, throwable, retry + 1, tunniste));
                }
                return CompletableFuture.completedFuture(r);
            }).thenCompose(java.util.function.Function.identity());
    }

    private BiConsumer<R, Throwable> lopputuloksenKasittelija(PyynnonTunniste tunniste, long starTimeMillis) {
        return (r, error) -> {
            if (error != null) {
                long l = System.currentTimeMillis();
                long duration = l - starTimeMillis;
                long min = MILLISECONDS.toMinutes(duration);
                String message = String.format("(Uuid=%s) (kesto %s minuuttia) Resurssin %s lataus epäonnistui hakukohteelle %s", tunniste.uuid, min, tunniste.resurssi, tunniste.hakukohdeOid);
                String messageWithResponse = HttpExceptionWithResponse.appendWrappedResponse(message, error);
                LOG.error(messageWithResponse, error);
            } else {
                long l = System.currentTimeMillis();
                long duration = l - starTimeMillis;
                LOG.info(String.format("(Uuid=%s) (Kesto %s s) Saatiin resurssi %s hakukohteelle %s", tunniste.uuid, MILLISECONDS.toSeconds(duration), tunniste.resurssi, tunniste.hakukohdeOid));
            }
        };
    }

    public CompletableFuture<R> getFuture() {
        return future;
    }

    public static class PyynnonTunniste {
        public final String resurssi;
        public final String uuid;
        public final String hakukohdeOid;

        public PyynnonTunniste(String resurssi, String uuid, String hakukohdeOid) {
            this.resurssi = resurssi;
            this.uuid = uuid;
            this.hakukohdeOid = hakukohdeOid;
        }

        public PyynnonTunniste withNimi(String resurssi) {
            return new PyynnonTunniste(resurssi, this.uuid, this.hakukohdeOid);
        }

        @Override
        public String toString() {
            return "Resurssinhaku " + resurssi + " : uuid=" + uuid + ", hakukohdeOid=" + hakukohdeOid;
        }
    }
}