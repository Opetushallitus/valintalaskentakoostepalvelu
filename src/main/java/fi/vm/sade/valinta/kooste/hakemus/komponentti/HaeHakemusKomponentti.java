package fi.vm.sade.valinta.kooste.hakemus.komponentti;

import org.apache.camel.language.Simple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

/**
 * User: wuoti Date: 9.9.2013 Time: 13.14
 */
@Component("haeHakemusKomponentti")
public class HaeHakemusKomponentti {

    @Autowired
    private ApplicationResource applicationResource;

    public Hakemus haeHakemus(@Simple("${property.hakemusOid}") String hakemusOid) {
        assert (SecurityContextHolder.getContext().getAuthentication() != null); // <-
                                                                                 // helps
                                                                                 // finding
                                                                                 // really
                                                                                 // difficult
                                                                                 // multithread
                                                                                 // bugs
        return applicationResource.getApplicationByOid(hakemusOid);
    }
}
