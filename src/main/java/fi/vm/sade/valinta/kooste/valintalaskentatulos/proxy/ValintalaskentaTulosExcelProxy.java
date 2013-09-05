package fi.vm.sade.valinta.kooste.valintalaskentatulos.proxy;

import java.io.InputStream;

public interface ValintalaskentaTulosExcelProxy {

    InputStream luoXls(String hakukohdeOid);
}
