package fi.vm.sade.valinta.kooste.hakuimport.route.impl;

import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.kuvaus;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.prosessi;
import static fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Property;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(HakuImportRouteImpl.class);

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
        /**
         * Tanne tullaan jos retry:t ei riita importoinnin loppuun vientiin
         */
        from("direct:tuoHakukohdeDead").process(new Processor() {
            public void process(Exchange exchange) throws Exception {
                HakuImportProsessi prosessi = exchange.getProperty(PROPERTY_VALVOMO_PROSESSI, HakuImportProsessi.class);
                int v = prosessi.lisaaVirhe();
                LOG.error("Virhe (numero {}) hakukohteiden importoinnissa! {}", exchange.getException().getMessage());
            }
        });

        /**
         * Erillinen reitti viennille(tuonnille). Reitilla oma errorhandler.
         */
        from("direct:tuoHakukohdeWithRetry")
        //
                .errorHandler(
                        deadLetterChannel("direct:tuoHakukohdeDead").maximumRedeliveries(15).redeliveryDelay(100L)
                                //
                                .logExhaustedMessageHistory(true).logStackTrace(false).logExhausted(true)
                                .logRetryStackTrace(false).logHandled(false))
                //
                .bean(valintaperusteService, "tuoHakukohde").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        HakuImportProsessi prosessi = exchange.getProperty(PROPERTY_VALVOMO_PROSESSI,
                                HakuImportProsessi.class);
                        int t = prosessi.lisaaTuonti();

                        LOG.info("Kaikki on tuotu onnistuneesti ({}/{}).",
                                new Object[] { t, prosessi.getHakukohteita() });
                    }
                });

        from(hakuImport())
                // .policy(admin)
                .setProperty(kuvaus(), constant("Haun importointi"))
                .setProperty(prosessi(), method(new PrepareHakuImportProcessDescription()))
                //
                .to(start())
                //
                .bean(suoritaHakuImportKomponentti)
                //
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        HakuImportProsessi prosessi = exchange.getProperty(PROPERTY_VALVOMO_PROSESSI,
                                HakuImportProsessi.class);
                        @SuppressWarnings("unchecked")
                        Collection<String> hakukohdeOids = (Collection<String>) exchange.getIn().getBody(
                                Collection.class);
                        prosessi.setHakukohteita(hakukohdeOids.size());
                        LOG.info("Hakukohteita importoitavana {}", hakukohdeOids.size());
                    }
                })
                //
                .split(body(), createAccumulatingAggregation()).parallelProcessing()
                //
                .bean(suoritaHakukohdeImportKomponentti).process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        HakuImportProsessi prosessi = exchange.getProperty(PROPERTY_VALVOMO_PROSESSI,
                                HakuImportProsessi.class);
                        int i = prosessi.lisaaImportoitu();
                        if (i == prosessi.getHakukohteita()) {
                            LOG.info("Kaikki hakukohteet ({}) importoitu!", i);
                        }
                    }
                }).end()
                //
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        LOG.info("Viedään hakukohteita {} kpl valintaperusteet-servicen kantaan", exchange.getIn()
                                .getBody(Collection.class).size());
                    }
                })
                // valinnoille
                .split(body()).parallelProcessing()
                //
                .to("direct:tuoHakukohdeWithRetry")
                // .bean(valintaperusteService, "tuoHakukohde")
                .end()
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
