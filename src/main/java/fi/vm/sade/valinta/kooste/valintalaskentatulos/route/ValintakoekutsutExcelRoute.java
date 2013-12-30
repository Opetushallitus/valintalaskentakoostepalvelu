package fi.vm.sade.valinta.kooste.valintalaskentatulos.route;

import java.io.InputStream;
import java.util.List;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

public interface ValintakoekutsutExcelRoute {

    final String DIRECT_VALINTAKOE_EXCEL = "direct:kaynnistaTulostenLuontiXlsMuodossaReitti";

    InputStream luoXls(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
            @Property("valintakoeOid") List<String> valintakoeOid);
}
