package fi.vm.sade.valinta.kooste.valintatapajono.route.impl;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.HakukohdeResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoVientiRoute;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class ValintatapajonoConfig {

  @Bean
  public ValintatapajonoVientiRoute getValintatapajonoVientiRoute(
    ApplicationResource applicationResource,
    AtaruAsyncResource ataruAsyncResource,
    DokumenttiAsyncResource dokumenttiAsyncResource,
    TarjontaAsyncResource tarjontaAsyncResource,
    HakukohdeResource hakukohdeResource)
      throws Exception {
    return new ValintatapajonoVientiRouteImpl(
      applicationResource,
      ataruAsyncResource,
      dokumenttiAsyncResource,
      tarjontaAsyncResource,
      hakukohdeResource);
  }
}
