package fi.vm.sade.valinta.kooste.tarjonta.route.impl;

import fi.vm.sade.valinta.kooste.kela.komponentti.impl.LinjakoodiKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TarjontaRouteImpl extends SpringRouteBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(TarjontaRouteImpl.class);

  private LinjakoodiKomponentti linjakoodiKomponentti;
  private HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeNimiTarjonnaltaKomponentti;

  @Autowired
  public TarjontaRouteImpl(
      LinjakoodiKomponentti linjakoodiKomponentti,
      HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeNimiTarjonnaltaKomponentti) {
    this.linjakoodiKomponentti = linjakoodiKomponentti;
    this.hakukohdeNimiTarjonnaltaKomponentti = hakukohdeNimiTarjonnaltaKomponentti;
  }

  @Override
  public void configure() throws Exception {
    from("direct:tarjontaNimiReitti").bean(hakukohdeNimiTarjonnaltaKomponentti);
    from("direct:linjakoodiReitti").bean(linjakoodiKomponentti);
  }
}
