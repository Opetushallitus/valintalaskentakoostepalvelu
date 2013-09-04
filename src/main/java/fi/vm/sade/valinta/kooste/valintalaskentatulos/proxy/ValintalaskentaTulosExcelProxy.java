package fi.vm.sade.valinta.kooste.valintalaskentatulos.proxy;

import java.io.InputStream;

public interface ValintalaskentaTulosExcelProxy {

    InputStream haeValintalaskennanTuloksetXlsMuodossa(String hakukohdeOid);
}
