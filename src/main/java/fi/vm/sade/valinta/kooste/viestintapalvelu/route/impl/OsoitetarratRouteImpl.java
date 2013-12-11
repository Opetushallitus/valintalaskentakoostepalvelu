package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.valinta.kooste.exception.ViestintapalveluException;
import fi.vm.sade.valinta.kooste.sijoittelu.proxy.SijoitteluKoulutuspaikallisetProxy;
import fi.vm.sade.valinta.kooste.valintatieto.komponentti.ValintatietoHakukohteelleKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksyttyjenOsoitetarratRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.OsoitetarratRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Hyvaksyttyjen Osoitetarrat ja osoitetarrat koekutsua varten
 */
@Component
public class OsoitetarratRouteImpl extends SpringRouteBuilder {

    @Autowired
    private ViestintapalveluResource viestintapalveluResource;

    @Autowired
    private ValintatietoHakukohteelleKomponentti valintatietoHakukohteelleKomponentti;

    @Autowired
    private HaeOsoiteKomponentti osoiteKomponentti;

    @Autowired
    private SijoitteluKoulutuspaikallisetProxy sijoitteluProxy;

    public static class LuoOsoitteet {
        public Osoitteet luo(List<Osoite> osoitteet) {
            if (osoitteet == null || osoitteet.isEmpty()) {
                throw new ViestintapalveluException("Yritetään luoda nolla kappaletta osoitetarroja!");
            }
            return new Osoitteet(osoitteet);
        }
    }

    @Override
    public void configure() throws Exception {
        configureHyvaksyttyjenOsoitetarrat();
        configureOsoitetarrat();
    }

    private void configureHyvaksyttyjenOsoitetarrat() throws Exception {
        from(hyvaksyttyjenOsoitetarrat()).bean(sijoitteluProxy)
        //
                .split(body(), osoiteAggregation())
                // filter OSALLISTUU
                .to(kasitteleHakija()).bean(osoiteKomponentti)
                //
                .end()
                // enrich to Osoitteet
                .bean(new LuoOsoitteet())
                //
                .bean(viestintapalveluResource, "haeOsoitetarrat");

        from(kasitteleHakija())
        // enrich to hakemusOid
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setBody(exchange.getIn().getBody(HakijaDTO.class).getHakemusOid());
                    }
                });
    }

    private void configureOsoitetarrat() throws Exception {
        from(osoitetarrat()).bean(valintatietoHakukohteelleKomponentti)
        //
                .split(body(), osoiteAggregation())
                // filter OSALLISTUU
                .to(kasitteleHakemusOsallistuminen()).bean(osoiteKomponentti)
                //
                .end()
                // enrich to Osoitteet
                .bean(new LuoOsoitteet())
                //
                .bean(viestintapalveluResource, "haeOsoitetarrat");

        from(kasitteleHakemusOsallistuminen())
        //
                .filter(new Predicate() {
                    public boolean matches(Exchange exchange) {
                        HakemusOsallistuminenTyyppi o = exchange.getIn().getBody(HakemusOsallistuminenTyyppi.class);
                        for (ValintakoeOsallistuminenTyyppi o1 : o.getOsallistumiset()) {
                            if (Osallistuminen.OSALLISTUU.equals(o1.getOsallistuminen())) {
                                return true;
                            }
                        }
                        return false;
                    }
                })
                // enrich to hakemusOid
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setBody(
                                exchange.getIn().getBody(HakemusOsallistuminenTyyppi.class).getHakemusOid());
                    }
                });
    }

    private FlexibleAggregationStrategy<Osoite> osoiteAggregation() {
        return new FlexibleAggregationStrategy<Osoite>().storeInBody().accumulateInCollection(ArrayList.class);
    }

    private String osoitetarrat() {
        return OsoitetarratRoute.DIRECT_OSOITETARRAT;
    }

    private String hyvaksyttyjenOsoitetarrat() {
        return HyvaksyttyjenOsoitetarratRoute.DIRECT_HYVAKSYTTYJEN_OSOITETARRAT;
    }

    private final String DIRECT_KASITTELE_OSALLISTUMINEN = "direct:osoitetarrat_osallistuminen";

    private String kasitteleHakemusOsallistuminen() {
        return DIRECT_KASITTELE_OSALLISTUMINEN;
    }

    private final String DIRECT_KASITTELE_HAKIJA = "direct:osoitetarrat_hakija";

    private String kasitteleHakija() {
        return DIRECT_KASITTELE_HAKIJA;
    }

}
