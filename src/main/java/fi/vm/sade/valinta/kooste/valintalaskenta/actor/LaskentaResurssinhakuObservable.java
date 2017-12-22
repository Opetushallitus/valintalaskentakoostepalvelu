package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static rx.Observable.range;
import static rx.Observable.timer;

import fi.vm.sade.valinta.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.http.ObservableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

public class LaskentaResurssinhakuObservable<R> {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaResurssinhakuObservable.class);
    private final Observable<R> observable;
    private final PyynnonTunniste tunniste;

    public LaskentaResurssinhakuObservable(Observable<R> source, PyynnonTunniste tunniste, boolean retry) {
        Observable<R> runOnlyOnceObservable = ObservableUtil.wrapAsRunOnlyOnceObservable(source);
        if (retry) {
            this.observable = runOnlyOnceObservable.retryWhen(createRetryer(tunniste));
        } else {
            this.observable = runOnlyOnceObservable;
        }
        long starTimeMillis = System.currentTimeMillis();
        this.observable.subscribe(resurssiOK(starTimeMillis, tunniste), resurssiException(starTimeMillis, tunniste));
        this.tunniste = tunniste;
    }

    private static Func1<Observable<? extends Throwable>, Observable<?>> createRetryer(PyynnonTunniste tunniste) {
        int maxRetries = 2;
        int secondsToWaitMultiplier = 5;
        return errors -> errors.zipWith(range(1, maxRetries+1), (n, i) -> {
            if (i <= maxRetries) {
                LOG.info(String.format("%s : Uudelleenyritys # %s", tunniste, i));
                return Observable.timer(i * secondsToWaitMultiplier, SECONDS);
            } else {
                LOG.info(String.format("%s : Kaikki uudelleenyritykset (%s kpl) on käytetty, ei yritetä enää.", tunniste, maxRetries));
                return Observable.error(n);
            }})
            .flatMap(i -> {
                LOG.warn(tunniste.toString() + " retry number or error " + i);
                return i;
            });
    }

    private Action1<? super Object> resurssiOK(long startTime, PyynnonTunniste tunniste) {
        return r -> {
            long l = System.currentTimeMillis();
            long duration = l - startTime;
            LOG.info(String.format("(Uuid=%s) (Kesto %s s) Saatiin resurssi %s hakukohteelle %s", tunniste.uuid, MILLISECONDS.toSeconds(duration), tunniste.resurssi, tunniste.hakukohdeOid));
        };
    }

    private Action1<Throwable> resurssiException(long startTime, PyynnonTunniste tunniste) {
        return error -> {
            long l = System.currentTimeMillis();
            long duration = l - startTime;
            long min = MILLISECONDS.toMinutes(duration);
            String message = String.format("(Uuid=%s) (kesto %s minuuttia) Resurssin %s lataus epäonnistui hakukohteelle %s", tunniste.uuid, min, tunniste.resurssi, tunniste.hakukohdeOid);
            String messageWithResponse = HttpExceptionWithResponse.appendWrappedResponse(message, error);
            LOG.error(messageWithResponse, error);
        };
    }

    public Observable<R> getObservable() {
        return observable;
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

    @Override
    public String toString() {
        return getClass().getName() + ": " + tunniste.toString();
    }
}
