package fi.vm.sade.valinta.kooste.paasykokeet;

import java.util.Arrays;
import java.util.List;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.HakemusService;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("haeHakutoiveetKomponentti")
public class HaeHakutoiveetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeHakutoiveetKomponentti.class);

    @Autowired
    private HakemusService hakemusService;

    public List<HakemusTyyppi> haeHakutoiveet(@Simple("${property.hakukohdeOid}") String hakukohdeOid) {
        LOG.info("Haetaan hakutoiveita hakutoiveoid:lla {}", hakukohdeOid);
        return hakemusService.haeHakemukset(Arrays.asList(hakukohdeOid));// haeHakutoiveet(hakuOid);
    }
}
