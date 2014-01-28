package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.KoekutsukirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class KoekutsukirjeRouteImpl extends SpringRouteBuilder {

    private final ViestintapalveluResource viestintapalveluResource;
    private final KoekutsukirjeetKomponentti koekutsukirjeetKomponentti;

    @Autowired
    public KoekutsukirjeRouteImpl(ViestintapalveluResource viestintapalveluResource,
            KoekutsukirjeetKomponentti koekutsukirjeetKomponentti) {
        this.viestintapalveluResource = viestintapalveluResource;
        this.koekutsukirjeetKomponentti = koekutsukirjeetKomponentti;
    }

    @Override
    public void configure() throws Exception {
        //
        from(koekutsukirjeet())
        //
                .bean(koekutsukirjeetKomponentti)
                //
                .bean(viestintapalveluResource, "haeKoekutsukirjeet");
    }

    private String koekutsukirjeet() {
        return KoekutsukirjeRoute.DIRECT_KOEKUTSUKIRJEET;
    }
}
