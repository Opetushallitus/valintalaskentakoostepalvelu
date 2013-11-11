package fi.vm.sade.valinta.kooste.valintatieto.komponentti;

import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("valintatietoHakukohteelleKomponentti")
public class ValintatietoHakukohteelleKomponentti {

    @Autowired
    private ValintatietoService valintatietoService;

    public List<HakemusOsallistuminenTyyppi> valintatiedotHakukohteelle(
            @Property("valintakoeOid") List<String> valintakoeOids, @Property("hakukohdeOid") String hakukohdeOid) {
        return valintatietoService.haeValintatiedotHakukohteelle(valintakoeOids, hakukohdeOid);
    }
}
