package fi.vm.sade.valinta.kooste.security;

import com.google.common.collect.Sets;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultOrganization;
import fi.vm.sade.valinta.kooste.pistesyotto.service.HakukohdeOIDAuthorityCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static fi.vm.sade.valinta.kooste.util.SecurityUtil.*;

@Service
public class AuthorityCheckService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthorityCheckService.class);

    @Autowired
    private TarjontaAsyncResource tarjontaAsyncResource;

    public Observable<HakukohdeOIDAuthorityCheck> getAuthorityCheckForRoles(Collection<String> roles) {
        final Collection<String> authorities = getAuthoritiesFromAuthenticationStartingWith(roles);
        final Set<String> organizationOids = parseOrganizationOidsFromSecurityRoles(authorities);
        boolean isRootAuthority = organizationOids.stream().anyMatch(oid -> isRootOrganizationOID(oid));
        if(isRootAuthority) {
            return Observable.just((oid) -> true);
        } else {
            final Set<String> organizationGroupOids = parseOrganizationGroupOidsFromSecurityRoles(authorities);
            if(organizationGroupOids.isEmpty() && organizationOids.isEmpty()) {
                return Observable.error(new RuntimeException("Unauthorized. User has no organization OIDS"));
            }
            Observable<List<ResultOrganization>> searchByOrganizationOids =
                    Optional.of(organizationOids).filter(oids -> !oids.isEmpty()).map(tarjontaAsyncResource::hakukohdeSearchByOrganizationOids).orElse(Observable.just(Collections.emptyList()));

            Observable<List<ResultOrganization>> searchByOrganizationGroupOids =
                    Optional.of(organizationGroupOids).filter(oids -> !oids.isEmpty()).map(tarjontaAsyncResource::hakukohdeSearchByOrganizationGroupOids)
                            .orElse(Observable.just(Collections.emptyList()));

            return Observable.combineLatest(searchByOrganizationOids, searchByOrganizationGroupOids, (orgs, groupOrgs) -> {
                Set<String> hakukohdeOidSet1 = orgs.stream().flatMap(o -> o.getTulokset().stream()).map(ResultHakukohde::getOid).collect(Collectors.toSet());
                Set<String> hakukohdeOidSet2 = groupOrgs.stream().flatMap(o -> o.getTulokset().stream()).map(ResultHakukohde::getOid).collect(Collectors.toSet());

                return (oid) -> Sets.union(hakukohdeOidSet1, hakukohdeOidSet2).contains(oid);
            });
        }
    }
}
