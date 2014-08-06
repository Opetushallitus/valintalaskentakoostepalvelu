package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import fi.vm.sade.valinta.seuranta.sijoittelu.dto.SijoitteluDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 *
 */
@Component("jatkuvaSijoittelu")
public class JatkuvaSijoittelu {

    private static final Logger LOG = LoggerFactory.getLogger(JatkuvaSijoittelu.class);

    @Autowired
    private SijoittelunSeurantaResource sijoittelunSeurantaResource;

    public void suorita() {
        LOG.debug("JATKUVA SIJOITTELU KÄYNNISTETTY");
        for (SijoitteluDto sijoittelu : sijoittelunSeurantaResource.hae())  {
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


