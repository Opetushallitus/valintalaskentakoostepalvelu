package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import fi.vm.sade.valinta.kooste.sijoittelu.dto.AjastettuSijoitteluInfo;
import fi.vm.sade.valinta.kooste.sijoittelu.job.AjastettuSijoitteluJob;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.JatkuvaSijoittelu;
import fi.vm.sade.valinta.kooste.util.Formatter;
import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import fi.vm.sade.valinta.seuranta.sijoittelu.dto.SijoitteluDto;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.NotAuthorizedException;
import org.joda.time.DateTime;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

public class JatkuvaSijoitteluRouteImpl implements JatkuvaSijoittelu {
  private static final Logger LOG = LoggerFactory.getLogger(JatkuvaSijoitteluRouteImpl.class);

  private final SijoittelunSeurantaResource sijoittelunSeurantaResource;
  private final int VAKIO_AJOTIHEYS = 24;
  private final Timer timer;
  private final Scheduler sijoitteluScheduler;

  private Map<String, AjastettuSijoitteluInfo> ajastetutSijoitteluInfot = new HashMap<>();

  private Timer createFixedRateTimer(boolean start, long runEveryMinute) {
    Timer timer = new Timer("JatkuvaSijoitteluTimer");
    if (start) {
      TimerTask repeatedTask =
          new TimerTask() {
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
      @Value("${jatkuvasijoittelu.autostart:true}") boolean autoStartup,
      @Value("${valintalaskentakoostepalvelu.jatkuvasijoittelu.intervalMinutes:5}")
          long jatkuvaSijoitteluPollIntervalInMinutes,
      SijoittelunSeurantaResource sijoittelunSeurantaResource,
      SchedulerFactoryBean schedulerFactoryBean) {
    this.sijoittelunSeurantaResource = sijoittelunSeurantaResource;
    this.timer = createFixedRateTimer(autoStartup, jatkuvaSijoitteluPollIntervalInMinutes);
    this.sijoitteluScheduler = schedulerFactoryBean.getScheduler();
    try {
      sijoitteluScheduler.start();
    } catch (SchedulerException se) {
      throw new RuntimeException("Sijoitteluiden ajastimen luominen epäonnistui", se);
    }
  }

  @Override
  public void teeJatkuvaSijoittelu() {
    LOG.info("Sijoitteluiden ajastin kaynnistyi");
    Map<String, SijoitteluDto> aktiivisetSijoittelut = getAktiivisetSijoittelut();
    LOG.info(
        "Sijoitteluiden ajastin sai seurannalta {} aktiivista sijoittelua.",
        aktiivisetSijoittelut.size());
    ajastetutSijoitteluInfot =
        aktiivisetSijoittelut.values().stream()
            .map(
                dto ->
                    new AjastettuSijoitteluInfo(
                        dto.getHakuOid(), dto.getAloitusajankohta(), dto.getAjotiheys()))
            .collect(Collectors.toMap(AjastettuSijoitteluInfo::getHakuOid, Function.identity()));
    poistaSammutetutTaiJoidenAjankohtaEiOleViela(aktiivisetSijoittelut);
    laitaAjoon(aktiivisetSijoittelut);
  }

  @Override
  public List<AjastettuSijoitteluInfo> haeAjossaOlevatAjastetutSijoittelut() {
    try {
      return sijoitteluScheduler.getJobGroupNames().stream()
          .map(hakuOid -> ajastetutSijoitteluInfot.get(hakuOid))
          .collect(Collectors.toList());
    } catch (SchedulerException se) {
      throw new RuntimeException(se);
    }
  }

  private void laitaAjoon(Map<String, SijoitteluDto> aktiivisetSijoittelut) {
    aktiivisetSijoittelut.forEach(
        (hakuOid, sijoitteluDto) -> {
          if (sijoitteluDto.getAloitusajankohta() == null || sijoitteluDto.getAjotiheys() == null) {
            LOG.warn(
                "Ajastettua sijoittelua ei suoriteta haulle {} koska siltä puuttuu pakollisia parametreja.",
                hakuOid);
          } else if (sijoitteluDto.getAjotiheys() <= 0) {
            LOG.warn(
                "Ajastettua sijoittelua ei suoriteta haulle {} koska ajotieheys {} ei ole positiivinen.",
                hakuOid,
                sijoitteluDto.getAjotiheys());
          } else {
            try {
              Date asetusAjankohta = aloitusajankohtaTaiNyt(sijoitteluDto).toDate();
              Integer intervalli = ajotiheysTaiVakio(sijoitteluDto.getAjotiheys());

              String jobName =
                  String.valueOf(asetusAjankohta.hashCode())
                      .concat("#")
                      .concat(String.valueOf(intervalli.hashCode()));

              if (!sijoitteluScheduler.checkExists(JobKey.jobKey(jobName, hakuOid))) {
                sijoitteluScheduler.deleteJobs(
                    new ArrayList<>(
                        sijoitteluScheduler.getJobKeys(GroupMatcher.groupEquals(hakuOid))));
                LOG.info(
                    "Ajastettu sijoittelu haulle {} joka on asetettu {} intervallilla {} ajastetaan",
                    hakuOid,
                    Formatter.paivamaara(asetusAjankohta),
                    intervalli);

                JobDetail sijoitteluJob =
                    JobBuilder.newJob(AjastettuSijoitteluJob.class)
                        .withIdentity(jobName, hakuOid)
                        .usingJobData("hakuOid", hakuOid)
                        .build();

                Trigger sijoitteluTrigger =
                    TriggerBuilder.newTrigger()
                        .withIdentity(jobName, jobName)
                        .startAt(asetusAjankohta)
                        .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInHours(intervalli)
                                .repeatForever())
                        .build();

                Set<Trigger> triggers = new HashSet<>();
                triggers.add(sijoitteluTrigger);

                sijoitteluScheduler.scheduleJob(sijoitteluJob, triggers, true);
              }
            } catch (SchedulerException se) {
              LOG.error("Ajastetun sijoittelun lisääminen haulle {} epäonnistui", hakuOid, se);
            }
          }
        });
  }

  private void poistaSammutetutTaiJoidenAjankohtaEiOleViela(
      Map<String, SijoitteluDto> aktiivisetSijoittelut) {
    try {
      sijoitteluScheduler
          .getJobGroupNames()
          .forEach(
              hakuOid -> {
                try {
                  if (!aktiivisetSijoittelut.containsKey(hakuOid)) {
                    List<JobKey> jobKeys =
                        new ArrayList<>(
                            sijoitteluScheduler.getJobKeys(GroupMatcher.groupEquals(hakuOid)));
                    sijoitteluScheduler.deleteJobs(jobKeys);
                    LOG.warn(
                        "Sijoittelu haulle {} poistettu ajastuksesta. Joko aloitusajankohtaa siirrettiin tulevaisuuteen tai jatkuvasijoittelu ei ole enaa aktiivinen haulle.",
                        hakuOid);
                  }
                } catch (SchedulerException se) {
                  LOG.error("Ajastetun sijoittelun siivoaminen haulle {} epäonnistui", hakuOid, se);
                }
              });

    } catch (Exception e) {
      LOG.error("Ajastettujen sijoittelujen siivous epäonnistui", e);
    }
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
        .filter(SijoitteluDto::isAjossa)
        .filter(
            sijoitteluDto -> {
              DateTime aloitusajankohtaTaiNyt = aloitusajankohtaTaiNyt(sijoitteluDto);
              // jos aloitusajankohta on jo mennyt tai se on nyt niin sijoittelu on aktiivinen sen
              // osalta
              return laitetaankoJoTyoJonoonEliEnaaTuntiJaljellaAktivointiin(aloitusajankohtaTaiNyt);
            })
        .collect(Collectors.toMap(SijoitteluDto::getHakuOid, s -> s));
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
