package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import java.util.Collection;

import org.apache.camel.Property;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.valinta.kooste.OPH;

public interface SijoitteluKaikkiPaikanVastaanottaneet {

    Collection<HakijaDTO> vastaanottaneet(@Property(OPH.HAKUOID) String hakuOid);
}
