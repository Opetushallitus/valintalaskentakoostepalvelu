package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.pistesyotto.service.HakukohdeOIDAuthorityCheck;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.function.Consumer;

@Service
public class MockAuthorityCheckService extends AuthorityCheckService {

    @Override
    public void getAuthorityCheckForRoles(Collection<String> roles, Consumer<HakukohdeOIDAuthorityCheck> callback, Consumer<Throwable> failureCallback) {
        callback.accept((OID) -> true);
    }
}
