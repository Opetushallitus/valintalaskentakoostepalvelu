package fi.vm.sade.valinta.kooste.hakemus.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * User: wuoti Date: 29.8.2013 Time: 13.50
 */
@Component("haeHaunHakemuksetKomponentti")
public class HaeHaunHakemuksetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeHaunHakemuksetKomponentti.class);

    public static final String ACTIVE = "ACTIVE";
    public static final String INCOMPLETE = "INCOMPLETE";

    @Autowired
    private ApplicationResource applicationResource;
    @Value("${valintalaskentakoostepalvelu.hakemus.rest.url}")
    private String applicationResourceUrl;

    public List<SuppeaHakemus> haeHaunHakemukset(@Simple("${property.hakuOid}") String hakuOid) {
        assert (SecurityContextHolder.getContext().getAuthentication() != null);
        LOG.info("Haetaan HakemusList osoitteesta {}/applications?asId={}&start=0&rows={}", new Object[]{
                applicationResourceUrl, hakuOid, Integer.MAX_VALUE});
        HakemusList hakemusList = applicationResource.findApplications(null, Arrays.asList(ACTIVE, INCOMPLETE), null, null, hakuOid, null, 0,
                Integer.MAX_VALUE);

        LOG.info("Haettiin {} kpl hakemuksia", hakemusList.getResults().size());

        return hakemusList.getResults();
    }
}
