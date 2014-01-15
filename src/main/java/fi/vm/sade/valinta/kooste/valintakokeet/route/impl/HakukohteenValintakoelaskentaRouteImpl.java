package fi.vm.sade.valinta.kooste.valintakokeet.route.impl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakukohteenHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HakemusOidSplitter;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.LaskeValintakoeosallistumisetHakemukselleKomponentti;
import fi.vm.sade.valinta.kooste.valintakokeet.route.HakukohteenValintakoelaskentaRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class HakukohteenValintakoelaskentaRouteImpl extends SpringRouteBuilder {

    @Autowired
    private HaeHakukohteenHakemuksetKomponentti haeHakukohteenHakemuksetKomponentti;

    @Autowired
    private LaskeValintakoeosallistumisetHakemukselleKomponentti laskeValintakoeosallistumisetHakemukselleKomponentti;

    @Override
    public void configure() throws Exception {
        from(hakukohteenValintakoelaskentaDeadLetterChannel())
                .setHeader(
                        "message",
                        simple("[${property.authentication.name}] Valintakoelaskenta ei toimi. Hakukohteelle  ${property.hakukohdeOid},  ${property.hakemusOid}, haku ${property.hakuOid}"))
                .to(fail());

        from(hakukohteenValintakoelaskenta())
        //
                .errorHandler(deadLetterChannel(hakukohteenValintakoelaskentaDeadLetterChannel()))
                //
                .bean(new SecurityPreprocessor())
                //
                .process(luoProsessiHakukohteenValintakoelaskennalle())
                //
                .to(start())
                //
                .bean(haeHakukohteenHakemuksetKomponentti)
                //
                .bean(new HakemusOidSplitter())
                //
                .split(body())
                //
                .setProperty(OPH.HAKEMUSOID, body())
                //
                .bean(laskeValintakoeosallistumisetHakemukselleKomponentti)
                //
                .end()
                //
                .to(finish());
    }

    private String hakukohteenValintakoelaskenta() {
        return HakukohteenValintakoelaskentaRoute.DIRECT_HAKUKOHTEEN_VALINTAKOELASKENTA;
    }

    private Processor luoProsessiHakukohteenValintakoelaskennalle() {
        return new Processor() {
            public void process(Exchange exchange) throws Exception {
                String hakukohdeOid = exchange.getProperty(OPH.HAKUKOHDEOID, String.class);
                exchange.setProperty(kuvaus(), "Valintakoelaskenta hakukohteelle " + hakukohdeOid);
                exchange.setProperty(prosessi(), new Prosessi("Valintakoelaskenta", "Hakukohteelle " + hakukohdeOid,
                        exchange.getProperty(OPH.HAKUOID, String.class)));
            }
        };
    }

    private static String kuvaus() {
        return ValvomoAdminService.PROPERTY_VALVOMO_PROSESSIKUVAUS;
    }

    private static String prosessi() {
        return ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI;
    }

    private static String fail() {
        return "bean:haunValintakoelaskentaValvomo?method=fail";
    }

    private static String start() {
        return "bean:haunValintakoelaskentaValvomo?method=start";
    }

    private static String finish() {
        return "bean:haunValintakoelaskentaValvomo?method=finish";
    }

    private static String hakukohteenValintakoelaskentaDeadLetterChannel() {
        return "direct:kaynnistaHakukohteenValintakoelaskentaReitti_deadletterchannel";
    }
}
