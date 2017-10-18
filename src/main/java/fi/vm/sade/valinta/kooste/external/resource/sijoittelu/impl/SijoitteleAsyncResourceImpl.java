package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.sijoittelu.domain.HaunSijoittelunTila;
import fi.vm.sade.sijoittelu.domain.SijoitteluAjo;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import rx.Observable;

@Service
public class SijoitteleAsyncResourceImpl extends UrlConfiguredResource implements SijoitteleAsyncResource {

    private final static Logger LOGGER = LoggerFactory.getLogger(SijoitteleAsyncResourceImpl.class);
    public SijoitteleAsyncResourceImpl() {
        super(TimeUnit.MINUTES.toMillis(50));
    }

    @Override
    public void sijoittele(String hakuOid, Consumer<String> callback, Consumer<Throwable> failureCallback) {
        LOGGER.info("Sijoitellaan pollaten haulle {}", hakuOid);
        String luontiUrl = getUrl("sijoittelu-service.sijoittele", hakuOid);
        int secondsUntilNextPoll = 3;
        String status = "";
        AtomicReference<Boolean> done = new AtomicReference<>(false);
        Long sijoitteluId = -1L;

        try {
            sijoitteluId = getWebClient()
                    .path(luontiUrl)
                    .accept(MediaType.WILDCARD_TYPE)
                    .async()
                    .get(Long.class).get();
        } catch (Exception e) {
            LOGGER.info("(Haku {}) sijoittelun rajapintakutsu epäonnistui", hakuOid);
        }
        /*
        //Luodaan sijoittelu, saadaan palautusarvona sen id, jota käytetään pollattaessa toista rajapintaa.
        Observable apiReturns = postAsObservable(
                luontiUrl,
                Long.class,
                Entity.entity(Long.class, MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.TEXT_PLAIN_TYPE);
                    return client;
                });

        Long sijoitteluId = (Long) apiReturns.toBlocking().first();
        */

        //Jos rajapinta palauttaa id:ksi -1 (tai kutsu epäonnistuu), sijoittelua ei luotu. Lopetetaan tämän kutsun osalta.
        if(sijoitteluId == -1) {
            LOGGER.error("Uuden sijoittelun luonti haulle {} epäonnistui", hakuOid);
            failureCallback.accept(new Exception());
            return;
        }

        LOGGER.info("(Haku: {}) Sijoittelu on käynnistynyt id:llä {}. Pollataan kunnes se on päättynyt.", hakuOid, sijoitteluId);

        String pollingUrl = getUrl("sijoittelu-service.sijoittele.ajontila", sijoitteluId);

        while (!done.get()) {
            try {
                status = getWebClient()
                        .path(pollingUrl)
                        .accept(MediaType.WILDCARD_TYPE)
                        .async()
                        .get(String.class).get();

                LOGGER.info("Saatiin ajontila-rajapinnalta palautusarvo {}", status);
                if (HaunSijoittelunTila.VALMIS.equals(status)) {
                    LOGGER.info("#### Sijoittelu {} haulle {} on valmistunut", sijoitteluId, hakuOid);
                    callback.accept(status);
                    done.set(true);
                    return;
                }
                if (HaunSijoittelunTila.VIRHE.equals(status)) {
                    LOGGER.info("#### Sijoittelu {} haulle {} päättyi virheeseen", sijoitteluId, hakuOid);
                    failureCallback.accept(new Exception());
                    done.set(true);
                    return;
                }
            } catch (Exception e) {
                LOGGER.info("Sijoittelussa {} haulle {} tapahtui virhe", sijoitteluId, hakuOid, e);
                failureCallback.accept(e);
                return;
            }

            try {
                TimeUnit.SECONDS.sleep(secondsUntilNextPoll);
            } catch (Exception e) {
                throw new RuntimeException();
            }
            if (secondsUntilNextPoll < 30) {
                secondsUntilNextPoll += 3;
            }
        }

    }
}
