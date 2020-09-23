package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import java.util.concurrent.TimeUnit;
import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("hakuTarjonnaltaKomponentti")
public class HaeHakuTarjonnaltaKomponentti {

  @Autowired private TarjontaAsyncResource tarjontaAsyncResource;

  public HakuV1RDTO getHaku(@Property("hakuOid") String hakuOid) {
    try {
      return tarjontaAsyncResource.haeHaku(hakuOid).get(5, TimeUnit.MINUTES);
    } catch (Exception e) {
      throw new RuntimeException(String.format("Haun %s haku epäonnistui", hakuOid), e);
    }
  }
}
