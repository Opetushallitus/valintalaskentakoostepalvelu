package fi.vm.sade.valinta.kooste.valintalaskentatulos.route;

import fi.vm.sade.valinta.kooste.OPH;
import java.io.InputStream;
import org.apache.camel.Property;

public interface ValintalaskentaTulosExcelRoute {

  final String DIRECT_VALINTALASKENTA_EXCEL = "direct:kaynnistaValintalaskennanTulostenLuontiXlsMuodossaReitti";

  InputStream luoXls(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid);
}
