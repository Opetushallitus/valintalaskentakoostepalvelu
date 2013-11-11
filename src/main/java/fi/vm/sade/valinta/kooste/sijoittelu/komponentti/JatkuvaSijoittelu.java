package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;


import fi.vm.sade.service.sijoittelu.SijoitteluService;
import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.service.valintatiedot.schema.HakuTyyppi;
import fi.vm.sade.valinta.kooste.sijoittelu.Sijoittelu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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

    @Resource(name="valintatietoServiceAsAdmin")
    private ValintatietoService valintatietoService;

    @Resource(name="sijoitteluServiceAsAdmin")
    private SijoitteluService sijoitteluService;

    public void suorita() {
        LOG.debug("JATKUVA SIJOITTELU KÄYNNISTETTY");
        for (Sijoittelu sijoittelu : SIJOITTELU_HAUT.values()) {
            if(sijoittelu.isAjossa()) {
                LOG.debug("JATKUVA SIJOITTELU: {}", sijoittelu.getHakuOid());
                try {
                    HakuTyyppi ht = valintatietoService.haeValintatiedot(sijoittelu.getHakuOid());
                    LOG.info("Haettu valinnan tulokset");
                    sijoitteluService.sijoittele(ht);
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


