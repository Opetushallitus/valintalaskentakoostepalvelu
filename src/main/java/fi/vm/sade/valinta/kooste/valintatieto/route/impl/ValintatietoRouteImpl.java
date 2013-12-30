package fi.vm.sade.valinta.kooste.valintatieto.route.impl;

import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.valintatieto.komponentti.ValintatietoHakukohteelleKomponentti;
import fi.vm.sade.valinta.kooste.valintatieto.komponentti.ValintatietoKomponentti;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class ValintatietoRouteImpl extends SpringRouteBuilder {

    private ValintatietoHakukohteelleKomponentti valintatietoHakukohteelleKomponentti;
    private ValintatietoKomponentti valintatietokomponentti;

    @Autowired
    public ValintatietoRouteImpl(ValintatietoHakukohteelleKomponentti valintatietoHakukohteelleKomponentti,
            ValintatietoKomponentti valintatietokomponentti) {
        this.valintatietoHakukohteelleKomponentti = valintatietoHakukohteelleKomponentti;
        this.valintatietokomponentti = valintatietokomponentti;
    }

    @Override
    public void configure() throws Exception {
        from("direct:valintatietoHakukohteelleReitti").bean(valintatietoHakukohteelleKomponentti);

        from("direct:valintatietoReitti").bean(valintatietokomponentti);
    }
}
