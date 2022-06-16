package fi.vm.sade.valinta.kooste.hakemus.komponentti;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.SuppeaHakemus;
import java.util.Arrays;
import java.util.List;
import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("haeHaunHakemuksetKomponentti")
public class HaeHaunHakemuksetKomponentti {
  private static final Logger LOG = LoggerFactory.getLogger(HaeHaunHakemuksetKomponentti.class);

  public static final String ACTIVE = "ACTIVE";
  public static final String INCOMPLETE = "INCOMPLETE";

  @Autowired
  private ApplicationResource applicationResource;

  public List<SuppeaHakemus> haeHaunHakemukset(@Property(OPH.HAKUOID) String hakuOid) {
    LOG.info("Haetaan HakemusList osoitteesta .../applications?asId={}&start=0&rows={}",
        new Object[] { hakuOid, Integer.MAX_VALUE });
    HakemusList hakemusList = applicationResource.findApplications(null, Arrays.asList(ACTIVE, INCOMPLETE), null,
        null, hakuOid, null, 0, ApplicationResource.MAX);

    LOG.info("Haettiin {} kpl hakemuksia", hakemusList.getResults().size());
    return hakemusList.getResults();
  }
}
