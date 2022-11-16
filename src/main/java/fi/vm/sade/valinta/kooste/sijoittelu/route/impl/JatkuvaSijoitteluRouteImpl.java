package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.JatkuvaSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.ModuloiPaivamaaraJaTunnit;
import fi.vm.sade.valinta.kooste.util.Formatter;
import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import fi.vm.sade.valinta.seuranta.sijoittelu.dto.SijoitteluDto;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.ws.rs.NotAuthorizedException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

public class JatkuvaSijoitteluRouteImpl implements JatkuvaSijoittelu {
  private static final Logger LOG = LoggerFactory.getLogger(JatkuvaSijoitteluRouteImpl.class);

  private final SijoitteleAsyncResource sijoitteluAsyncResource;
  private final SijoittelunSeurantaResource sijoittelunSeurantaResource;
  private final DelayQueue<DelayedSijoittelu> jatkuvaSijoitteluDelayedQueue;
  private final int DELAY_WHEN_FAILS = (int) TimeUnit.MINUTES.toMillis(45L);
  private final int VAKIO_AJOTIHEYS = 24;
  private final ConcurrentHashMap<String, Long> ajossaHakuOids;
  private final Timer timer;

  private Timer createFixedRateTimer(boolean start, long runEveryMinute) {
    Timer timer = new Timer("JatkuvaSijoitteluTimer");
    if(start) {
      TimerTask repeatedTask = new TimerTask() {
        public void run() {
          JatkuvaSijoitteluRouteImpl.this.teeJatkuvaSijoittelu();
        }
      };
      long delay = 1000L;
      long period = TimeUnit.MINUTES.toMillis(runEveryMinute);
      timer.scheduleAtFixedRate(repeatedTask, delay, period);
    }
    return timer;
  }

  @Autowired
  public JatkuvaSijoitteluRouteImpl(
      @Value("${jatkuvasijoittelu.autostart:true}")
      boolean autoStartup,
      @Value("${valintalaskentakoostepalvelu.jatkuvasijoittelu.intervalMinutes:5}")
      long jatkuvaSijoitteluPollIntervalInMinutes,
      SijoitteleAsyncResource sijoitteluAsyncResource,
      SijoittelunSeurantaResource sijoittelunSeurantaResource,
      @Qualifier("jatkuvaSijoitteluDelayedQueue")
          DelayQueue<DelayedSijoittelu> jatkuvaSijoitteluDelayedQueue) {
    this.sijoitteluAsyncResource = sijoitteluAsyncResource;
    this.sijoittelunSeurantaResource = sijoittelunSeurantaResource;
    this.jatkuvaSijoitteluDelayedQueue = jatkuvaSijoitteluDelayedQueue;
    this.ajossaHakuOids = new ConcurrentHashMap<>();
    this.timer = createFixedRateTimer(autoStartup, jatkuvaSijoitteluPollIntervalInMinutes);
  }

  public JatkuvaSijoitteluRouteImpl(
      boolean autoStartup,
      long jatkuvaSijoitteluPollIntervalInMinutes,
      SijoitteleAsyncResource sijoitteluAsyncResource,
      SijoittelunSeurantaResource sijoittelunSeurantaResource,
      DelayQueue<DelayedSijoittelu> jatkuvaSijoitteluDelayedQueue,
      ConcurrentHashMap<String, Long> ajossaHakuOids) {
    this.sijoitteluAsyncResource = sijoitteluAsyncResource;
    this.sijoittelunSeurantaResource = sijoittelunSeurantaResource;
    this.jatkuvaSijoitteluDelayedQueue = jatkuvaSijoitteluDelayedQueue;
    this.ajossaHakuOids = ajossaHakuOids;
    this.timer = createFixedRateTimer(autoStartup, jatkuvaSijoitteluPollIntervalInMinutes);
  }

  @Override
  public Collection<DelayedSijoittelu> haeJonossaOlevatSijoittelut() {
    return new ArrayList<>(jatkuvaSijoitteluDelayedQueue);
  }

  public void teeJatkuvaSijoittelu() {
    LOG.info("Jatkuvansijoittelun ajastin kaynnistyi");
    Map<String, SijoitteluDto> aktiivisetSijoittelut = getAktiivisetSijoittelut();
    LOG.info(
        "Jatkuvansijoittelun ajastin sai seurannalta {} aktiivista sijoittelua.",
        aktiivisetSijoittelut.size());
    poistaYritetytJaahylta();
    poistaSammutetutTaiJoidenAjankohtaEiOleViela(aktiivisetSijoittelut);
    laitaAjoon(aktiivisetSijoittelut);
  }

  private void laitaAjoon(Map<String, SijoitteluDto> aktiivisetSijoittelut) {
    aktiivisetSijoittelut.forEach(
        (hakuOid, sijoitteluDto) -> {
          boolean hakuEiJonossa =
              jatkuvaSijoitteluDelayedQueue.stream()
                      .filter(j -> hakuOid.equals(j.getHakuOid()))
                      .distinct()
                      .count()
                  == 0L;
          if (!ajossaHakuOids.containsKey(hakuOid) && hakuEiJonossa) {
            if (sijoitteluDto.getAloitusajankohta() == null
                || sijoitteluDto.getAjotiheys() == null) {
              LOG.warn(
                  "Jatkuvaa sijoittelua ei suoriteta haulle {} koska siltä puuttuu pakollisia parametreja.",
                  hakuOid);
            } else if (sijoitteluDto.getAjotiheys() <= 0) {
              LOG.warn(
                  "Jatkuvaa sijoittelua ei suoriteta haulle {} koska ajotieheys {} ei ole positiivinen.",
                  hakuOid,
                  sijoitteluDto.getAjotiheys());
            } else {
              DateTime asetusAjankohta = aloitusajankohtaTaiNyt(sijoitteluDto);
              Integer intervalli = ajotiheysTaiVakio(sijoitteluDto.getAjotiheys());
              DateTime suoritusAjankohta =
                  ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
                      asetusAjankohta, DateTime.now(), intervalli);
              LOG.info(
                  "Jatkuva sijoittelu haulle {} joka on asetettu {} intervallilla {} laitetaan suoritettavaksi seuraavan kerran {}",
                  hakuOid,
                  Formatter.paivamaara(asetusAjankohta.toDate()),
                  intervalli,
                  Formatter.paivamaara(suoritusAjankohta.toDate()));
              jatkuvaSijoitteluDelayedQueue.add(new DelayedSijoittelu(hakuOid, suoritusAjankohta));
            }
          }
        });
  }

  private void poistaSammutetutTaiJoidenAjankohtaEiOleViela(
      Map<String, SijoitteluDto> aktiivisetSijoittelut) {
    jatkuvaSijoitteluDelayedQueue.forEach(
        d -> {
          // Poistetaan tyojonosta passiiviset sijoittelut
          if (!aktiivisetSijoittelut.containsKey(d.getHakuOid())) {
            jatkuvaSijoitteluDelayedQueue.remove(d);
            LOG.warn(
                "Sijoittelu haulle {} poistettu ajastuksesta {}. Joko aloitusajankohtaa siirrettiin tulevaisuuteen tai jatkuvasijoittelu ei ole enaa aktiivinen haulle.",
                d.getHakuOid(),
                Formatter.paivamaara(new Date(d.getWhen())));
          }
          if (ajossaHakuOids.containsKey(d.getHakuOid())) {
            LOG.info(
                "Sijoittelu haulle {} poistettu ajastuksesta {}. Ylimaarainen sijoitteluajo tai joko parhaillaan ajossa tai epaonnistunut.",
                d.getHakuOid(),
                Formatter.paivamaara(new Date(d.getWhen())));
            jatkuvaSijoitteluDelayedQueue.remove(d);
          }
        });
  }

  private void poistaYritetytJaahylta() {
    ajossaHakuOids.forEach(
        (hakuOid, activationTime) -> {
          DateTime activated = new DateTime(activationTime);
          DateTime expires = new DateTime(activationTime).plusMillis(DELAY_WHEN_FAILS);
          boolean vanheneekoNyt = expires.isBeforeNow() || expires.isEqualNow();
          LOG.debug(
              "Aktivoitu {} ja vanhenee {} vanheneeko nyt {}",
              Formatter.paivamaara(activated.toDate()),
              Formatter.paivamaara(expires.toDate()),
              vanheneekoNyt);
          if (vanheneekoNyt) {
            LOG.debug("Jaahy haulle {} vanhentui", hakuOid);
            ajossaHakuOids.remove(hakuOid);
          }
        });
  }

  private Map<String, SijoitteluDto> getAktiivisetSijoittelut() {
    try {
      return _getAktiivisetSijoittelut();
    } catch (NotAuthorizedException e) {
      LOG.warn("Aktiivisten sijoittelujen haku epäonnistui. Yritetään uudelleen.", e);
      return _getAktiivisetSijoittelut(); // FIXME kill me OK-152!
    }
  }

  private Map<String, SijoitteluDto> _getAktiivisetSijoittelut() {
    return sijoittelunSeurantaResource.hae().stream()
        .filter(Objects::nonNull)
        .filter(
          SijoitteluDto::isAjossa)
        .filter(
            sijoitteluDto -> {
              DateTime aloitusajankohtaTaiNyt = aloitusajankohtaTaiNyt(sijoitteluDto);
              // jos aloitusajankohta on jo mennyt tai se on nyt niin sijoittelu on aktiivinen sen
              // osalta
              return laitetaankoJoTyoJonoonEliEnaaTuntiJaljellaAktivointiin(aloitusajankohtaTaiNyt);
            })
        .collect(Collectors.toMap(SijoitteluDto::getHakuOid, s -> s));
  }

  @Override
  public void kaynnistaJatkuvaSijoittelu(DelayedSijoittelu sijoittelu) {
    try {
      // Vie sijoittelu queuesta toita sijoitteluun sita mukaa kuin vanhenee
      final Long onkoAjossa =
        ajossaHakuOids.putIfAbsent(
          sijoittelu.getHakuOid(), System.currentTimeMillis());
      if (onkoAjossa == null) {
        LOG.error(
          "Jatkuvasijoittelu kaynnistyy nyt haulle {}",
          sijoittelu.getHakuOid());
        sijoitteluAsyncResource.sijoittele(
          sijoittelu.getHakuOid(),
          done -> {
            LOG.warn(
              "Jatkuva sijoittelu saatiin tehtya haulle {}",
              sijoittelu.getHakuOid());
            sijoittelunSeurantaResource.merkkaaSijoittelunAjetuksi(
              sijoittelu.getHakuOid());
            LOG.warn(
              "Jatkuva sijoittelu merkattiin ajetuksi haulle {}",
              sijoittelu.getHakuOid());
          },
          poikkeus -> {
            LOG.error(
              "Jatkuvan sijoittelun suorittaminen ei onnistunut haulle "
                + sijoittelu.getHakuOid(),
              poikkeus);
          });
      } else {
        LOG.error(
          "Jatkuvasijoittelu ei kaynnisty haulle {} koska uudelleen kaynnistysviivetta on viela jaljella",
          sijoittelu.getHakuOid());
      }
    } catch (Exception e) {
      LOG.error(
        "Jatkuvasijoittelu paattyi virheeseen {}\r\n{}",
        e.getMessage(),
        e.getStackTrace());
    }
  }

  public int ajotiheysTaiVakio(Integer ajotiheys) {
    return Optional.ofNullable(ajotiheys).orElse(VAKIO_AJOTIHEYS);
  }

  public boolean laitetaankoJoTyoJonoonEliEnaaTuntiJaljellaAktivointiin(DateTime aloitusAika) {
    return aloitusAika.isBefore(DateTime.now().plusHours(1));
  }

  private DateTime aloitusajankohtaTaiNyt(SijoitteluDto sijoitteluDto) {
    return new DateTime(
        Optional.ofNullable(sijoitteluDto.getAloitusajankohta()).orElse(new Date()));
  }
}
