package fi.vm.sade.valinta.kooste.kela.komponentti;

import java.util.Date;

import org.apache.camel.Body;
import org.apache.camel.Property;

import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Rajapinta komponentille yksikkotestausta varten.
 */
public interface KelaHakijaRiviKomponentti {

    TKUVAYHVA luo(@Body HakijaDTO hakija, @Property("lukuvuosi") Date lukuvuosi,
            @Property("poimintapaivamaara") Date poimintapaivamaara) throws Exception;
}
