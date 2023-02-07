package fi.vm.sade.valinta.kooste.hakuimport.route.impl;

import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class HakuImportRouteConfig {

  @Bean("hakuImportValvomo")
  public ValvomoServiceImpl<HakuImportProsessi> getValvomoServiceImpl() {
    return new ValvomoServiceImpl<>();
  }
}
