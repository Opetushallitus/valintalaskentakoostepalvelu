package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.OPH;

/**
 *
 */
@Component("suoritaSijoittelu")
public class SuoritaSijoittelu {

    private static final Logger LOG = LoggerFactory.getLogger(SuoritaSijoittelu.class);

    @Autowired
    private SijoitteluSuoritaKomponentti sijoitteluProxy;

    public void sijoittele(@Property(OPH.HAKUOID) String hakuOid) {

        LOG.info("Haettu valinnan tulokset");
        sijoitteluProxy.sijottele(hakuOid);
        LOG.info("Viety sijoittelulle valinnan tulokset");

    }
}
