package fi.vm.sade.valinta.kooste.hakemus.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import org.apache.camel.language.Simple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * User: wuoti
 * Date: 9.9.2013
 * Time: 13.14
 */
@Component("haeHakemusKomponentti")
public class HaeHakemusKomponentti {

    @Autowired
    private ApplicationResource applicationResource;

    public Hakemus haeHakemus(@Simple("${property.hakemusOid}") String hakemusOid) {
        return applicationResource.getApplicationByOid(hakemusOid);
    }
}
