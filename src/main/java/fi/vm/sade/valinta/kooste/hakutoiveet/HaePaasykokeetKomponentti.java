package fi.vm.sade.valinta.kooste.hakutoiveet;

import java.util.List;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.PaasykoeHakukohdeTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("haePaasykokeetKomponentti")
public class HaePaasykokeetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaePaasykokeetKomponentti.class);

    @Autowired
    private ValintaperusteService valintaperusteService;

    public List<PaasykoeHakukohdeTyyppi> haeHakutoiveet(@Simple("${property.hakukohdeOid}") String hakukohdeOid) {
        LOG.info("Haetaan hakutoiveita hakutoiveoid:lla {}", hakukohdeOid);
        return valintaperusteService.haePaasykokeet(hakukohdeOid);
    }
}
