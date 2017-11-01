package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

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
        super(TimeUnit.MINUTES.toMillis(50));
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
            sijoitteluajoId = getWebClient()
                    .path(luontiUrl)
                    .accept(MediaType.WILDCARD_TYPE)
                    .async()
                    .get(Long.class).get();
        } catch (Exception e) {
            LOGGER.info("(Haku {}) sijoittelun rajapintakutsu epäonnistui", hakuOid);
        }
        //Jos rajapinta palauttaa -1 tai kutsu epäonnistuu, uutta sijoitteluajoa ei luotu. Ei aloiteta pollausta.
        if(sijoitteluajoId == -1) {
            LOGGER.error("Uuden sijoittelun luonti haulle {} epäonnistui", hakuOid);
            failureCallback.accept(new Exception());
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
                status = getWebClient()
                        .path(pollingUrl)
                        .accept(MediaType.WILDCARD_TYPE)
                        .async()
                        .get(String.class).get();

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
                LOGGER.info("Sijoittelussa {} haulle {} tapahtui virhe", sijoitteluajoId, hakuOid, e);
                failureCallback.accept(e);
                return;
            }
        }

    }
}
