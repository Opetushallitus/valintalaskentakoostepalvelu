package fi.vm.sade.valinta.kooste.hakuimport.route.impl;

import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.kuvaus;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.prosessi;

import java.util.ArrayList;

import org.apache.camel.Property;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdeImportTyyppi;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakuImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakukohdeImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakuImportRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

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

    public static class PrepareHakuImportProcessDescription {

        public Prosessi prepareProcess(@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSIKUVAUS) String kuvaus,
                @Property(OPH.HAKUOID) String hakuOid) {
            return new HakuImportProsessi(kuvaus, hakuOid);
        }
    }

    @Override
    public void configure() throws Exception {
        from(hakuImport())
                // .policy(admin)
                .setProperty(kuvaus(), constant("Haun importointi"))
                .setProperty(prosessi(), method(new PrepareHakuImportProcessDescription()))
                //
                .to(start())
                //
                .bean(suoritaHakuImportKomponentti)
                //
                .split(body(), createAccumulatingAggregation())
                //
                .bean(suoritaHakukohdeImportKomponentti).end()
                // valinnoille
                .split(body())
                //
                .bean(valintaperusteService, "tuoHakukohde").end()
                //
                .to(finish());
    }

    private FlexibleAggregationStrategy<HakukohdeImportTyyppi> createAccumulatingAggregation() {
        return new FlexibleAggregationStrategy<HakukohdeImportTyyppi>().storeInBody().accumulateInCollection(
                ArrayList.class);
    }

    private String hakuImport() {
        return HakuImportRoute.DIRECT_HAKU_IMPORT;
    }

    public static String fail() {
        return "bean:hakuImportValvomo?method=fail(*,*)";
    }

    public static String start() {
        return "bean:hakuImportValvomo?method=start(*)";
    }

    public static String finish() {
        return "bean:hakuImportValvomo?method=finish(*)";
    }
}
