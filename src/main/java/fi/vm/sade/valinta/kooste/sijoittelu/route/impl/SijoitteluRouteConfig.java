package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluAktivointiRoute;
import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;

import java.util.concurrent.DelayQueue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class SijoitteluRouteConfig {

  @Bean
  public JatkuvaSijoitteluRouteImpl getJatkuvaSijoitteluRouteImpl(
    @Value("${jatkuvasijoittelu.autostart:true}")
      boolean autoStartup,
    @Value("${valintalaskentakoostepalvelu.jatkuvasijoittelu.intervalMinutes:5}")
      long jatkuvaSijoitteluPollIntervalInMinutes,
    SijoitteleAsyncResource sijoitteluAsyncResource,
    SijoittelunSeurantaResource sijoittelunSeurantaResource,
    @Qualifier("jatkuvaSijoitteluDelayedQueue")
      DelayQueue<DelayedSijoittelu> jatkuvaSijoitteluDelayedQueue) {
    return new JatkuvaSijoitteluRouteImpl(
      autoStartup,
      jatkuvaSijoitteluPollIntervalInMinutes,
      sijoitteluAsyncResource,
      sijoittelunSeurantaResource,
      jatkuvaSijoitteluDelayedQueue);
  }

  @Bean(name = "jatkuvaSijoitteluDelayedQueue")
  public DelayQueue<DelayedSijoittelu> createDelayQueue() {
    return new DelayQueue<>();
  }

}
