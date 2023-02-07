package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** User: wuoti Date: 20.5.2013 Time: 10.46 */
@Component("suoritaHakuImportKomponentti")
public class SuoritaHakuImportKomponentti {

  private static final Logger LOG = LoggerFactory.getLogger(SuoritaHakuImportKomponentti.class);

  @Autowired private TarjontaAsyncResource tarjontaAsyncResource;

  public Collection<String> suoritaHakukohdeImport(String hakuOid) {
    try {
      Set<String> hakukohteet =
          tarjontaAsyncResource.haunHakukohteet(hakuOid).get(60, TimeUnit.SECONDS);
      LOG.info("Importoidaan hakukohteita yhteensä {} kpl", hakukohteet.size());
      return hakukohteet;
    } catch (Exception e) {
      throw new RuntimeException(String.format("Haun %s haku epäonnistui", hakuOid), e);
    }
  }
}
