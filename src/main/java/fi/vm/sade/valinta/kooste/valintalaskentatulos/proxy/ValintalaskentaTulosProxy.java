package fi.vm.sade.valinta.kooste.valintalaskentatulos.proxy;

import java.io.InputStream;
import java.util.List;

public interface ValintalaskentaTulosProxy {

    InputStream haeTuloksetXlsMuodossa(String hakukohdeOid, List<String> valintakoeOid);
}
