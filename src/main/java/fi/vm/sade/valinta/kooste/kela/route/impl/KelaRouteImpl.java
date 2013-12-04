package fi.vm.sade.valinta.kooste.kela.route.impl;

import static fi.vm.sade.valinta.kooste.dokumenttipalvelu.SendMessageToDocumentService.MESSAGE;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.fail;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.finish;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.kuvaus;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.prosessi;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.start;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.camel.Body;
import org.apache.camel.Property;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fi.vm.sade.rajapinnat.kela.tkuva.util.KelaUtil;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.dokumenttipalvelu.SendMessageToDocumentService;
import fi.vm.sade.valinta.kooste.kela.komponentti.KelaDokumentinLuontiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.KelaHakijaRiviKomponentti;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.PrepareKelaProcessDescription;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKaikkiPaikanVastaanottaneet;

/**
 * @author Jussi Jartamo
 * 
 *         Route to Kela.
 */
@Component
public class KelaRouteImpl extends SpringRouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(KelaRouteImpl.class);

    @Autowired
    private KelaHakijaRiviKomponentti kelaHakijaKomponentti;

    @Autowired
    private KelaDokumentinLuontiKomponentti kelaDokumentinLuontiKomponentti;

    @Autowired
    private SijoitteluKaikkiPaikanVastaanottaneet sijoitteluVastaanottaneet;

    @Autowired
    private DokumenttiResource dokumenttiResource;

    @Autowired
    private SendMessageToDocumentService messageService;
    private PrepareKelaProcessDescription luoUusiProsessi;

    public KelaRouteImpl() {
        this.luoUusiProsessi = new PrepareKelaProcessDescription();
    }

    /**
     * @param processFactory
     *            For unit tests
     */
    public KelaRouteImpl(PrepareKelaProcessDescription processFactory) {
        this.luoUusiProsessi = processFactory;
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
        setErrorHandlerBuilder(deadLetterChannel(kelaFailed())
        // max redeliveries 1?
                .maximumRedeliveries(1).logStackTrace(false));
        from(kelaFailed())
        // merkkaa prosessi failediksi
                .to(fail())
                // informoi kayttajaa
                .setHeader(MESSAGE, constant("Kela-dokumentin luonti epäonnistui.")).bean(messageService);

        /**
         * Kela-dokkarin luonti reitti
         */
        from(kelaLuonti())
        // RESURSSI
                .setProperty(kuvaus(), constant("Dokumentin luonti")).setProperty(prosessi(), method(luoUusiProsessi))
                // Start prosessi valvomoon dokumentin luonnin aloittamisesta
                .wireTap(start()).end()
                // ilmoitetaan dokumenttipalveluun aloitetusta luonnista
                // (informoi kayttajaa)
                .setHeader(MESSAGE, constant("Kela-dokumentin luonti aloitettu.")).bean(messageService)
                // haetaan sijoittelusta vastaanottaneet hakijat
                .bean(sijoitteluVastaanottaneet)
                // List<HakijaDTO> -->
                .split(body(), createAccumulatingAggregation())
                // HakijaDTO -->
                .bean(kelaHakijaKomponentti).end()
                // Collection<TKUVAYHVA> ->
                .bean(kelaDokumentinLuontiKomponentti)
                // lahetetaan valmis inputstream dokumenttipalveluun kayttajan
                // ladattavaksi. Body == InputStream ->
                .bean(new SendKelaDocument())
                // Done valvomoon
                .wireTap(finish()).end();

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
