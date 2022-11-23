package fi.vm.sade.valinta.kooste.dokumenttipalvelu.route.impl;

import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
public class DokumenttipalveluRouteImpl {
  private static final Logger LOG = LoggerFactory.getLogger(DokumenttipalveluRouteImpl.class);
  private final DokumenttiAsyncResource dokumenttiAsyncResource;

  @Autowired
  public DokumenttipalveluRouteImpl(DokumenttiAsyncResource dokumenttiAsyncResource) throws SchedulerException {
    this.dokumenttiAsyncResource = dokumenttiAsyncResource;
    scheduleCleanCronJob();
  }
  private void scheduleCleanCronJob() {
    String cronExpression = "0 0 0/2 * * ?";
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.initialize();
    ScheduledFuture<?> schedule = scheduler.schedule(this::execute, new CronTrigger(cronExpression));
  }

  public void execute() {
    try {
      dokumenttiAsyncResource.tyhjenna().get(1, TimeUnit.HOURS);
    } catch (Exception e) {
      LOG.info(
        "Dokumenttipalvelun tyhjennys-kutsu epäonnistui! Yritetään uudelleen.", e);
      try { // FIXME kill me OK-152
        dokumenttiAsyncResource.tyhjenna().get(1, TimeUnit.HOURS);
      } catch (Exception e2) {
        LOG.error("Dokumenttipalvelun tyhjennys-kutsu epäonnistui!", e2);
      }
    }
  }
}
