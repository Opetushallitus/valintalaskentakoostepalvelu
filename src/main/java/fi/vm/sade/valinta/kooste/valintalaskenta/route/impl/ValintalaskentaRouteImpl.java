package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakemusKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakukohteenHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HakemusOidSplitter;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohteetTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.SplitHakukohteetKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.HaeValintaperusteetKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.SuoritaLaskentaKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HakukohteenValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HaunValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

/**
 * @author Jussi Jartamo
 */
@Component
public class ValintalaskentaRouteImpl extends SpringRouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaRouteImpl.class);

    @Autowired
    private SecurityPreprocessor securityProcessor;

    @Autowired
    private SuoritaLaskentaKomponentti suoritaLaskentaKomponentti;

    @Autowired
    private HaeHakukohteetTarjonnaltaKomponentti haeHakukohteetTarjonnaltaKomponentti;

    @Autowired
    private HaeHakukohteenHakemuksetKomponentti haeHakukohteenHakemuksetKomponentti;

    @Autowired
    private HaeHakemusKomponentti haeHakemusKomponentti;

    @Autowired
    private HaeValintaperusteetKomponentti haeValintaperusteetKomponentti;

    @Override
    public void configure() throws Exception {
        errorHandler(deadLetterChannel(suoritaLaskentaDeadLetterChannel()));
        /**
         * Laskenta dead-letter-channel. Nyt ainoastaan paattaa prosessin.
         * Jatkossa lisaa metadataa paatettyyn prosessiin yllapitajalle.
         */
        from(suoritaLaskentaDeadLetterChannel()).to(fail());

        from("direct:suorita_haehakemus")
                .errorHandler(
                        deadLetterChannel(suoritaLaskentaDeadLetterChannel()).maximumRedeliveries(15)
                                .redeliveryDelay(100L)
                                // log exhausted stacktrace
                                .logExhaustedMessageHistory(true).logExhausted(true)
                                // hide retry/handled stacktrace
                                .logStackTrace(false).logRetryStackTrace(false).logHandled(false))
                //
                .setHeader(OPH.HAKEMUSOID, body())
                //
                .to("log:direct_suorita_haehakemus?level=INFO&showProperties=true").bean(securityProcessor)
                //
                .bean(haeHakemusKomponentti).convertBodyTo(HakemusTyyppi.class);
        /**
         * Alireitti yhden kohteen laskentaan
         */
        from("direct:suorita_valintalaskenta") // jos reitti epaonnistuu parent
                                               // failaa
                //
                .to("log:direct_suorita_valintalaskenta?level=INFO")
                //
                .bean(securityProcessor)
                //
                .bean(haeHakukohteenHakemuksetKomponentti)
                //
                .bean(new HakemusOidSplitter())
                //
                .to("log:direct_suorita_valintalaskenta_pre_split_hakemukset?level=INFO")
                //
                .split(body(),
                        new FlexibleAggregationStrategy<HakemusTyyppi>().storeInHeader("hakemustyypit")
                                .accumulateInCollection(ArrayList.class))
                //
                .parallelProcessing().stopOnException()
                //
                .to("direct:suorita_haehakemus").end()
                //
                .bean(suoritaLaskentaKomponentti);

        from(haunValintalaskenta())
        //
                .process(luoProsessiHaunValintalaskennalle()).to(start())
                //
                .bean(securityProcessor)
                //
                .bean(haeHakukohteetTarjonnaltaKomponentti)
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
                .bean(haeValintaperusteetKomponentti)
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

    private static String suoritaLaskentaDeadLetterChannel() {
        return "direct:suorita_laskenta_deadletterchannel";
    }

    private String hakukohteenValintalaskenta() {
        return HakukohteenValintalaskentaRoute.DIRECT_VALINTALASKENTA_HAKUKOHTEELLE;
    }

    private String haunValintalaskenta() {
        return HaunValintalaskentaRoute.DIRECT_VALINTALASKENTA_HAULLE;
    }
}
