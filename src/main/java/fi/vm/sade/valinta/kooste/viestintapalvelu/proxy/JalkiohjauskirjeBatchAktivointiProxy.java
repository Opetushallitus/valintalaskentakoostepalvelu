package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

import java.util.concurrent.Future;

import org.springframework.security.core.Authentication;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface JalkiohjauskirjeBatchAktivointiProxy {

    Future<Object> jalkiohjauskirjeetAktivoi(String hakuOid, Authentication auth);
}
