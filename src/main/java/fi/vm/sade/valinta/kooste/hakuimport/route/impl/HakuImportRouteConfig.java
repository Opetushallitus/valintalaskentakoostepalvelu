package fi.vm.sade.valinta.kooste.hakuimport.route.impl;

import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakuImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakukohdeImportKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class HakuImportRouteConfig {

  @Bean(name = "hakuImportValvomo")
  public ValvomoServiceImpl<HakuImportProsessi> getValvomoServiceImpl() {
    return new ValvomoServiceImpl<>();
  }

  @Bean
  public HakuImportRouteImpl getHakuImportAktivointiRoute(
    @Value("${valintalaskentakoostepalvelu.hakuimport.threadpoolsize:10}")
      Integer hakuImportThreadpoolSize,
    @Value("${valintalaskentakoostepalvelu.hakukohdeimport.threadpoolsize:10}")
      Integer hakukohdeImportThreadpoolSize,
    @Qualifier("hakuImportValvomo")
      ValvomoAdminService<HakuImportProsessi> hakuImportValvomo,
    SuoritaHakuImportKomponentti suoritaHakuImportKomponentti,
    ValintaperusteetAsyncResource valintaperusteetRestResource,
    SuoritaHakukohdeImportKomponentti tarjontaJaKoodistoHakukohteenHakuKomponentti) {
    return new HakuImportRouteImpl(hakuImportThreadpoolSize,
      hakukohdeImportThreadpoolSize,
      hakuImportValvomo,
      suoritaHakuImportKomponentti,
      valintaperusteetRestResource,
      tarjontaJaKoodistoHakukohteenHakuKomponentti);
  }

}
