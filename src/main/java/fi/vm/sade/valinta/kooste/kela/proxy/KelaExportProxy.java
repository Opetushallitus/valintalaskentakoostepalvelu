package fi.vm.sade.valinta.kooste.kela.proxy;

import java.util.Date;
import java.util.concurrent.Future;

import org.springframework.security.core.Authentication;

/**
 * 
 * @author Jussi Jartamo
 * 
 * 
 */
public interface KelaExportProxy {

    Future<Object> luoTKUVAYHVA(String hakuOid, Date lukuvuosi, Date poimintapaivamaara, String aineistonNimi,
            String organisaationNimi, Authentication authentication);
}
