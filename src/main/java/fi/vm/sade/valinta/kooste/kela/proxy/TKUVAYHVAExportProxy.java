package fi.vm.sade.valinta.kooste.kela.proxy;

import java.io.InputStream;
import java.util.Date;

/**
 * 
 * @author Jussi Jartamo
 * 
 * 
 */
public interface TKUVAYHVAExportProxy {

    InputStream luoTKUVAYHVA(String hakuOid, Date lukuvuosi, Date poimintapaivamaara);
}
