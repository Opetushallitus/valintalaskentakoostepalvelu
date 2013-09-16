package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;


import fi.vm.sade.valinta.kooste.sijoittelu.Sijoittelu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 *
 */
@Component("jatkuvaSijoittelu")
public class JatkuvaSijoittelu {
    public static Map<String, Sijoittelu> SIJOITTELU_HAUT = new HashMap<String, Sijoittelu>();

    private static final Logger LOG = LoggerFactory.getLogger(JatkuvaSijoittelu.class);

    @Autowired
    private SuoritaSijoittelu suoritaSijoittelu;

    public void suorita() {
        LOG.debug("JATKUVA SIJOITTELU KÄYNNISTETTY");
        for (Sijoittelu sijoittelu : SIJOITTELU_HAUT.values()) {
            LOG.debug("JATKUVA SIJOITTELU: " + sijoittelu.getHakuOid());
            try {
                suoritaSijoittelu.haeLahtotiedot(sijoittelu.getHakuOid());
            } catch(Exception e) {
                LOG.error("JATKUVA SIJOITTELU", e);
                sijoittelu.setLastError(e.getMessage());
            }
            sijoittelu.setViimeksiAjettu(new Date());
        }
        LOG.debug("JATKUVA SIJOITTELU LOPETETTU");
    }
}


