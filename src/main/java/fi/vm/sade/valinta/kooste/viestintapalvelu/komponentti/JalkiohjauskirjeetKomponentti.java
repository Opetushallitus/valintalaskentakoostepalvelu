package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.List;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("jalkiohjauskirjeetKomponentti")
public class JalkiohjauskirjeetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(JalkiohjauskirjeetKomponentti.class);

    public String teeJalkiohjauskirjeet(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
            @Simple("${property.hakuOid}") String hakuOid, @Simple("${property.sijoitteluajoId}") Long sijoitteluajoId,
            @Simple("${property.hakemukset}") List<HakemusTyyppi> hakemukset) {
        LOG.debug("Jalkiohjauskirjeet for hakukohde '{}' and haku '{}'", new Object[] { hakukohdeOid, hakuOid });

        return null;// jalkiohjauskirjeet;
    }
}
