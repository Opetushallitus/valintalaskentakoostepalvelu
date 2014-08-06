package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import fi.vm.sade.valinta.kooste.sijoittelu.Sijoittelu;
import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
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
    private SijoittelunSeurantaResource sijoittelunSeurantaResource;

    public void suorita() {
        LOG.debug("JATKUVA SIJOITTELU KÄYNNISTETTY");
        for (Sijoittelu sijoittelu : SIJOITTELU_HAUT.values()) {
            if(sijoittelu.isAjossa()) {
                LOG.debug("JATKUVA SIJOITTELU: {}", sijoittelu.getHakuOid());
                try {
                    sijoittelunSeurantaResource.merkkaaSijoittelunAjossaTila(sijoittelu.getHakuOid(), true);
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


