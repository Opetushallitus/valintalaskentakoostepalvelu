package fi.vm.sade.valinta.kooste.valintakokeet.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * User: wuoti
 * Date: 29.8.2013
 * Time: 13.50
 */
@Component("haeHaunHakemuksetKomponentti")
public class HaeHaunHakemuksetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeHaunHakemuksetKomponentti.class);

    @Autowired
    private ApplicationResource applicationResource;

    public List<String> haeHaunHakemukset(@Simple("${property.hakuOid}") String hakuOid) {
        LOG.info("Haetaan haun " + hakuOid + " hakemukset");

        HakemusList hakemusList = applicationResource.findApplications(null, null, null, null, hakuOid, 0,
                Integer.MAX_VALUE);

        List<String> hakemusOids = new ArrayList<String>();
        for (SuppeaHakemus hakemus : hakemusList.getResults()) {
            hakemusOids.add(hakemus.getOid());
        }

        return hakemusOids;
    }
}
