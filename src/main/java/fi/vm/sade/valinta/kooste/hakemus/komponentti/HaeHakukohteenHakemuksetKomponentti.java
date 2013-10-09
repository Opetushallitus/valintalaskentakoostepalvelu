package fi.vm.sade.valinta.kooste.hakemus.komponentti;

import fi.vm.sade.valinta.kooste.exception.HakemuspalveluException;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * User: wuoti Date: 9.9.2013 Time: 9.05
 */
@Component("haeHakukohteenHakemuksetKomponentti")
public class HaeHakukohteenHakemuksetKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(HaeHakukohteenHakemuksetKomponentti.class);

    public static final String ACTIVE = "ACTIVE";
    public static final String INCOMPLETE = "INCOMPLETE";

    @Autowired
    private ApplicationResource applicationResource;
    @Value("${valintalaskentakoostepalvelu.hakemus.rest.url}")
    private String applicationResourceUrl;

    public List<SuppeaHakemus> haeHakukohteenHakemukset(@Property("hakukohdeOid") String hakukohdeOid) {
        assert (SecurityContextHolder.getContext().getAuthentication() != null);
        LOG.info("Haetaan HakemusList osoitteesta {}/applications?aoOid={}&start=0&rows={}", new Object[]{
                applicationResourceUrl, hakukohdeOid, Integer.MAX_VALUE});
        HakemusList hakemusList = applicationResource.findApplications(null, Arrays.asList(ACTIVE, INCOMPLETE), null, null, null, hakukohdeOid, 0,
                Integer.MAX_VALUE);
        if (hakemusList == null || hakemusList.getResults() == null) {
            throw new HakemuspalveluException("Hakemuspalvelu ei palauttanut hakemuksia hakukohteelle " + hakukohdeOid);
        }
        LOG.info("Haettiin {} kpl hakemuksia", hakemusList.getResults().size());
        return hakemusList.getResults();
    }
}
