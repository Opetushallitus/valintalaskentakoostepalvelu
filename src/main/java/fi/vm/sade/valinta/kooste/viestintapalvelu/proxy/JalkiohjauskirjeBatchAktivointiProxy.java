package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

import org.springframework.security.core.Authentication;

import java.util.concurrent.Future;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface JalkiohjauskirjeBatchAktivointiProxy {

    Future<Object> jalkiohjauskirjeetAktivoi(String hakuOid, Authentication auth);
}
