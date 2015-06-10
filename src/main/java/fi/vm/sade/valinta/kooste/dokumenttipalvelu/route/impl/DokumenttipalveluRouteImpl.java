package fi.vm.sade.valinta.kooste.dokumenttipalvelu.route.impl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;

@Component
public class DokumenttipalveluRouteImpl extends SpringRouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(DokumenttipalveluRouteImpl.class);
    private final String quartzDocumentServiceFlush;
    private final DokumenttiResource dokumenttiResource;

    @Autowired
    public DokumenttipalveluRouteImpl(
            @Value("quartz://documentServiceFlush?cron=0+0+0/2+*+*+?") String quartzDocumentServiceFlush,
            @Qualifier("adminDokumenttipalveluRestClient") DokumenttiResource dokumenttiResource) {
        this.quartzDocumentServiceFlush = quartzDocumentServiceFlush;
        this.dokumenttiResource = dokumenttiResource;
    }

    @Override
    public void configure() throws Exception {
        from(quartzDocumentServiceFlush)
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        try {
                            dokumenttiResource.tyhjenna();
                        } catch (Exception e) {
                            LOG.error("Dokumenttipalvelun tyhjennys-kutsu epäonnistui! {}", e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
    }
}
