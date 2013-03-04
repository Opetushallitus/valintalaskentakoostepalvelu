package fi.vm.sade.valinta.esb.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.esb.HaeLahtotiedotKomponentti;
import fi.vm.sade.valinta.esb.SuoritaLaskentaKomponentti;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Rakentaa reitin valintalaskentaan käyttäen hakemus- ja
 *         valintaperustepalvelua lähtötietoina
 * 
 */
@Component
public class ValintalaskentaRouteBuilderImpl extends RouteBuilder {

    public void configure() throws Exception {
        from("servlet://laske?matchOnUriPrefix=true&servletName=StartLaskentaServlet").process(new Processor() {

            public void process(Exchange exchange) throws Exception {

                Map<String, String> keyValue = createMapFromQueryParameters(exchange.getIn()
                        .getHeader(Exchange.HTTP_QUERY, String.class).split("&"));
                exchange.getOut().setHeader("hakukohdeOid", keyValue.get("hakukohdeOid"));
                exchange.getOut().setHeader("valinnanvaihe", Integer.valueOf(keyValue.get("valinnanvaihe")));
            }

        }).bean(HaeLahtotiedotKomponentti.class, "haeLahtotiedot")// .to("bean:HaeLahtotiedotKomponentti?methodName=haeLahtotiedotKomponentti")
                .bean(SuoritaLaskentaKomponentti.class, "suoritaLaskenta");

    }

    private static Map<String, String> createMapFromQueryParameters(String[] queryParameters) {
        Map<String, String> queryMap = new HashMap<String, String>();
        for (String parameter : queryParameters) {
            String[] keyval = parameter.split("=");
            queryMap.put(keyval[0], keyval[1]);
        }
        return queryMap;
    }
}
