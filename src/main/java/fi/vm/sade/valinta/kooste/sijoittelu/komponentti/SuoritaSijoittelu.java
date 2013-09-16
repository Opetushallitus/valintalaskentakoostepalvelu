package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;


import fi.vm.sade.service.sijoittelu.SijoitteluService;
import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.service.valintatiedot.schema.HakuTyyppi;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 *
 */
@Component("suoritaSijoittelu")
public class SuoritaSijoittelu {

    private static final Logger LOG = LoggerFactory.getLogger(SuoritaSijoittelu.class);

    @Autowired
    private ValintatietoService valintatietoService;

    @Autowired
    private SijoitteluService sijoitteluService;

    public void haeLahtotiedot(@Simple("${property.hakuOid}") String hakuOid) {

        LOG.info("KOOSTEPALVELU: Haetaan valintatiedot haulle {}", new Object[] {hakuOid});
        HakuTyyppi ht = valintatietoService.haeValintatiedot(hakuOid);
        LOG.info("Haettu valinnan tulokset");
        sijoitteluService.sijoittele(ht);
        LOG.info("Viety sijoittelulle valinnan tulokset");

    }
}


