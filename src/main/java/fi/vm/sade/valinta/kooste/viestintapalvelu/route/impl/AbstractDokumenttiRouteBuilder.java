package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.IOUtils;
import java.util.UUID;
import org.joda.time.DateTime;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tunniste;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

/**
 *         Dokumenttiprosessin käsittelyyn valmiit camel dsl:t
 *         dokumenttireittejä varten
 */
public abstract class AbstractDokumenttiRouteBuilder extends SpringRouteBuilder {
    public AbstractDokumenttiRouteBuilder() {
    }

    protected DokumenttiProsessi dokumenttiprosessi(Exchange exchange) {
        return exchange.getProperty(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI, DokumenttiProsessi.class);
    }

    protected List<String> hakemusOids(Exchange exchange) {
        return exchange.getProperty("hakemusOids", Collections.emptyList(), List.class);
    }

    protected String hakukohdeOid(Exchange exchange) {
        return exchange.getProperty(OPH.HAKUKOHDEOID, String.class);
    }

    protected String hakuOid(Exchange exchange) {
        return exchange.getProperty(OPH.HAKUOID, String.class);
    }

    protected AuditSession auditSession(Exchange exchange) {
        return exchange.getProperty("AuditSession", AuditSession.class);
    }

    protected String sijoitteluajoId(Exchange exchange) {
        return exchange.getProperty(OPH.SIJOITTELUAJOID, String.class);
    }

    protected Date defaultExpirationDate() {
        return DateTime.now().plusHours(168).toDate();
    }

    protected String generateId() {
        return UUID.randomUUID().toString();
    }

    protected Exception kasittelePoikkeus(String palvelu, Exchange exchange, Exception exception, Tunniste... oids) {
        exchange.setException(exception);
        dokumenttiprosessi(exchange).getPoikkeukset().add(new Poikkeus(palvelu, StringUtils.EMPTY, exception.getMessage(), oids));
        return exception;
    }

    protected InputStream pipeInputStreams(InputStream incoming) throws IOException {
        byte[] dokumentti = IOUtils.toByteArray(incoming);
        if (dokumentti == null || dokumentti.length == 0) {
            throw new RuntimeException("Viestintäpalvelu palautti tyhjän dokumentin!");
        }
        InputStream p = new ByteArrayInputStream(dokumentti);
        incoming.close();
        return p;
    }

    protected Predicate isEmpty(final ValueBuilder v) {
        return exchange -> {
            try {
                Collection<?> c = v.evaluate(exchange, Collection.class);
                return c == null || c.isEmpty();
            } catch (Exception e) {
                return true;
            }
        };
    }

    protected String valintatapajonoOid(Exchange exchange) {
        return exchange.getProperty(OPH.VALINTAPAJONOOID, String.class);
    }
}
