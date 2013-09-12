package fi.vm.sade.valinta.kooste.valintalaskentatulos.proxy;

import java.io.InputStream;

public interface JalkiohjaustulosExcelProxy {

    InputStream luoXls(String hakuOid);
}
