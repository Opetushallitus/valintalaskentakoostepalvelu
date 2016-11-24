package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.pistesyotto.service.HakukohdeOIDAuthorityCheck;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.Collection;
import java.util.function.Consumer;

@Service
public class MockAuthorityCheckService extends AuthorityCheckService {

    @Override
    public Observable<HakukohdeOIDAuthorityCheck> getAuthorityCheckForRoles(Collection<String> roles) {
        return Observable.just((oid) -> true);
    }
}
