package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.pistesyotto.service.HakukohdeOIDAuthorityCheck;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.Collection;

@Service
public class MockAuthorityCheckService extends AuthorityCheckService {

    @Override
    public Observable<HakukohdeOIDAuthorityCheck> getAuthorityCheckForRoles(Collection<String> roles) {
        return Observable.just((oid) -> true);
    }

    @Override
    public void checkAuthorizationForHaku(String hakuOid, Collection<String> requiredRoles) { }

    @Override
    public boolean isAuthorizedForAnyParentOid(String[] organisaatioOids, Collection<? extends GrantedAuthority> userRoles, Collection<String> requiredRoles) {
        return true;
    }
}
