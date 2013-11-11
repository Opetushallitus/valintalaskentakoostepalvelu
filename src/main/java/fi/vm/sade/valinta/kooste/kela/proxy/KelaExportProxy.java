package fi.vm.sade.valinta.kooste.kela.proxy;

import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.concurrent.Future;

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
