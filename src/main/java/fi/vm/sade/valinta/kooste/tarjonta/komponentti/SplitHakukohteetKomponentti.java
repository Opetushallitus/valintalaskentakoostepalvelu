package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * User: wuoti
 * Date: 20.5.2013
 * Time: 13.11
 */
@Component("splitHakukohteetKomponentti")
public class SplitHakukohteetKomponentti {

    public List<String> splitHakukohteet(List<HakukohdeTyyppi> hakukohteet) {
        List<String> oids = new ArrayList<String>();

        for (HakukohdeTyyppi hk : hakukohteet) {
            oids.add(hk.getOid());
        }
        return oids;
    }
}
