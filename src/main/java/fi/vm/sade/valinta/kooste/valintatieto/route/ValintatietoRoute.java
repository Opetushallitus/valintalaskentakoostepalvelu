package fi.vm.sade.valinta.kooste.valintatieto.route;

import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakuDTO;
import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

public interface ValintatietoRoute {
    HakuDTO haeValintatiedot(@Property(OPH.HAKUOID) String hakuOid);
}
