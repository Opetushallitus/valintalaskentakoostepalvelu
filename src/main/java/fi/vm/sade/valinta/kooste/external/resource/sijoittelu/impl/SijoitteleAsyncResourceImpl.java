package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.sijoittelu.domain.SijoitteluajonTila;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SijoitteleAsyncResourceImpl implements SijoitteleAsyncResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(SijoitteleAsyncResourceImpl.class);
  private static final int MAX_POLL_INTERVALL_IN_SECONDS = 30;
  private static final int ADDED_WAIT_PER_POLL_IN_SECONDS = 3;

  private final RestCasClient restCasClient;

  private final UrlConfiguration urlConfiguration;

  @Autowired
  public SijoitteleAsyncResourceImpl(
      @Qualifier("SijoitteluServiceCasClient") RestCasClient restCasClient) {
    this.restCasClient = restCasClient;
    this.urlConfiguration = UrlConfiguration.getInstance();
  }

  @Override
  public void sijoittele(
      String hakuOid, Consumer<String> callback, Consumer<Throwable> failureCallback) {
    LOGGER.info("Sijoitellaan pollaten haulle {}", hakuOid);
    int secondsUntilNextPoll = 3;
    String status = "";
    AtomicReference<Boolean> done = new AtomicReference<>(false);
    Long sijoitteluajoId = -1L;

    // Luodaan sijoitteluajo, saadaan palautusarvona sen id, jota käytetään pollattaessa toista
    // rajapintaa.
    String luontiUrl = this.urlConfiguration.url("sijoittelu-service.sijoittele", hakuOid);
    try {
      sijoitteluajoId =
          this.restCasClient
              .get(luontiUrl, new TypeToken<Long>() {}, Map.of("Accept", "*/*"), 30 * 1000)
              .get();
    } catch (Exception e) {
      LOGGER.error(String.format("(Haku %s) sijoittelun rajapintakutsu epäonnistui", hakuOid), e);
    }
    // Jos rajapinta palauttaa -1 tai kutsu epäonnistuu, uutta sijoitteluajoa ei luotu. Ei aloiteta
    // pollausta.
    if (sijoitteluajoId == -1) {
      String msg =
          String.format(
              "Uuden sijoittelun luonti haulle %s epäonnistui: luontirajapinta palautti -1",
              hakuOid);
      LOGGER.error(msg);
      failureCallback.accept(new Exception(msg));
      return;
    }

    LOGGER.info(
        "(Haku: {}) Sijoittelu on käynnistynyt id:llä {}. Pollataan kunnes se on päättynyt.",
        hakuOid,
        sijoitteluajoId);
    String pollingUrl =
        this.urlConfiguration.url("sijoittelu-service.sijoittele.ajontila", sijoitteluajoId);
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
        status =
            this.restCasClient
                .get(pollingUrl, new TypeToken<String>() {}, Map.of("Accept", "*/*"), 15 * 1000)
                .get();

        LOGGER.info("Saatiin ajontila-rajapinnalta palautusarvo {}", status);
        if (SijoitteluajonTila.VALMIS.toString().equals(status)) {
          LOGGER.info("#### Sijoittelu {} haulle {} on valmistunut", sijoitteluajoId, hakuOid);
          callback.accept(status);
          done.set(true);
          return;
        }
        if (SijoitteluajonTila.VIRHE.toString().equals(status)) {
          LOGGER.error("#### Sijoittelu {} haulle {} päättyi virheeseen", sijoitteluajoId, hakuOid);
          failureCallback.accept(new Exception());
          done.set(true);
          return;
        }
      } catch (Exception e) {
        LOGGER.error(
            String.format("Sijoittelussa %s haulle %s tapahtui virhe", sijoitteluajoId, hakuOid),
            e);
        failureCallback.accept(e);
        return;
      }
    }
  }
}
