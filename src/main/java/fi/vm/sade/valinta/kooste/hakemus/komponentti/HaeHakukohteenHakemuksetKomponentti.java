package fi.vm.sade.valinta.kooste.hakemus.komponentti;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.exception.HakemuspalveluException;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.SuppeaHakemus;
import java.util.List;
import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("haeHakukohteenHakemuksetKomponentti")
public class HaeHakukohteenHakemuksetKomponentti {
  private static final Logger LOG =
      LoggerFactory.getLogger(HaeHakukohteenHakemuksetKomponentti.class);

  private ApplicationResource applicationResource;
  private String applicationResourceUrl;

  @Autowired
  public HaeHakukohteenHakemuksetKomponentti(
      ApplicationResource applicationResource,
      @Value("${valintalaskentakoostepalvelu.hakemus.rest.url:''}") String applicationResourceUrl) {
    this.applicationResourceUrl = applicationResourceUrl;
    this.applicationResource = applicationResource;
  }

  public List<SuppeaHakemus> haeHakukohteenHakemukset(
      @Property(OPH.HAKUKOHDEOID) String hakukohdeOid) {
    LOG.info(
        "Haetaan HakemusList osoitteesta {}/applications?aoOid={}&start=0&rows={}",
        new Object[] {applicationResourceUrl, hakukohdeOid, Integer.MAX_VALUE});
    HakemusList hakemusList =
        applicationResource.findApplications(
            null,
            ApplicationResource.ACTIVE_AND_INCOMPLETE,
            null,
            null,
            null,
            hakukohdeOid,
            0,
            ApplicationResource.MAX);
    if (hakemusList == null || hakemusList.getResults() == null) { // ||
      // hakemusList.getResults().isEmpty()
      throw new HakemuspalveluException(
          "Hakemuspalvelu ei palauttanut hakemuksia hakukohteelle " + hakukohdeOid);
    }
    LOG.info("Haettiin {} kpl hakemuksia", hakemusList.getResults().size());
    return hakemusList.getResults();
  }
}
