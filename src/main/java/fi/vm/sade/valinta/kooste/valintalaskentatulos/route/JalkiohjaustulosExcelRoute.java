package fi.vm.sade.valinta.kooste.valintalaskentatulos.route;

import java.io.InputStream;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

public interface JalkiohjaustulosExcelRoute {

    final String DIRECT_JALKIOHJAUS_EXCEL = "direct:kaynnistaJalkiohjaustulostenLuontiXlsMuodossaReitti";

    InputStream luoXls(@Property(OPH.HAKUOID) String hakuOid);
}
