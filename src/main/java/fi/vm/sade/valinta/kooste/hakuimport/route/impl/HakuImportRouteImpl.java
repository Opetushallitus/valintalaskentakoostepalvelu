package fi.vm.sade.valinta.kooste.hakuimport.route.impl;

import java.util.ArrayList;

import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdeImportTyyppi;
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

    @Autowired
    private ValintaperusteService valintaperusteService;

    @Override
    public void configure() throws Exception {
        from(hakuImport())
        // .policy(admin)
                .bean(suoritaHakuImportKomponentti)
                //
                .split(body(), createAccumulatingAggregation()) // , new
                                                                // FlexibleAggregationStrategy<String>())//
                // .setProperty(OPH.HAKUKOHDEOID,
                // body())
                .bean(suoritaHakukohdeImportKomponentti).end()
                // valinnoille
                .split(body())
                //
                .bean(valintaperusteService, "tuoHakukohde"); // <- metodin nimi
                                                              // ei tarpeen
                                                              // mutta auttaa
                                                              // reitin
                                                              // lukemisessa
    }

    private FlexibleAggregationStrategy<HakukohdeImportTyyppi> createAccumulatingAggregation() {
        return new FlexibleAggregationStrategy<HakukohdeImportTyyppi>().storeInBody().accumulateInCollection(
                ArrayList.class);
    }

    private String hakuImport() {
        return HakuImportRoute.DIRECT_HAKU_IMPORT;
    }
}
