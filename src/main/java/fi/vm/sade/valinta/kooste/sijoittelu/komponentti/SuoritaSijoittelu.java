package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import fi.vm.sade.service.valintatiedot.schema.HakuTyyppi;
import fi.vm.sade.valinta.kooste.sijoittelu.proxy.SijoitteluSuoritaProxy;
import fi.vm.sade.valinta.kooste.valintatieto.route.ValintatietoRoute;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component("suoritaSijoittelu")
public class SuoritaSijoittelu {

    private static final Logger LOG = LoggerFactory.getLogger(SuoritaSijoittelu.class);

    @Autowired
    private ValintatietoRoute valintatietoProxy;

    @Autowired
    private SijoitteluSuoritaProxy sijoitteluProxy;

    public void haeLahtotiedot(@Simple("${property.hakuOid}") String hakuOid) {

        LOG.info("KOOSTEPALVELU: Haetaan valintatiedot haulle {}", new Object[] { hakuOid });
        HakuTyyppi ht = valintatietoProxy.haeValintatiedot(hakuOid);
        LOG.info("Haettu valinnan tulokset");
        sijoitteluProxy.suorita(ht);
        LOG.info("Viety sijoittelulle valinnan tulokset");

    }
}
