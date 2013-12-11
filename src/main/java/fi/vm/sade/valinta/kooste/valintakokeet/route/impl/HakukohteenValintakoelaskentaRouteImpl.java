package fi.vm.sade.valinta.kooste.valintakokeet.route.impl;

import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakukohteenHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HakemusOidSplitter;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.LaskeValintakoeosallistumisetHakemukselleKomponentti;
import fi.vm.sade.valinta.kooste.valintakokeet.route.HakukohteenValintakoelaskentaRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class HakukohteenValintakoelaskentaRouteImpl extends SpringRouteBuilder {

    @Autowired
    private HaeHakukohteenHakemuksetKomponentti haeHakukohteenHakemuksetKomponentti;

    @Autowired
    private LaskeValintakoeosallistumisetHakemukselleKomponentti laskeValintakoeosallistumisetHakemukselleKomponentti;

    @Override
    public void configure() throws Exception {
        from(hakukohteenValintakoelaskenta())
        //
                .bean(haeHakukohteenHakemuksetKomponentti)
                //
                .bean(new HakemusOidSplitter())
                //
                .split(body())
                //
                .setProperty(OPH.HAKEMUSOID, body())
                //
                .bean(laskeValintakoeosallistumisetHakemukselleKomponentti)
                //
                .end();
    }

    private String hakukohteenValintakoelaskenta() {
        return HakukohteenValintakoelaskentaRoute.DIRECT_HAKUKOHTEEN_VALINTAKOELASKENTA;
    }
}
