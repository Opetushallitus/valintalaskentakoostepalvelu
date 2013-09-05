package fi.vm.sade.valinta.kooste.valintalaskentatulos.proxy;

import java.io.InputStream;
import java.util.List;

public interface ValintalaskentaTulosProxy {

    InputStream luoXls(String hakukohdeOid, List<String> valintakoeOid);
}
