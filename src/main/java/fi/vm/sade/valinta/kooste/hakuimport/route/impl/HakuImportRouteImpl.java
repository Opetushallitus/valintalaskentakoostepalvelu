package fi.vm.sade.valinta.kooste.hakuimport.route.impl;

import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakuImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakukohdeImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakuImportRoute;

@Component
public class HakuImportRouteImpl extends SpringRouteBuilder {

    // @Resource(name = "admin")
    // private org.apache.camel.spi.Policy admin;

    @Autowired
    private SuoritaHakuImportKomponentti suoritaHakuImportKomponentti;

    @Autowired
    private SuoritaHakukohdeImportKomponentti suoritaHakukohdeImportKomponentti;

    @Override
    public void configure() throws Exception {
        from(hakuImport())
        // .policy(admin)
        //
                .bean(suoritaHakuImportKomponentti)
                //
                .split(body()) // , new FlexibleAggregationStrategy<String>())//
                               // .setProperty(OPH.HAKUKOHDEOID,
                // body())
                .bean(suoritaHakukohdeImportKomponentti).end();
    }

    private String hakuImport() {
        return HakuImportRoute.DIRECT_HAKU_IMPORT;
    }
}
