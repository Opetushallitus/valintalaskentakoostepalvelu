package fi.vm.sade.valinta.kooste;

import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * TODO maybe move to java-cas or some other suitable place on a sunnier day.
 */
public class AuthorizationUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationUtil.class);

    public static AuditSession createAuditSession(HttpServletRequest httpServletRequestJaxRS) {
        return createAuditSession(false, httpServletRequestJaxRS);
    }

    private static HttpServletRequest request(HttpServletRequest httpServletRequestJaxRS) {
        if(null != httpServletRequestJaxRS) {
            //Käytetään unit-testeissä
            return httpServletRequestJaxRS;
        }
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        if(null != attributes) {
            if(attributes instanceof ServletRequestAttributes) {
                return ((ServletRequestAttributes)attributes).getRequest();
            } else {
                throw new IllegalStateException("Ei löydetty validia HTTP requestia.");
            }
        }
        throw new InternalError("Ei löydetty HTTP requestia.");
    }

    public static AuditSession createAuditSession(boolean isUnmodifiedSinceMandatory, HttpServletRequest httpServletRequestJaxRS) {
        HttpServletRequest httpServletRequest = request(httpServletRequestJaxRS);
        AuditSession session = new AuditSession();
        session.setSessionId(httpServletRequest.getSession().getId());
        session.setUid(KoosteAudit.uid().orElse("Unknown user"));
        session.setPersonOid(KoosteAudit.username());
        session.setInetAddress(resolveClientIpAddress(httpServletRequest));
        session.setUserAgent(Optional.ofNullable(httpServletRequest.getHeader("User-Agent")).orElse("Unknown user agent"));
        session.setIfUnmodifiedSince(readIfUnmodifiedSince(isUnmodifiedSinceMandatory, httpServletRequestJaxRS));
        session.setRoles(getRoles());
        return session;
    }

    private static String resolveClientIpAddress(HttpServletRequest httpServletRequest) {
        String xRealIp = httpServletRequest.getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(xRealIp)) {
            return xRealIp;
        }
        String xForwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(xForwardedFor)) {
            if (xForwardedFor.contains(",")) {
                LOG.error("Ei löytynyt X-Real-IP -headeria, mutta X-Forwarded-For-headerissa on useampi arvo: " + xForwardedFor +
                    " . Tästä voi aiheutua ongelmia.");
            }
            return xForwardedFor;
        }
        LOG.warn("X-Real-IP or X-Forwarded-For was not set. Are we not running behind a load balancer?");
        return httpServletRequest.getRemoteAddr();
    }

    private static Optional<String> readIfUnmodifiedSince(boolean isUnmodifiedSinceMandatory, HttpServletRequest httpServletRequestJaxRS) {
        Optional<String> isUnmodifiedSinceHeader = Optional.ofNullable(request(httpServletRequestJaxRS).getHeader("If-Unmodified-Since"));
        if(isUnmodifiedSinceMandatory && !isUnmodifiedSinceHeader.isPresent()) {
            throw new IllegalArgumentException("If-Unmodified-Since on pakollinen otsake.");
        } else if(isUnmodifiedSinceMandatory) {
            try {
                DateTimeFormatter.RFC_1123_DATE_TIME.parse(isUnmodifiedSinceHeader.get());
            } catch (Exception e) {
                throw new IllegalArgumentException("Otsake If-Unmodified-Since on väärässä formaatissa: " + isUnmodifiedSinceHeader.get());
            }
        }
        return isUnmodifiedSinceHeader;
    }

    private static List<String> getRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(null == authentication) {
            return new ArrayList<>();
        }
        return authentication.getAuthorities().stream().map(a -> ((GrantedAuthority)a).getAuthority()).map(r -> r.replace("ROLE_", "")).collect(Collectors.toList());
    }

    public static String getCurrentUser() {
        SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx == null) {
            return null;
        }
        Authentication authentication = ctx.getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getName();
    }
}
