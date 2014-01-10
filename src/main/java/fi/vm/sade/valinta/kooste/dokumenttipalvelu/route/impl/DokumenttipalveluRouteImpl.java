package fi.vm.sade.valinta.kooste.dokumenttipalvelu.route.impl;

import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;

@Component
public class DokumenttipalveluRouteImpl extends SpringRouteBuilder {

    private final String quartzDocumentServiceFlush;
    private final DokumenttiResource dokumenttiResource;

    @Autowired
    public DokumenttipalveluRouteImpl(
            @Value("quartz://documentServiceFlush?cron=0+0+0/2+*+*+?") String quartzDocumentServiceFlush,
            DokumenttiResource dokumenttiResource) {
        this.quartzDocumentServiceFlush = quartzDocumentServiceFlush;
        this.dokumenttiResource = dokumenttiResource;
    }

    @Override
    public void configure() throws Exception {
        from(quartzDocumentServiceFlush).bean(dokumenttiResource, "tyhjenna");
    }

}
