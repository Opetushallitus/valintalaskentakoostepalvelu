package fi.vm.sade.valinta.kooste.security;

import static fi.vm.sade.valinta.kooste.util.SecurityUtil.containsOphRole;
import static fi.vm.sade.valinta.kooste.util.SecurityUtil.getAuthoritiesFromAuthenticationStartingWith;
import static fi.vm.sade.valinta.kooste.util.SecurityUtil.getRoles;
import static fi.vm.sade.valinta.kooste.util.SecurityUtil.isRootOrganizationOID;
import static fi.vm.sade.valinta.kooste.util.SecurityUtil.parseOrganizationGroupOidsFromSecurityRoles;
import static fi.vm.sade.valinta.kooste.util.SecurityUtil.parseOrganizationOidsFromSecurityRoles;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.google.common.collect.Sets;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultOrganization;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.service.HakukohdeOIDAuthorityCheck;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import javax.ws.rs.ForbiddenException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthorityCheckService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthorityCheckService.class);

    @Autowired
    private TarjontaAsyncResource tarjontaAsyncResource;
    @Autowired
    private OrganisaatioResource organisaatioResource;
    @Autowired
    private ValintaperusteetAsyncResource valintaperusteetAsyncResource;

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
                    Optional.of(organizationOids).filter(oids -> !oids.isEmpty()).map(tarjontaAsyncResource::hakukohdeSearchByOrganizationOids)
                            .orElse(Observable.just(Collections.emptyList()));

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

    public void checkAuthorizationForHaku(String hakuOid, Collection<String> requiredRoles) {
        Collection<? extends GrantedAuthority> userRoles = getRoles();

        if (containsOphRole(userRoles)) {
            // on OPH-käyttäjä, ei tarvitse käydä läpi organisaatioita
            return;
        }

        boolean isAuthorized = Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid)).map(haku -> {
            String[] organisaatioOids = haku.getTarjoajaOids();
            return isAuthorizedForAnyParentOid(organisaatioOids, userRoles, requiredRoles);
        }).timeout(2, MINUTES).blockingFirst();

        if (!isAuthorized) {
            String msg = String.format(
                    "Käyttäjällä ei oikeutta haun %s tarjoajaan tai sen yläorganisaatioihin.",
                    hakuOid
            );
            LOG.error(msg);
            throw new ForbiddenException(msg);
        }
    }

    public boolean isAuthorizedForAnyParentOid(String[] organisaatioOids, Collection<? extends GrantedAuthority> userRoles, Collection<String> requiredRoles) {
        try {
            for (String organisaatioOid : organisaatioOids) {
                String parentOidsPath = organisaatioResource.parentoids(organisaatioOid);
                String[] parentOids = parentOidsPath.split("/");

                for (String oid : parentOids) {
                    for (String role : requiredRoles) {
                        String organizationRole = role + "_" + oid;

                        for (GrantedAuthority auth : userRoles) {
                            if (organizationRole.equals(auth.getAuthority()))
                                return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            String msg = String.format("Organisaatioiden %s parentOids -haku epäonnistui", Arrays.toString(organisaatioOids));
            LOG.error(msg, e);
            throw new ForbiddenException(msg);
        }

        return false;
    }

    public void checkAuthorizationForValintaryhma(String valintaryhmaOid, List<String> requiredRoles) {
        Collection<? extends GrantedAuthority> userRoles = getRoles();

        boolean isOphUser = containsOphRole(userRoles);
        if (isOphUser) {
            return;
        }

        boolean isAuthorized = valintaperusteetAsyncResource.haeValintaryhmaVastuuorganisaatio(valintaryhmaOid).map((vastuuorganisaatioOid) -> {
            if (vastuuorganisaatioOid == null) {
                LOG.error("Valintaryhmän {} vastuuorganisaatio on null; vain OPH:lla oikeus valintaryhmään.", valintaryhmaOid);
                return false;
            } else {
                return isAuthorizedForAnyParentOid(new String[]{vastuuorganisaatioOid}, userRoles, requiredRoles);
            }
        }).timeout(2, MINUTES).blockingFirst();

        if (!isAuthorized) {
            String msg = String.format(
                    "Käyttäjällä ei oikeutta valintaryhmän %s vastuuorganisaatioon tai sen yläorganisaatioihin.",
                    valintaryhmaOid
            );
            LOG.error(msg);
            throw new ForbiddenException(msg);
        }
    }

    /**
     * Käyttöoikeustarkastelun konteksti
     *
     * Tämän avulla käyttöoikeustarkastelun voi siirtää käyttäjän
     * tunnistaneesta säikeestä toiseen säikeeseen.
     */
    public static class Context {
        protected final SecurityContext securityContext;

        protected Context(SecurityContext securityContext) {
            this.securityContext = securityContext;
        }

        protected SecurityContext getSecurityContext() {
            return securityContext;
        }
    }

    public Context getContext() {
        return new Context(SecurityContextHolder.getContext());
    }

    public void withContext(Context context, Runnable callback) {
        setContext(context);
        callback.run();
        clearContext();
    }

    private void setContext(Context context) {
        SecurityContextHolder.setContext(context.getSecurityContext());
    }

    private void clearContext() {
        SecurityContextHolder.clearContext();
    }
}
