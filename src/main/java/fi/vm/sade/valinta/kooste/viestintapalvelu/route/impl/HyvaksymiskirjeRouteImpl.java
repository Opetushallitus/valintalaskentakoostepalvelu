package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeRoute;

@Component
public class HyvaksymiskirjeRouteImpl extends SpringRouteBuilder {

    @Autowired
    private ViestintapalveluResource viestintapalveluResource;

    @Autowired
    private HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;

    @Override
    public void configure() throws Exception {
        from(hyvaksymiskirjeet())
        // TODO: Hae osoitteet erikseen
        // TODO: Cache ulkopuolisiin palvelukutsuihin
                .bean(hyvaksymiskirjeetKomponentti)
                //
                .bean(viestintapalveluResource, "haeHyvaksymiskirjeet");
    }

    private String hyvaksymiskirjeet() {
        return HyvaksymiskirjeRoute.DIRECT_HYVAKSYMISKIRJEET;
    }
}
