package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Profile("default")
@Configuration
public class SijoitteluRouteConfig {

  @Bean
  public JatkuvaSijoitteluRouteImpl getJatkuvaSijoitteluRouteImpl(
      @Value("${jatkuvasijoittelu.autostart:true}") boolean autoStartup,
      @Value("${valintalaskentakoostepalvelu.jatkuvasijoittelu.intervalMinutes:5}")
          long jatkuvaSijoitteluPollIntervalInMinutes,
      SijoittelunSeurantaResource sijoittelunSeurantaResource,
      SchedulerFactoryBean schedulerFactoryBean) {
    return new JatkuvaSijoitteluRouteImpl(
        autoStartup,
        jatkuvaSijoitteluPollIntervalInMinutes,
        sijoittelunSeurantaResource,
        schedulerFactoryBean);
  }

  @Bean
  public SchedulerFactoryBean schedulerFactoryBean() {
    SchedulerFactoryBean scheduler = new SchedulerFactoryBean();
    scheduler.setApplicationContextSchedulerContextKey("applicationContext");
    return scheduler;
  }
}
