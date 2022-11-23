package fi.vm.sade.valinta.kooste.tarjonta.route.impl;

import fi.vm.sade.valinta.kooste.kela.komponentti.impl.LinjakoodiKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.route.LinjakoodiRoute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TarjontaRouteImpl implements LinjakoodiRoute {

  private LinjakoodiKomponentti linjakoodiKomponentti;

  @Autowired
  public TarjontaRouteImpl(LinjakoodiKomponentti linjakoodiKomponentti) {
    this.linjakoodiKomponentti = linjakoodiKomponentti;
  }

  @Override
  public String haeLinjakoodi(String hakukohdeOid) {
    return linjakoodiKomponentti.haeLinjakoodi(hakukohdeOid);
  }
}
