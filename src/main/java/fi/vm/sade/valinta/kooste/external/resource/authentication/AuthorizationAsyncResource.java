package fi.vm.sade.valinta.kooste.external.resource.authentication;

import fi.vm.sade.authentication.business.service.Authorizer;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import org.springframework.security.core.Authentication;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author Jussi Jartamo
 */
public interface AuthorizationAsyncResource {

    void checkOrganisationAccess(
            Authentication authentication,
            String hakuOid,
            String hakukohdeOid, Collection<String> roolit,
            Consumer<Response> responseCallback, Consumer<Throwable> failureCallback);

}
