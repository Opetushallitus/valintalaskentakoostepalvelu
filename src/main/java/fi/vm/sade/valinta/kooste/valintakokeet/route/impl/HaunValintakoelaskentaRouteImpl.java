package fi.vm.sade.valinta.kooste.valintakokeet.route.impl;

import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHaunHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HakemusOidSplitter;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.LaskeValintakoeosallistumisetHakemukselleKomponentti;
import fi.vm.sade.valinta.kooste.valintakokeet.route.HaunValintakoelaskentaRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class HaunValintakoelaskentaRouteImpl extends SpringRouteBuilder {

    @Autowired
    private HaeHaunHakemuksetKomponentti haeHaunHakemuksetKomponentti;

    @Autowired
    private LaskeValintakoeosallistumisetHakemukselleKomponentti laskeValintakoeosallistumisetHakemukselleKomponentti;

    @Override
    public void configure() throws Exception {
        from(haunValintakoelaskenta())
        //
                .bean(haeHaunHakemuksetKomponentti)
                //
                .bean(new HakemusOidSplitter())
                //
                .split(body())
                //
                .setProperty(OPH.HAKEMUSOID, body())
                //
                .bean(laskeValintakoeosallistumisetHakemukselleKomponentti);
    }

    private String haunValintakoelaskenta() {
        return HaunValintakoelaskentaRoute.DIRECT_HAUN_VALINTAKOELASKENTA;
    }
}
