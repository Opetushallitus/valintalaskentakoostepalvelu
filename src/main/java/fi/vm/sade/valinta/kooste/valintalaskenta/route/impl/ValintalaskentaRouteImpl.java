package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.SplitHakukohteetKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HakukohteenValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HaunValintalaskentaRoute;

/**
 * @author Jussi Jartamo
 */
@Component
public class ValintalaskentaRouteImpl extends SpringRouteBuilder {

    @Autowired
    private SecurityPreprocessor securityProcessor;

    @Override
    public void configure() throws Exception {
        // <route>
        // <from uri="direct:kaynnistaHaunValintalaskentaReitti"/>
        // <setProperty propertyName="hakuOid">
        // <simple>${body.args[0]}</simple>
        // </setProperty>
        // <policy ref="admin">
        // <process ref="securityPreprocessor"/>
        // <to uri="bean:hakukohteetTarjonnaltaKomponentti"/>
        // <split parallelProcessing="false" stopOnException="false">
        // <method bean="splitHakukohteetKomponentti"
        // method="splitHakukohteet"/>
        // <process ref="securityPreprocessor"/>
        // <setProperty
        // propertyName="hakukohdeOid"><simple>${body}</simple></setProperty>
        // <to uri="direct:suoritaLaskenta"/>
        // </split>
        // </policy>
        // </route>
        from("direct:suorita_valintalaskenta").bean(securityProcessor)

        .to("bean:suoritaLaskentaKomponentti");

        from(haunValintalaskenta())
                //
                .bean(securityProcessor)
                //
                .to("bean:hakukohteetTarjonnaltaKomponentti")
                //
                .bean(new SplitHakukohteetKomponentti())
                //
                .split(body()).parallelProcessing().setProperty("hakukohdeOid", body())
                .to("direct:suorita_valintalaskenta").end();

        // <route>
        // <from uri="direct:suoritaLaskenta"/>
        // <to uri="bean:haeValintaperusteetKomponentti"/>
        // <setProperty
        // propertyName="valintaperusteet"><simple>${body}</simple></setProperty>
        // <to uri="bean:suoritaLaskentaKomponentti"/>
        // </route>

        // //////////////////

        // <route>
        // <from uri="direct:kaynnistaHakukohteenValintalaskentaReitti"/>
        // <policy ref="admin">
        // <process ref="securityPreprocessor"/>
        // <setProperty
        // propertyName="hakukohdeOid"><simple>${body.args[0]}</simple></setProperty>
        // <setProperty
        // propertyName="valinnanvaihe"><simple>${body.args[1]}</simple></setProperty>
        // <to uri="direct:suoritaLaskenta"/>
        // </policy>
        // </route>

        from(hakukohteenValintalaskenta()).to("bean:haeValintaperusteetKomponentti")
                .setProperty("valintaperusteet", body()).to("direct:suorita_valintalaskenta");

    }

    private String hakukohteenValintalaskenta() {
        return HakukohteenValintalaskentaRoute.DIRECT_VALINTALASKENTA_HAKUKOHTEELLE;
    }

    private String haunValintalaskenta() {
        return HaunValintalaskentaRoute.DIRECT_VALINTALASKENTA_HAULLE;
    }
}
