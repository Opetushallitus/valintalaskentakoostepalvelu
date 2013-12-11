package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.JalkiohjauskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.JalkiohjauskirjeRoute;

@Component
public class JalkiohjauskirjeRouteImpl extends SpringRouteBuilder {

    @Autowired
    private ViestintapalveluResource viestintapalveluResource;

    @Autowired
    private JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti;

    @Override
    public void configure() throws Exception {
        from(jalkiohjauskirjeet())
        //
                .setProperty("kielikoodi", constant("kieli_fi"))
                // TODO: Hae osoitteet erikseen
                // TODO: Cache ulkopuolisiin palvelukutsuihin
                .bean(jalkiohjauskirjeetKomponentti)
                //
                .bean(viestintapalveluResource, "haeJalkiohjauskirjeet");
    }

    private String jalkiohjauskirjeet() {
        return JalkiohjauskirjeRoute.DIRECT_JALKIOHJAUSKIRJEET;
    }
}
