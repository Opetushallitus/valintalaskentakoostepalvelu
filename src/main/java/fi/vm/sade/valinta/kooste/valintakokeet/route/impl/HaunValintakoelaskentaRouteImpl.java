package fi.vm.sade.valinta.kooste.valintakokeet.route.impl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHaunHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HakemusOidSplitter;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.LaskeValintakoeosallistumisetHakemukselleKomponentti;
import fi.vm.sade.valinta.kooste.valintakokeet.route.HaunValintakoelaskentaRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class HaunValintakoelaskentaRouteImpl extends SpringRouteBuilder {

    // private static final String ENSIMMAINEN_VIRHE =
    // "ensimmainen_virhe_reitilla";

    @Autowired
    private HaeHaunHakemuksetKomponentti haeHaunHakemuksetKomponentti;

    @Autowired
    private LaskeValintakoeosallistumisetHakemukselleKomponentti laskeValintakoeosallistumisetHakemukselleKomponentti;

    @Override
    public void configure() throws Exception {
        from(yksittainenValintakoelaskentaDeadLetterChannel())
                //
                .setHeader(
                        "message",
                        simple("[${property.authentication.name}] Valintakoelaskenta ei toimi. Hakemus ${property.hakemusOid}, haku ${property.hakuOid}"))
                .to(fail());

        from(haunValintakoelaskentaDeadLetterChannel())
                //
                .setHeader(
                        "message",
                        simple("[${property.authentication.name}] HakuApp ei toimi. Hakemukset haulle ${property.hakuOid}"))
                .to(fail());

        /**
         * Koko haun valintakoelaskenta alkaa tasta!
         */
        from(haunValintakoelaskenta())
        //
                .errorHandler(deadLetterChannel(haunValintakoelaskentaDeadLetterChannel()))
                //
                .bean(new SecurityPreprocessor())
                //
                // .setProperty(ENSIMMAINEN_VIRHE, constant(new
                // AtomicBoolean(true)))
                //
                .process(luoProsessiHaunValintakoelaskennalle())
                //
                .to(start())
                //
                .bean(haeHaunHakemuksetKomponentti)
                //
                .bean(new HakemusOidSplitter())
                //
                .split(body())
                //
                .shareUnitOfWork()
                //
                .parallelProcessing()
                //
                .stopOnException()
                //
                .to(yksittainenValintakoelaskenta())
                //
                .end()
                //
                .to(finish());

        /**
         * Yksittaisen hakemuksen valintakoelaskenta!
         */
        from(yksittainenValintakoelaskenta())
                .errorHandler(
                        deadLetterChannel(yksittainenValintakoelaskentaDeadLetterChannel()).maximumRedeliveries(10)
                                .redeliveryDelay(300L)
                                // log exhausted stacktrace
                                .logExhaustedMessageHistory(true).logExhausted(true)
                                // hide retry/handled stacktrace
                                .logStackTrace(false).logRetryStackTrace(false).logHandled(false))
                //
                .bean(new SecurityPreprocessor())
                //
                .setProperty(OPH.HAKEMUSOID, body())
                //
                .bean(laskeValintakoeosallistumisetHakemukselleKomponentti);

    }

    private Processor luoProsessiHaunValintakoelaskennalle() {
        return new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.setProperty(kuvaus(), "Valintakoelaskenta haulle");
                exchange.setProperty(prosessi(),
                        new Prosessi("Valintakoelaskenta", "Haulle", exchange.getProperty(OPH.HAKUOID, String.class)));
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
        return "bean:haunValintakoelaskentaValvomo?method=fail";
    }

    private static String start() {
        return "bean:haunValintakoelaskentaValvomo?method=start";
    }

    private static String finish() {
        return "bean:haunValintakoelaskentaValvomo?method=finish";
    }

    private String haunValintakoelaskenta() {
        return HaunValintakoelaskentaRoute.DIRECT_HAUN_VALINTAKOELASKENTA;
    }

    private static String yksittainenValintakoelaskenta() {
        return "direct:kaynnistaHaunValintakoelaskentaReitti_yksittainen";
    }

    private static String yksittainenValintakoelaskentaDeadLetterChannel() {
        return "direct:kaynnistaHaunValintakoelaskentaReitti_yksittainen_deadletterchannel";
    }

    private static String haunValintakoelaskentaDeadLetterChannel() {
        return "direct:kaynnistaHaunValintakoelaskentaReitti_deadletterchannel";
    }
}
