package fi.vm.sade.valinta.kooste.valintalaskentatulos.route;

import java.io.InputStream;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

public interface SijoittelunTulosExcelRoute {

    final String DIRECT_SIJOITTELU_EXCEL = "direct:sijoittelunTulosExcelReitti";

    public InputStream luoXls(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
            @Property(OPH.SIJOITTELUAJOID) Long sijoitteluajoId, @Property(OPH.HAKUOID) String hakuOid);
}
