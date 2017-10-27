package fi.vm.sade.valinta.kooste;

import fi.vm.sade.auditlog.ApplicationType;
import fi.vm.sade.auditlog.Audit;
import fi.vm.sade.security.SadeUserDetailsWrapper;
import fi.vm.sade.sharedutils.AuditLogger;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author Jussi Jartamo
 */
public class KoosteAudit {
    public static final Audit AUDIT = new Audit(new AuditLogger(), "valintalaskentakoostepalvelu", ApplicationType.VIRKAILIJA);

    public static String username() {
        return Optional.ofNullable((Principal) SecurityContextHolder.getContext().getAuthentication()).orElse(
                () -> "Kirjautumaton käyttäjä"
        ).getName();
    }

    public static Optional<String> uid() {
        Optional<String> uid = uidFromCasAuthenticationToken();
        if(!uid.isPresent()) {
            uid = uidFromLdapUserDetails();
        }
        return uid;
    }

    private static Optional<String> uidFromCasAuthenticationToken() {
        if(SecurityContextHolder.getContext().getAuthentication() instanceof CasAuthenticationToken) {
            CasAuthenticationToken casAuthenticationToken = (CasAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
            if(null != casAuthenticationToken.getAssertion() && null != casAuthenticationToken.getAssertion().getPrincipal()) {
                return Optional.ofNullable(casAuthenticationToken.getAssertion().getPrincipal().getName());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> uidFromLdapUserDetails() {
        if(null != SecurityContextHolder.getContext().getAuthentication()) {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if(principal instanceof SadeUserDetailsWrapper && ((SadeUserDetailsWrapper)principal).getDetails() instanceof LdapUserDetails) {
                return parseUidFromDn(((LdapUserDetails)((SadeUserDetailsWrapper)principal).getDetails()).getDn());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> parseUidFromDn(String dn) {
        return StringUtils.isEmpty(dn) ? Optional.empty() :
            Arrays.stream(StringUtils.split(dn, ",")).filter(s -> s.startsWith("uid")).findFirst().map(s ->
                    StringUtils.split(s, "=")).map(uid -> (2 == uid.length && !StringUtils.isEmpty(uid[1])) ? uid[1].trim() : null);
    }
}
