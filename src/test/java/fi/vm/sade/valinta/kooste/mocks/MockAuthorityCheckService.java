package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.pistesyotto.service.HakukohdeOIDAuthorityCheck;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.ForbiddenException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class MockAuthorityCheckService extends AuthorityCheckService {

  @Override
  public CompletableFuture<HakukohdeOIDAuthorityCheck> getAuthorityCheckForRoles(
      Collection<String> roles) {
    return CompletableFuture.completedFuture((oid) -> true);
  }

  @Override
  public void checkAuthorizationForHaku(String hakuOid, Collection<String> requiredRoles) {
    if (hakuOid.equals("unauthorized.oid")) {
      throw new ForbiddenException("Test unauthorized");
    }
  }

  @Override
  public boolean isAuthorizedForAnyParentOid(
      Set<String> organisaatioOids,
      Collection<? extends GrantedAuthority> userRoles,
      Collection<String> requiredRoles) {
    return true;
  }
}
