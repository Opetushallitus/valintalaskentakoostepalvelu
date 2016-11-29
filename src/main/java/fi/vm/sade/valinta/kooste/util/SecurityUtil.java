package fi.vm.sade.valinta.kooste.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SecurityUtil {
    public static final String ROOTOID = "1.2.246.562.10.00000000001";
    private static final String ORGANIZATION_OID_PREFIX = "1.2.246.562.10";
    private static final String ORGANIZATION_GROUP_OID_PREFIX = "1.2.246.562.28";
    private static final Logger LOG = LoggerFactory.getLogger(SecurityUtil.class);

    public static Collection<String> getAuthoritiesFromAuthenticationStartingWith(Collection<String> prefixes) {
        return getAuthoritiesFromAuthentication().stream().filter(auth -> prefixes.stream().anyMatch(prefix -> auth.startsWith(prefix))).collect(Collectors.toSet());
    }

    public static boolean isRootOrganizationOID(String organizationOID) {
        return ROOTOID.equals(organizationOID);
    }

    public static Collection<String> getAuthoritiesFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication instanceof CasAuthenticationToken) {
            CasAuthenticationToken casAuthenticationToken = (CasAuthenticationToken)authentication;
            return casAuthenticationToken.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList());
        } else {
            LOG.error("Tried to get authorities from spring authentication token but token wasn't CAS authentication token");
            return Collections.emptyList();
        }
    }

    public static Optional<String> parseOrganizationOidFromSecurityRole(String role) {
        String[] pieces = StringUtils.trimToEmpty(role).split("_");
        if(pieces.length > 0) {
            String lastPiece = pieces[pieces.length - 1];
            if(lastPiece.startsWith(ORGANIZATION_OID_PREFIX)) {
                return Optional.of(lastPiece);
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }
    public static Optional<String> parseOrganizationGroupOidFromSecurityRole(String role) {
        String[] pieces = StringUtils.trimToEmpty(role).split("_");
        if(pieces.length > 0) {
            String lastPiece = pieces[pieces.length - 1];
            if(lastPiece.startsWith(ORGANIZATION_GROUP_OID_PREFIX)) {
                return Optional.of(lastPiece);
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }
    public static Set<String> parseOrganizationOidsFromSecurityRoles(Collection<String> roles) {
        return roles.stream().flatMap(r -> parseOrganizationOidFromSecurityRole(r).map(org -> Stream.of(org)).orElse(Stream.empty())).collect(Collectors.toSet());
    }
    public static Set<String> parseOrganizationGroupOidsFromSecurityRoles(Collection<String> roles) {
        return roles.stream().flatMap(r -> parseOrganizationGroupOidFromSecurityRole(r).map(org -> Stream.of(org)).orElse(Stream.empty())).collect(Collectors.toSet());
    }
}
