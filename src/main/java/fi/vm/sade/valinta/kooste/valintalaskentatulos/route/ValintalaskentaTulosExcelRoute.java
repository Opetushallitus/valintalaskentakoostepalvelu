package fi.vm.sade.valinta.kooste.valintalaskentatulos.route;

import java.io.InputStream;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

public interface ValintalaskentaTulosExcelRoute {

    final String DIRECT_VALINTALASKENTA_EXCEL = "direct:kaynnistaValintalaskennanTulostenLuontiXlsMuodossaReitti";

    InputStream luoXls(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid);
}
