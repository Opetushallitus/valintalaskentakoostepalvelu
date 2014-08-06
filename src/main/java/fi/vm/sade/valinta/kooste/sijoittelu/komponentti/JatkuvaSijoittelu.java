package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;


import fi.vm.sade.valinta.kooste.sijoittelu.Sijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.resource.SijoitteluResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Component("jatkuvaSijoittelu")
public class JatkuvaSijoittelu {
    public static Map<String, Sijoittelu> SIJOITTELU_HAUT = new HashMap<String, Sijoittelu>();

    private static final Logger LOG = LoggerFactory.getLogger(JatkuvaSijoittelu.class);

    @Autowired
    private SijoitteluResource sijoitteluResource;

    public void suorita() {
        LOG.debug("JATKUVA SIJOITTELU KÄYNNISTETTY");
        for (Sijoittelu sijoittelu : SIJOITTELU_HAUT.values()) {
            if(sijoittelu.isAjossa()) {
                LOG.debug("JATKUVA SIJOITTELU: {}", sijoittelu.getHakuOid());
                try {
//                    HakuTyyppi ht = valintatietoService.haeValintatiedot(sijoittelu.getHakuOid());
//                    LOG.info("Haettu valinnan tulokset");
//                    sijoitteluService.sijoittele(ht);
                    sijoitteluResource.sijoittele(sijoittelu.getHakuOid());
                    LOG.info("Viety sijoittelulle valinnan tulokset");
                } catch(Exception e) {
                    LOG.error("JATKUVA SIJOITTELU", e);
                    sijoittelu.setVirhe(e.getMessage());
                }
                sijoittelu.setViimeksiAjettu(new Date());
            }
        }
        LOG.debug("JATKUVA SIJOITTELU LOPETETTU");
    }
}


