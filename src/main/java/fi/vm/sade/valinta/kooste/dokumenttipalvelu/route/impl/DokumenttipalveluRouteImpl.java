package fi.vm.sade.valinta.kooste.dokumenttipalvelu.route.impl;

import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DokumenttipalveluRouteImpl extends SpringRouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(DokumenttipalveluRouteImpl.class);
    private final String quartzDocumentServiceFlush;
    private final DokumenttiAsyncResource dokumenttiAsyncResource;

    @Autowired
    public DokumenttipalveluRouteImpl(
            @Value("quartz://documentServiceFlush?cron=0+0+0/2+*+*+?") String quartzDocumentServiceFlush, DokumenttiAsyncResource dokumenttiAsyncResource) {
        this.quartzDocumentServiceFlush = quartzDocumentServiceFlush;
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
    }

    @Override
    public void configure() throws Exception {
        from(quartzDocumentServiceFlush)
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        try {
                            dokumenttiAsyncResource.tyhjenna();
                        } catch (Exception e) {
                            LOG.info("Dokumenttipalvelun tyhjennys-kutsu epäonnistui! Yritetään uudelleen.", e);
                            try { // FIXME kill me OK-152
                                dokumenttiAsyncResource.tyhjenna();
                            } catch (Exception e2) {
                                LOG.error("Dokumenttipalvelun tyhjennys-kutsu epäonnistui!", e2);
                            }
                        }
                    }
                });
    }
}
