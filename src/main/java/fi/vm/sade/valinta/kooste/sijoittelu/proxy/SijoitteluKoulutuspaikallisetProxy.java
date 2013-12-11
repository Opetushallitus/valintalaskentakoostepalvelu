package fi.vm.sade.valinta.kooste.sijoittelu.proxy;

import java.util.Collection;

import org.apache.camel.Property;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.valinta.kooste.OPH;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         TODO: Refaktoroi yhdeksi proxyksi sijoittelun operaatiot!
 */
public interface SijoitteluKoulutuspaikallisetProxy {

    Collection<HakijaDTO> koulutuspaikalliset(@Property(OPH.HAKUOID) String hakuOid,
            @Property(OPH.HAKUKOHDEOID) String hakukohdeOid, @Property(OPH.SIJOITTELUAJOID) String sijoitteluajoId);
}
