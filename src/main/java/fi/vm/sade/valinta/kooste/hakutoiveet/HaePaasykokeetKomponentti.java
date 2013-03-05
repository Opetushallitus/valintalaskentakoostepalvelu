package fi.vm.sade.valinta.kooste.hakutoiveet;

import java.util.Collections;
import java.util.List;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakutoiveTyyppi;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("haeHakutoiveetKomponentti")
public class HaePaasykokeetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaePaasykokeetKomponentti.class);

    @Autowired
    private ValintaperusteService valintaperusteService;

    public List<HakutoiveTyyppi> haeHakutoiveet(@Simple("${property.hakuOid}") String hakuOid) {
        LOG.info("Haetaan hakutoiveita hakutoiveoid:lla {}", hakuOid);
        return Collections.emptyList();
        // return hakemusService.haeHakutoiveet(hakuOid);
    }
}
