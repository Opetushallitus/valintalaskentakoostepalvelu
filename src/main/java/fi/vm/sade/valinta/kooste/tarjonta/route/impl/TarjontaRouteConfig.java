package fi.vm.sade.valinta.kooste.tarjonta.route.impl;

import fi.vm.sade.tarjonta.service.resources.HakuResource;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;
import fi.vm.sade.valinta.kooste.tarjonta.route.OrganisaatioRoute;
import fi.vm.sade.valinta.kooste.tarjonta.route.TarjontaHakuRoute;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class TarjontaRouteConfig {

  @Bean
  public OrganisaatioRoute getOrganisaatioRoute(OrganisaatioResource organisaatioResource) throws Exception {
    return organisaatioResource::getOrganisaatioByOID;
  }

  @Bean
  public TarjontaHakuRoute getTarjontaHakuRoute(HakuResource hakuResource) throws Exception {
    return hakuResource::getByOID;
  }
}
