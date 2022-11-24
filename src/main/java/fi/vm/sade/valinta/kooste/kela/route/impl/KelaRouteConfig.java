package fi.vm.sade.valinta.kooste.kela.route.impl;

import fi.vm.sade.organisaatio.resource.api.KelaResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.*;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class KelaRouteConfig {

  @Bean(name = "kelaValvomo")
  public ValvomoServiceImpl<KelaProsessi> getValvomoServiceImpl() {
    return new ValvomoServiceImpl<KelaProsessi>();
  }

}
