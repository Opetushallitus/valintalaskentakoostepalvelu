package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import fi.vm.sade.valinta.kooste.util.RetryUtil;
import fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaskentaResurssinhakuWrapper {
  private static final Logger LOG = LoggerFactory.getLogger(LaskentaResurssinhakuWrapper.class);

  public static <O> CompletableFuture<O> luoLaskentaResurssinHakuFuture(
      Supplier<CompletableFuture<O>> source, PyynnonTunniste tunniste, boolean retry) {
    CompletableFuture<O> f;
    if (retry) {
      f = RetryUtil.executeWithRetry(source, tunniste.toString(), 5, 10);
    } else {
      f = source.get();
    }

    long starTimeMillis = System.currentTimeMillis();

    return f.whenComplete(lopputuloksenKasittelija(tunniste, starTimeMillis));
  }

  private static <T> BiConsumer<T, Throwable> lopputuloksenKasittelija(
      PyynnonTunniste tunniste, long starTimeMillis) {
    return (r, error) -> {
      if (error != null) {
        long l = System.currentTimeMillis();
        long duration = l - starTimeMillis;
        long min = MILLISECONDS.toMinutes(duration);
        String message =
            String.format(
                "(Uuid=%s) (kesto %s minuuttia) Resurssin %s lataus epäonnistui hakukohteelle %s",
                tunniste.uuid, min, tunniste.resurssi, tunniste.hakukohdeOid);
        String messageWithResponse =
            HttpExceptionWithResponse.appendWrappedResponse(message, error);
        LOG.error(messageWithResponse, error);
      } else {
        long l = System.currentTimeMillis();
        long duration = l - starTimeMillis;
        LOG.info(
            String.format(
                "(Uuid=%s) (Kesto %s s) Saatiin resurssi %s hakukohteelle %s",
                tunniste.uuid,
                MILLISECONDS.toSeconds(duration),
                tunniste.resurssi,
                tunniste.hakukohdeOid));
      }
    };
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
