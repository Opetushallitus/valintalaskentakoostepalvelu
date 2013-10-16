package fi.vm.sade.valinta.kooste.kela.proxy;

import java.util.Collection;
import java.util.Date;

/**
 * 
 * @author Jussi Jartamo
 * 
 * 
 */
public interface TKUVAYHVAExportProxy {

    Collection luoTKUVAYHVA(String hakuOid, Date lukuvuosi, Date poimintapaivamaara);
}
