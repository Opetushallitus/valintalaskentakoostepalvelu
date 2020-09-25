package fi.vm.sade.valinta.kooste.tarjonta.route.impl;

import fi.vm.sade.valinta.kooste.kela.komponentti.impl.LinjakoodiKomponentti;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TarjontaRouteImpl extends SpringRouteBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(TarjontaRouteImpl.class);

  private LinjakoodiKomponentti linjakoodiKomponentti;

  @Autowired
  public TarjontaRouteImpl(LinjakoodiKomponentti linjakoodiKomponentti) {
    this.linjakoodiKomponentti = linjakoodiKomponentti;
  }

  @Override
  public void configure() throws Exception {
    from("direct:linjakoodiReitti").bean(linjakoodiKomponentti);
  }
}
