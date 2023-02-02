package fi.vm.sade.valinta.kooste.sijoittelu.job;

import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class AjastettuSijoitteluJob extends QuartzJobBean {

  private static final Logger LOG = LoggerFactory.getLogger(AjastettuSijoitteluJob.class);

  public static String JOB_GROUP = "AJASTETTU_SIJOITTELU";

  @Autowired
  public AjastettuSijoitteluJob() {}

  @Override
  public void executeInternal(JobExecutionContext context) throws JobExecutionException {
    try {
      ApplicationContext applicationContext =
          (ApplicationContext) context.getScheduler().getContext().get("applicationContext");
      SijoitteleAsyncResource sijoitteluAsyncResource =
          applicationContext.getBean(SijoitteleAsyncResource.class);
      SijoittelunSeurantaResource sijoittelunSeurantaResource =
          applicationContext.getBean(SijoittelunSeurantaResource.class);

      String hakuOid = (String) context.getJobDetail().getJobDataMap().get("hakuOid");
      try {
        LOG.error("Jatkuvasijoittelu kaynnistyy nyt haulle {}", hakuOid);
        sijoitteluAsyncResource.sijoittele(
            hakuOid,
            done -> {
              LOG.warn("Jatkuva sijoittelu saatiin tehtya haulle {}", hakuOid);
              sijoittelunSeurantaResource.merkkaaSijoittelunAjetuksi(hakuOid);
              LOG.warn("Jatkuva sijoittelu merkattiin ajetuksi haulle {}", hakuOid);
            },
            poikkeus -> {
              LOG.error(
                  "Jatkuvan sijoittelun suorittaminen ei onnistunut haulle " + hakuOid, poikkeus);
            });
      } catch (Exception e) {
        LOG.error(
            "Jatkuvasijoittelu paattyi virheeseen ({}) {}\r\n{}",
            hakuOid,
            e.getMessage(),
            e.getStackTrace());
      }
    } catch (Exception e) {
      throw new JobExecutionException(e);
    }
  }
}
