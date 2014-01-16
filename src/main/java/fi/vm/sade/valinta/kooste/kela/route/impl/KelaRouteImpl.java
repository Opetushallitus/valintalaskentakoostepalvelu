package fi.vm.sade.valinta.kooste.kela.route.impl;

import static fi.vm.sade.valinta.kooste.dokumenttipalvelu.SendMessageToDocumentService.MESSAGE;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.fail;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.finish;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.kuvaus;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.prosessi;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.start;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Property;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import fi.vm.sade.rajapinnat.kela.tkuva.util.KelaUtil;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.Message;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaDokumentinLuontiKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaHakijaRiviKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.PrepareKelaProcessDescription;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKaikkiPaikanVastaanottaneet;

/**
 * @author Jussi Jartamo
 * 
 *         Route to Kela.
 */
@Component
public class KelaRouteImpl extends SpringRouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(KelaRouteImpl.class);
    private static final String ENSIMMAINEN_VIRHE = "ensimmainen_virhe_reitilla";
    private final KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti;
    private final KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti;
    private final SijoitteluKaikkiPaikanVastaanottaneet sijoitteluVastaanottaneet;
    private final DokumenttiResource dokumenttiResource;
    private final PrepareKelaProcessDescription luoUusiProsessi;

    @Autowired
    public KelaRouteImpl(@Qualifier("dokumenttipalveluRestClient") DokumenttiResource dokumenttiResource,
            KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti,
            KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti,
            SijoitteluKaikkiPaikanVastaanottaneet sijoitteluVastaanottaneet) {
        this.luoUusiProsessi = new PrepareKelaProcessDescription();
        this.dokumenttiResource = dokumenttiResource;
        this.kelaHakijaKomponentti = kelaHakijaKomponentti;
        this.sijoitteluVastaanottaneet = sijoitteluVastaanottaneet;
        this.kelaDokumentinLuontiKomponentti = kelaDokumentinLuontiKomponentti;
    }

    public class SendKelaDocument {
        public void send(@Body InputStream filedata, @Property(OPH.HAKUOID) String hakuOid) {
            List<String> tags = Lists.newArrayList();
            tags.add(hakuOid);
            tags.add("kela");
            tags.add("valintalaskentakoostepalvelu");
            dokumenttiResource.tallenna(KelaUtil.createTiedostoNimiYhva14(new Date()), DateTime.now().plusDays(1)
                    .getMillis(), tags, "", filedata);
        }
    }

    /**
     * Kela Camel Configuration: Siirto and document generation.
     */
    public final void configure() {

        from(kelaFailed()).process(new Processor() {
            public void process(Exchange exchange) throws Exception {
                AtomicBoolean onkoEnsimmainenVirhe = exchange.getProperty(ENSIMMAINEN_VIRHE, AtomicBoolean.class);
                if (onkoEnsimmainenVirhe != null && onkoEnsimmainenVirhe.compareAndSet(true, false)) {
                    dokumenttiResource.viesti(new Message("Kela-dokumentin luonti epäonnistui.", Arrays.asList(
                            "valintalaskentakoostepalvelu", "kela"), DateTime.now().plusDays(1).toDate()));
                }
            }
        })
        // merkkaa prosessi failediksi

                // informoi kayttajaa
                .setHeader(MESSAGE, constant("Kela-dokumentin luonti epäonnistui."))
                //
                .to(fail());
        //
        // Vaan eka virhe logataan
        //

        from("direct:kela_yksittainen_rivi")
        //
                .errorHandler(deadLetterChannel(kelaFailed())// .useOriginalMessage()
                        //
                        // (kelaFailed())
                        //
                        .maximumRedeliveries(10).redeliveryDelay(300L)
                        // log exhausted stacktrace
                        .logExhaustedMessageHistory(true).logExhausted(true)
                        // hide retry/handled stacktrace
                        .logStackTrace(false).logRetryStackTrace(false).logHandled(false)
                //
                )
                //
                .process(new SecurityPreprocessor())
                //
                .bean(kelaHakijaKomponentti);

        /**
         * Kela-dokkarin luonti reitti
         */
        from(kelaLuonti())
        //
                .errorHandler(deadLetterChannel(kelaFailed())
                //
                        .logExhaustedMessageHistory(true).logExhausted(true)
                        // hide retry/handled stacktrace
                        .logStackTrace(true).logRetryStackTrace(true).logHandled(true))
                //
                .process(new SecurityPreprocessor())
                //
                .setProperty(ENSIMMAINEN_VIRHE, constant(new AtomicBoolean(true)))
                // RESURSSI
                .setProperty(kuvaus(), constant("Dokumentin luonti"))
                //
                .setProperty(prosessi(), method(luoUusiProsessi))
                // Start prosessi valvomoon dokumentin luonnin aloittamisesta
                .to(start())
                // ilmoitetaan dokumenttipalveluun aloitetusta luonnista
                // (informoi kayttajaa)
                //
                .setBody(
                        constant(new Message("Kela-dokumentin luonti aloitettu.", Arrays.asList(
                                "valintalaskentakoostepalvelu", "kela"), DateTime.now().plusDays(1).toDate())))
                //
                .bean(dokumenttiResource, "viesti")
                // haetaan sijoittelusta vastaanottaneet hakijat
                .bean(sijoitteluVastaanottaneet)
                // List<HakijaDTO> -->
                .split(body(), createAccumulatingAggregation())
                //
                .shareUnitOfWork()
                //

                //
                .parallelProcessing()
                //
                .stopOnException()
                //

                // HakijaDTO -->
                .to("direct:kela_yksittainen_rivi").end()
                // Collection<Collection<TKUVAYHVA>> ->
                .process(new Processor() { // FLATTEN
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                exchange.getOut().setBody(Iterables.concat((List<?>) exchange.getIn().getBody()));
                            }
                        })
                // Collection<TKUVAYHVA> ->
                .bean(kelaDokumentinLuontiKomponentti)
                // lahetetaan valmis inputstream dokumenttipalveluun kayttajan
                // ladattavaksi. Body == InputStream ->

                // dokumenttiResource.viesti(new Message(message, tags,
                // DateTime.now().plusDays(1).toDate()));
                .setBody(
                        constant(new Message("Dokumentinluonti onnistui", Arrays.asList("valintalaskentakoostepalvelu",
                                "kela"), DateTime.now().plusDays(1).toDate())))
                //
                .bean(dokumenttiResource, "viesti")
                // Done valvomoon
                .to(finish());

    }

    /**
     * @return Arraylist aggregation strategy.
     */
    private <T> AggregationStrategy createAccumulatingAggregation() {
        return new FlexibleAggregationStrategy<T>().storeInBody().accumulateInCollection(ArrayList.class);
    }

    /**
     * @return direct:kela_siirto
     */
    private String kelaFailed() {
        return KelaRoute.DIRECT_KELA_FAILED;
    }

    /**
     * @return direct:kela_luonti
     */
    private String kelaLuonti() {
        return KelaRoute.DIRECT_KELA_LUONTI;
    }

}
