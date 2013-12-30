package fi.vm.sade.valinta.kooste.valintalaskentatulos.route.impl;

import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.JalkiohjaustulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.SijoittelunTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.ValintalaskennanTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.ValintalaskentaTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.JalkiohjaustulosExcelRoute;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.SijoittelunTulosExcelRoute;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.ValintakoekutsutExcelRoute;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.ValintalaskentaTulosExcelRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class ValintalaskentaTulosRouteImpl extends SpringRouteBuilder {

    private JalkiohjaustulosExcelKomponentti jalkiohjaustulosExcelKomponentti;
    private SijoittelunTulosExcelKomponentti sijoittelunTulosExcelKomponentti;
    private ValintalaskennanTulosExcelKomponentti valintalaskennanTulosExcelKomponentti;
    private ValintalaskentaTulosExcelKomponentti valintalaskentaTulosExcelKomponentti;

    public ValintalaskentaTulosRouteImpl(JalkiohjaustulosExcelKomponentti jalkiohjaustulosExcelKomponentti,
            SijoittelunTulosExcelKomponentti sijoittelunTulosExcelKomponentti,
            ValintalaskennanTulosExcelKomponentti valintalaskennanTulosExcelKomponentti,
            ValintalaskentaTulosExcelKomponentti valintalaskentaTulosExcelKomponentti) {
        this.jalkiohjaustulosExcelKomponentti = jalkiohjaustulosExcelKomponentti;
        this.sijoittelunTulosExcelKomponentti = sijoittelunTulosExcelKomponentti;
        this.valintalaskennanTulosExcelKomponentti = valintalaskennanTulosExcelKomponentti;
        this.valintalaskentaTulosExcelKomponentti = valintalaskentaTulosExcelKomponentti;
    }

    @Override
    public void configure() throws Exception {
        from(JalkiohjaustulosExcelRoute.DIRECT_JALKIOHJAUS_EXCEL).bean(jalkiohjaustulosExcelKomponentti);
        from(SijoittelunTulosExcelRoute.DIRECT_SIJOITTELU_EXCEL).bean(sijoittelunTulosExcelKomponentti);

        from(ValintalaskentaTulosExcelRoute.DIRECT_VALINTALASKENTA_EXCEL).bean(valintalaskennanTulosExcelKomponentti);
        from(ValintakoekutsutExcelRoute.DIRECT_VALINTAKOE_EXCEL).bean(valintalaskentaTulosExcelKomponentti);

    }

}
