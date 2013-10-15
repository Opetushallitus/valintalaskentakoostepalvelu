package fi.vm.sade.valinta.kooste.kela.proxy;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 * 
 */
public interface TKUVAYHVAExportProxy {

    List<InputStream> luoTKUVAYHVA(String hakuOid, Date lukuvuosi, Date poimintapaivamaara);
}
