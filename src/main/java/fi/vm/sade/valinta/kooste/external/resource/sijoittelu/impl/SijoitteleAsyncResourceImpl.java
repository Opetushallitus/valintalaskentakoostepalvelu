package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import fi.vm.sade.sijoittelu.domain.SijoitteluajonTila;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.MediaType;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Service
public class SijoitteleAsyncResourceImpl extends UrlConfiguredResource implements SijoitteleAsyncResource {

    private final static Logger LOGGER = LoggerFactory.getLogger(SijoitteleAsyncResourceImpl.class);
    private final static int MAX_POLL_INTERVALL_IN_SECONDS = 30;
    private final static int ADDED_WAIT_PER_POLL_IN_SECONDS = 3;
    public SijoitteleAsyncResourceImpl() {
        super(MINUTES.toMillis(50));
    }

    @Override
    public void sijoittele(String hakuOid, Consumer<String> callback, Consumer<Throwable> failureCallback) {
        LOGGER.info("Sijoitellaan pollaten haulle {}", hakuOid);
        int secondsUntilNextPoll = 3;
        String status = "";
        AtomicReference<Boolean> done = new AtomicReference<>(false);
        Long sijoitteluajoId = -1L;

        //Luodaan sijoitteluajo, saadaan palautusarvona sen id, jota käytetään pollattaessa toista rajapintaa.
        String luontiUrl = getUrl("sijoittelu-service.sijoittele", hakuOid);
        try {
            sijoitteluajoId = this.<Long>getAsObservableLazily(luontiUrl, Long.class, client -> client.accept(MediaType.WILDCARD_TYPE))
                .timeout(30, SECONDS)
                .toBlocking()
                .first();
        } catch (Exception e) {
            LOGGER.info(String.format("(Haku %s) sijoittelun rajapintakutsu epäonnistui", hakuOid), e);
        }
        //Jos rajapinta palauttaa -1 tai kutsu epäonnistuu, uutta sijoitteluajoa ei luotu. Ei aloiteta pollausta.
        if (sijoitteluajoId == -1) {
            String msg = String.format("Uuden sijoittelun luonti haulle %s epäonnistui: luontirajapinta palautti -1", hakuOid);
            LOGGER.error(msg);
            failureCallback.accept(new Exception(msg));
            return;
        }

        LOGGER.info("(Haku: {}) Sijoittelu on käynnistynyt id:llä {}. Pollataan kunnes se on päättynyt.", hakuOid, sijoitteluajoId);
        String pollingUrl = getUrl("sijoittelu-service.sijoittele.ajontila", sijoitteluajoId);
        while (!done.get()) {
            try {
                TimeUnit.SECONDS.sleep(secondsUntilNextPoll);
            } catch (Exception e) {
                throw new RuntimeException();
            }
            if (secondsUntilNextPoll < MAX_POLL_INTERVALL_IN_SECONDS) {
                secondsUntilNextPoll += ADDED_WAIT_PER_POLL_IN_SECONDS;
            }
            try {
                status = this.<String>getAsObservableLazily(pollingUrl, String.class, webClient -> webClient.accept(MediaType.WILDCARD_TYPE))
                    .timeout(15, SECONDS)
                    .toBlocking()
                    .first();
                LOGGER.info("Saatiin ajontila-rajapinnalta palautusarvo {}", status);
                if (SijoitteluajonTila.VALMIS.toString().equals(status)) {
                    LOGGER.info("#### Sijoittelu {} haulle {} on valmistunut", sijoitteluajoId, hakuOid);
                    callback.accept(status);
                    done.set(true);
                    return;
                }
                if (SijoitteluajonTila.VIRHE.toString().equals(status)) {
                    LOGGER.info("#### Sijoittelu {} haulle {} päättyi virheeseen", sijoitteluajoId, hakuOid);
                    failureCallback.accept(new Exception());
                    done.set(true);
                    return;
                }
            } catch (Exception e) {
                LOGGER.info(String.format("Sijoittelussa %s haulle %s tapahtui virhe", sijoitteluajoId, hakuOid), e);
                failureCallback.accept(e);
                return;
            }
        }

    }
}
