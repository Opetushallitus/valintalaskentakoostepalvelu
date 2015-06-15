package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.IOUtils;
import org.jgroups.util.UUID;
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
    private final Processor inkrementoiKokonaistyota = new Processor() {
        public void process(Exchange exchange) throws Exception {
            dokumenttiprosessi(exchange).inkrementoiKokonaistyota();
        }
    };
    private final Processor inkrementoiTehtyjaToita = new Processor() {
        public void process(Exchange exchange) throws Exception {
            dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
        }
    };

    public AbstractDokumenttiRouteBuilder() {
    }

    protected Processor merkkaaTyoTehdyksi() {
        return this.inkrementoiTehtyjaToita;
    }

    protected Processor inkrementoiKokonaistyota() {
        return this.inkrementoiKokonaistyota;
    }

    protected Processor asetaKokonaistyo(final int kokonaistoidenMaara) {
        return new Processor() {
            public void process(Exchange exchange) throws Exception {
                dokumenttiprosessi(exchange).setKokonaistyo(kokonaistoidenMaara);
            }
        };
    }

    protected DokumenttiProsessi dokumenttiprosessi(Exchange exchange) {
        return exchange.getProperty(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI, DokumenttiProsessi.class);
    }

    protected Predicate prosessiOnKeskeytetty() {
        return new Predicate() {
            public boolean matches(Exchange exchange) {
                return dokumenttiprosessi(exchange).isKeskeytetty();
            }
        };
    }

    @SuppressWarnings("unchecked")
    protected List<String> valintakoeOids(Exchange exchange) {
        return exchange.getProperty("valintakoeOid", Collections.<String>emptyList(), List.class);
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

    protected String sijoitteluajoId(Exchange exchange) {
        return exchange.getProperty(OPH.SIJOITTELUAJOID, String.class);
    }

    protected Date defaultExpirationDate() {
        return DateTime.now().plusHours(168).toDate(); // almost a day
    }

    protected String generateId() {
        return UUID.randomUUID().toString();
    }

    protected Exception kasittelePoikkeus(String palvelu, Exchange exchange, Exception exception, Tunniste... oids) {
        exchange.setException(exception);
        dokumenttiprosessi(exchange).getPoikkeukset().add(new Poikkeus(palvelu, StringUtils.EMPTY, exception.getMessage(), oids));
        return exception;
    }

    protected Processor kirjaaPoikkeus(final Poikkeus... poikkeus) {
        return new Processor() {
            public void process(Exchange exchange) throws Exception {
                dokumenttiprosessi(exchange).getPoikkeukset().addAll(Lists.newArrayList(poikkeus));
                if (exchange.getException() != null) {
                    dokumenttiprosessi(exchange).getPoikkeukset().add(new Poikkeus("", "", exchange.getException().getMessage()));
                }
            }
        };
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
        return new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                try {
                    Collection<?> c = v.evaluate(exchange, Collection.class);
                    if (c == null) {
                        return true;
                    }
                    return c.isEmpty();
                } catch (Exception e) {
                    return true;
                }
            }
        };
    }

    protected String valintatapajonoOid(Exchange exchange) {
        return exchange.getProperty(OPH.VALINTAPAJONOOID, String.class);
    }
}
