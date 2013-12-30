package fi.vm.sade.valinta.kooste.valintatieto.route;

import java.util.List;

import org.apache.camel.Property;

import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;

public interface ValintatietoHakukohteelleRoute {

    List<HakemusOsallistuminenTyyppi> haeValintatiedotHakukohteelle(
            @Property("valintakoeOid") List<String> valintakoeOids, @Property("hakukohdeOid") String hakukohdeOid);
}
