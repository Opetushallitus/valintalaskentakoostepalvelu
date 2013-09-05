package fi.vm.sade.valinta.kooste.valintalaskentatulos.proxy;

import java.io.InputStream;

public interface SijoittelunTulosExcelProxy {

    public InputStream luoXls(String hakukohdeOid, Long sijoitteluajoId);
}
