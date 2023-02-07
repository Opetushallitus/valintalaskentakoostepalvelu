package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import static fi.vm.sade.tarjonta.service.types.TarjontaTila.JULKAISTU;

import com.google.common.collect.Collections2;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("hakukohteetTarjonnaltaKomponentti")
public class HaeHakukohteetTarjonnaltaKomponentti {

  private final TarjontaPublicService tarjontaService;

  @Autowired
  public HaeHakukohteetTarjonnaltaKomponentti(TarjontaPublicService tarjontaService) {
    this.tarjontaService = tarjontaService;
  }

  public Collection<HakukohdeTyyppi> haeHakukohteetTarjonnalta(String hakuOid) {
    return Collections2.filter(
        tarjontaService.haeTarjonta(hakuOid).getHakukohde(),
        hakukohde -> JULKAISTU == hakukohde.getHakukohteenTila());
  }
}
