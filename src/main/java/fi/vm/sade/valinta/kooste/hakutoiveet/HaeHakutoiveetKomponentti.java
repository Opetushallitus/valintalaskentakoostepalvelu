package fi.vm.sade.valinta.kooste.hakutoiveet;

import java.util.List;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.HakemusService;
import fi.vm.sade.service.hakemus.schema.HakutoiveTyyppi;

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

    public List<HakutoiveTyyppi> haeHakutoiveet(@Simple("${property.hakutoiveetOid}") String hakutoiveetOid) {
        LOG.info("Haetaan hakutoiveita hakutoiveoid:lla {}", hakutoiveetOid);
        return hakemusService.haeHakutoiveet(hakutoiveetOid);
    }
}
