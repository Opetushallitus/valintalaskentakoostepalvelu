package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component("splitHakukohteetKomponentti")
public class SplitHakukohteetKomponentti {

  public List<String> splitHakukohteet(Collection<HakukohdeTyyppi> hakukohteet) {
    List<String> oids = new ArrayList<String>();

    for (HakukohdeTyyppi hk : hakukohteet) {
      oids.add(hk.getOid());
    }
    return oids;
  }
}