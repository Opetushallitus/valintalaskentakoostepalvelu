package fi.vm.sade.valinta.kooste.valintatieto.route;

import java.util.List;

import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import org.apache.camel.Property;

public interface ValintatietoHakukohteelleRoute {
    List<HakemusOsallistuminenDTO> haeValintatiedotHakukohteelle(@Property("valintakoeOid") List<String> valintakoeOids, @Property("hakukohdeOid") String hakukohdeOid);
}
