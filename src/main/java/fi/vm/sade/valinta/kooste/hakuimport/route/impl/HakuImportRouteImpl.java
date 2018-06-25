package fi.vm.sade.valinta.kooste.hakuimport.route.impl;

import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.kuvaus;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.prosessi;
import static fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakuImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakukohdeImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakuImportRoute;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.Property;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class HakuImportRouteImpl extends SpringRouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(HakuImportRouteImpl.class);

    private final SuoritaHakuImportKomponentti suoritaHakuImportKomponentti;
    private final SuoritaHakukohdeImportKomponentti tarjontaJaKoodistoHakukohteenHakuKomponentti;
    private final ValintaperusteetAsyncResource valintaperusteetRestResource;
    private final ExecutorService hakuImportThreadPool;
    private final ExecutorService hakukohdeImportThreadPool;

    @Autowired
    public HakuImportRouteImpl(
            @Value("${valintalaskentakoostepalvelu.hakuimport.threadpoolsize:10}") Integer hakuImportThreadpoolSize,
            @Value("${valintalaskentakoostepalvelu.hakukohdeimport.threadpoolsize:10}") Integer hakukohdeImportThreadpoolSize,
            SuoritaHakuImportKomponentti suoritaHakuImportKomponentti,
            ValintaperusteetAsyncResource valintaperusteetRestResource,
            SuoritaHakukohdeImportKomponentti tarjontaJaKoodistoHakukohteenHakuKomponentti) {
        this.suoritaHakuImportKomponentti = suoritaHakuImportKomponentti;
        this.tarjontaJaKoodistoHakukohteenHakuKomponentti = tarjontaJaKoodistoHakukohteenHakuKomponentti;
        this.valintaperusteetRestResource = valintaperusteetRestResource;
        this.hakuImportThreadPool = Executors.newFixedThreadPool(hakuImportThreadpoolSize);
        LOG.info("Using hakuImportThreadPool thread pool size " + hakuImportThreadpoolSize);
        this.hakukohdeImportThreadPool = Executors.newFixedThreadPool(hakukohdeImportThreadpoolSize);
        LOG.info("Using hakukohdeImportThreadPool thread pool size " + hakukohdeImportThreadpoolSize);
    }

    public static class PrepareHakuImportProcessDescription {
        public Prosessi prepareProcess(
                @Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSIKUVAUS) String kuvaus,
                @Property(OPH.HAKUOID) String hakuOid) {
            return new HakuImportProsessi(kuvaus, hakuOid);
        }
    }

    @Override
    public void configure() {
        /**
         * Tanne tullaan jos retry:t ei riita importoinnin loppuun vientiin
         */
        from("direct:tuoHakukohdeDead")
                //
                .log(LoggingLevel.ERROR,
                        "Reason ${exception.message} ${exception.stacktrace}")
                        //
                .setHeader(
                        "message",
                        simple("[${property.authentication.name}] Valintaperusteiden vienti epäonnistui hakukohteelle ${body}"))
                .to(fail())
                        //
                .process(logFailedHakuImport())
                        //
                .stop();
        from("direct:convertHakukohdeDead")
                //
                .log(LoggingLevel.ERROR,
                        "Reason ${exception.message} ${exception.stacktrace}")
                        //
                .setHeader(
                        "message",
                        simple("[${property.authentication.name}] Valintaperusteiden vienti epäonnistui hakukohteelle ${body}"))
                .to(fail())
                        //
                .process(logFailedHakuConvert())
                        //
                .stop();
        //
        from("direct:hakuimport_epaonnistui")

                //
                .log(LoggingLevel.ERROR,
                        "Reason ${exception.message} ${exception.stacktrace}")
                        //
                .setHeader(
                        "message",
                        simple("[${property.authentication.name}] Tarjonnasta ei saatu hakua(${property.hakuOid}) tai haun hakukohteiden prosessointi ei mennyt oikein"))
                .to(fail())
                        //
                        // .process(logFailedHakuConvert())
                        //
                .stop();
        /**
         * Erillinen reitti viennille(tuonnille). Reitilla oma errorhandler.
         */

        from("direct:hakuimport_koostepalvelulta_valinnoille")
                //
                .errorHandler(
                        deadLetterChannel("direct:tuoHakukohdeDead")
                                .maximumRedeliveries(0)
                                        // .redeliveryDelay(200L)
                                        //
                                .logExhaustedMessageHistory(true)
                                .logStackTrace(false).logExhausted(true)
                                .logRetryStackTrace(false).logHandled(false))
                        //
                .process(SecurityPreprocessor.SECURITY)
                        //
                .split(body())
                        //
                .executorService(hakukohdeImportThreadPool)
                    //
                .process(new AsyncProcessor() {
                    @Override
                    public boolean process(Exchange exchange, AsyncCallback callback) {
                        HakukohdeImportDTO hki = exchange.getIn().getBody(
                            HakukohdeImportDTO.class);
                        valintaperusteetRestResource.tuoHakukohde(hki).subscribe(
                            ok -> callback.done(false),
                            error -> {
                                callback.done(false);
                                LOG.error("valintaperusteetRestResource.tuoHakukohde palautti virheen", error);
                            }
                        );
                        return false;
                    }

                    @Override
                    public void process(Exchange exchange) {
                        throw new UnsupportedOperationException("Hakukohteiden käsittelyn pitäisi tapahtua asynkronisesti," +
                            " tänne ei pitäisi koskaan päätyä.");
                    }
                })
                        //
                .process(logSuccessfulHakuImport());

        from("direct:hakuimport_tarjonnasta_koostepalvelulle")
                //
                .errorHandler(
                        deadLetterChannel("direct:convertHakukohdeDead")
                                .maximumRedeliveries(0)
                                .redeliveryDelay(200L)
                                        //
                                .logExhaustedMessageHistory(true)
                                .logStackTrace(false).logExhausted(true)
                                .logRetryStackTrace(false).logHandled(false))
                        //
                .process(SecurityPreprocessor.SECURITY)
                        //
                .bean(tarjontaJaKoodistoHakukohteenHakuKomponentti)
                        //
                .process(logSuccessfulHakukohdeGet())
                        //
                .to("direct:hakuimport_koostepalvelulta_valinnoille");

        from(hakuImport())
                .errorHandler(
                        deadLetterChannel("direct:hakuimport_epaonnistui"))
                        // .policy(admin)
                .process(SecurityPreprocessor.SECURITY)
                        //
                .setProperty(kuvaus(), constant("Haun importointi"))
                .setProperty(prosessi(),
                        method(new PrepareHakuImportProcessDescription()))
                        //
                .to(start())
                        //
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) {
                        exchange.getOut().setBody(
                                suoritaHakuImportKomponentti
                                        .suoritaHakukohdeImport(exchange
                                                .getProperty(OPH.HAKUOID,
                                                        String.class)));
                    }
                })
                        //
                .process(logSuccessfulHakuGet())
                        //
                .split(body())
                        //
                .executorService(hakuImportThreadPool)
                        //
                        // .shareUnitOfWork()
                        //
                .parallelProcessing()
                        //
                        // .stopOnException()
                        //
                .to("direct:hakuimport_tarjonnasta_koostepalvelulle")
                        //
                .end()
                        //
                .to(finish());

    }

    private String hakuImport() {
        return HakuImportRoute.DIRECT_HAKU_IMPORT;
    }

    private static String fail() {
        return "bean:hakuImportValvomo?method=fail(*,*)";
    }

    private static String start() {
        return "bean:hakuImportValvomo?method=start(*)";
    }

    private static String finish() {
        return "bean:hakuImportValvomo?method=finish(*)";
    }

    private Processor logSuccessfulHakukohdeGet() {
        return new Processor() {
            public void process(Exchange exchange) {
                HakuImportProsessi prosessi = exchange.getProperty(
                        PROPERTY_VALVOMO_PROSESSI, HakuImportProsessi.class);
                int i = prosessi.lisaaImportoitu();
                if (i == prosessi.getHakukohteita()) {
                    LOG.info("Importointi on valmis! Onnistuneita importointeja {}", i);
                    if (prosessi.getVirhe() != 0) {
                        LOG.error("Importoinnin valimistumisen jalkeen epaonnistuneita importointeja {}", i);
                    }
                }
            }
        };
    }

    private Processor logSuccessfulHakuGet() {
        return new Processor() {
            public void process(Exchange exchange) {
                HakuImportProsessi prosessi = exchange.getProperty(PROPERTY_VALVOMO_PROSESSI, HakuImportProsessi.class);
                @SuppressWarnings("unchecked")
                Collection<String> hakukohdeOids = (Collection<String>) exchange.getIn().getBody(Collection.class);
                prosessi.setHakukohteita(hakukohdeOids.size());
                LOG.info("Hakukohteita importoitavana {}", hakukohdeOids.size());
            }
        };
    }

    private Processor logSuccessfulHakuImport() {
        return new Processor() {
            public void process(Exchange exchange) {
                HakuImportProsessi prosessi = exchange.getProperty(PROPERTY_VALVOMO_PROSESSI, HakuImportProsessi.class);
                int t = prosessi.lisaaTuonti();
                if (t % 25 == 0 || t == prosessi.getHakukohteita()) {
                    LOG.info("Hakukohde on tuotu onnistuneesti ({}/{}).",
                            new Object[]{t, prosessi.getHakukohteita()});
                }
            }
        };
    }

    private Processor logFailedHakuConvert() {
        return new Processor() {
            public void process(Exchange exchange) {
                HakuImportProsessi prosessi = exchange.getProperty(PROPERTY_VALVOMO_PROSESSI, HakuImportProsessi.class);
                String oid = exchange.getIn().getBody(String.class);
                if (oid != null) {
                    prosessi.lisaaVirhe(oid + "_KONVERSIOSSA");
                } else {
                    prosessi.lisaaVirhe("<<Tuntematon hakukohde>>_KONVERSIOSSA");
                }
                LOG.error("Epaonnistuneita hakukohdeOideja tahan mennessa {}", Arrays.toString(prosessi.getEpaonnistuneetHakukohteet()));
            }
        };
    }

    private Processor logFailedHakuImport() {
        return new Processor() {
            public void process(Exchange exchange) {
                HakuImportProsessi prosessi = exchange.getProperty(
                        PROPERTY_VALVOMO_PROSESSI, HakuImportProsessi.class);
                HakukohdeImportDTO hki = exchange.getIn().getBody(
                        HakukohdeImportDTO.class);
                if (hki != null) {
                    prosessi.lisaaVirhe(hki.getHakukohdeOid() + "_VIENNISSA");
                } else {
                    prosessi.lisaaVirhe("<<Tuntematon hakukohde>>_VIENNISSA");
                }
                LOG.error("Epaonnistuneita hakukohdeOideja tahan mennessa {}", Arrays.toString(prosessi.getEpaonnistuneetHakukohteet()));
            }
        };
    }
}
