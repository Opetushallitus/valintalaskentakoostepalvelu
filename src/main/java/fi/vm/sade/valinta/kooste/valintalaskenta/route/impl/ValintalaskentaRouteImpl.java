package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.SplitHakukohteetKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HakukohteenValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HaunValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

/**
 * @author Jussi Jartamo
 */
@Component
public class ValintalaskentaRouteImpl extends SpringRouteBuilder {

    @Autowired
    private SecurityPreprocessor securityProcessor;

    @Override
    public void configure() throws Exception {
        from("direct:suorita_laskenta_dead").to(fail());
        /**
         * Alireitti yhden kohteen laskentaan
         */
        from("direct:suorita_valintalaskenta")
        //
                .errorHandler(
                        deadLetterChannel("direct:suorita_laskenta_dead").maximumRedeliveries(15).redeliveryDelay(100L)
                                //
                                .logExhaustedMessageHistory(true).logStackTrace(false).logExhausted(true)
                                .logRetryStackTrace(false).logHandled(false))
                //
                .bean(securityProcessor)
                //
                .to("bean:suoritaLaskentaKomponentti");

        from(haunValintalaskenta())
        //
                .process(luoProsessiHaunValintalaskennalle()).to(start())
                //
                .bean(securityProcessor)
                //
                .to("bean:hakukohteetTarjonnaltaKomponentti")
                // Collection<HakukohdeTyyppi>
                .bean(new SplitHakukohteetKomponentti())
                // Collection<String>
                .split(body()).parallelProcessing()
                //
                .setProperty("hakukohdeOid", body()).to("direct:suorita_valintalaskenta")
                // end splitter
                .end()
                // route done
                .to(finish());

        from(hakukohteenValintalaskenta())
        //
                .process(luoProsessiHakukohteenValintalaskennalle()).to(start())
                //
                .to("bean:haeValintaperusteetKomponentti")
                //
                .setProperty("valintaperusteet", body())
                //
                .to("direct:suorita_valintalaskenta").to(finish());

    }

    private Processor luoProsessiHaunValintalaskennalle() {
        return new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.setProperty(kuvaus(), "Valintalaskenta haulle");
                exchange.setProperty(
                        prosessi(),
                        new ValintalaskentaProsessi("Valintalaskenta", "Haulle", exchange.getProperty(OPH.HAKUOID,
                                String.class), exchange.getProperty(OPH.HAKUKOHDEOID, String.class), exchange
                                .getProperty(OPH.VALINNANVAIHE, Integer.class)));
            }
        };
    }

    private Processor luoProsessiHakukohteenValintalaskennalle() {
        return new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.setProperty(kuvaus(), "Valintalaskenta hakukohteelle");
                exchange.setProperty(
                        prosessi(),
                        new ValintalaskentaProsessi("Valintalaskenta", "Hakukohteelle", exchange.getProperty(
                                OPH.HAKUOID, String.class), exchange.getProperty(OPH.HAKUKOHDEOID, String.class),
                                exchange.getProperty(OPH.VALINNANVAIHE, Integer.class)));
            }
        };
    }

    private static String kuvaus() {
        return ValvomoAdminService.PROPERTY_VALVOMO_PROSESSIKUVAUS;
    }

    private static String prosessi() {
        return ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI;
    }

    private static String fail() {
        return "bean:valintalaskentaValvomo?method=fail(*,*)";
    }

    private static String start() {
        return "bean:valintalaskentaValvomo?method=start(*)";
    }

    private static String finish() {
        return "bean:valintalaskentaValvomo?method=finish(*)";
    }

    private String hakukohteenValintalaskenta() {
        return HakukohteenValintalaskentaRoute.DIRECT_VALINTALASKENTA_HAKUKOHTEELLE;
    }

    private String haunValintalaskenta() {
        return HaunValintalaskentaRoute.DIRECT_VALINTALASKENTA_HAULLE;
    }
}
