package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.exception.TarjontaException;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import java.util.concurrent.TimeUnit;
import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Use proxy instead of calling bean:hakukohdeTarjonnaltaKomponentti! Proxy provides retries! */
@Component("hakukohdeNimiTarjonnaltaKomponentti")
public class HaeHakukohdeNimiTarjonnaltaKomponentti {
  private TarjontaAsyncResource tarjontaAsyncResource;

  @Autowired
  public HaeHakukohdeNimiTarjonnaltaKomponentti(TarjontaAsyncResource tarjontaAsyncResource) {
    this.tarjontaAsyncResource = tarjontaAsyncResource;
  }

  public HakukohdeV1RDTO haeHakukohdeNimi(@Property("hakukohdeOid") String hakukohdeOid) {
    if (hakukohdeOid == null) {
      throw new SijoittelupalveluException(
          "Sijoittelu palautti puutteellisesti luodun hakutoiveen! Hakukohteen tunniste puuttuu!");
    }
    try {
      return tarjontaAsyncResource.haeHakukohde(hakukohdeOid).get(5, TimeUnit.MINUTES);
    } catch (Exception e) {
      throw new TarjontaException("Tarjonnasta ei löydy hakukohdetta " + hakukohdeOid);
    }
  }
}
