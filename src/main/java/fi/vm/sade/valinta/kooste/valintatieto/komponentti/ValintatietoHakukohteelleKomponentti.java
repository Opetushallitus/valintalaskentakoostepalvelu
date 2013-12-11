package fi.vm.sade.valinta.kooste.valintatieto.komponentti;

import java.util.List;

import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;

@Component("valintatietoHakukohteelleKomponentti")
public class ValintatietoHakukohteelleKomponentti {

    private ValintatietoService valintatietoService;

    @Autowired
    public ValintatietoHakukohteelleKomponentti(
            @Qualifier("valintatietoServiceAsAdmin") ValintatietoService valintatietoService) {
        this.valintatietoService = valintatietoService;
    }

    public List<HakemusOsallistuminenTyyppi> valintatiedotHakukohteelle(
            @Property("valintakoeOid") List<String> valintakoeOids, @Property("hakukohdeOid") String hakukohdeOid) {
        return valintatietoService.haeValintatiedotHakukohteelle(valintakoeOids, hakukohdeOid);
    }
}
