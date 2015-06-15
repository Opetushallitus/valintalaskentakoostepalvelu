package fi.vm.sade.valinta.kooste;

import java.net.HttpURLConnection;
import java.util.Collection;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.gson.GsonBuilder;

/**
 * @Deprecated Ainoastaan Cache-ongelmien poistotarkoituksiin.
 */
@Deprecated
public class AbstractSecurityTicketOutInterceptor<T extends Message> extends AbstractPhaseInterceptor<T> {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractSecurityTicketOutInterceptor.class);

    @Value("${auth.mode:cas}")
    private String authMode;

    private ProxyAuthenticator proxyAuthenticator = new ProxyAuthenticator();

    static class ProxyAuthenticator {
        public void proxyAuthenticate(String casTargetService, String authMode, Callback callback) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            try {
                if (authentication != null && "dev".equals(authMode)) {
                    proxyAuthenticateDev(callback, authentication);
                } else {
                    proxyAuthenticateCas(casTargetService, callback, authentication);
                }
            } catch (Throwable e) {
                throw new RuntimeException(
                        "Could not attach security ticket to SOAP message, user: "
                                + (authentication != null ? authentication.getName()
                                : "null") + ", authmode: " + authMode
                                + ", exception: " + e, e);
            }
        }

        protected void proxyAuthenticateCas(String casTargetService, Callback callback, Authentication authentication) {
            String proxyTicket = getCachedProxyTicket(casTargetService, authentication, callback);
            if (proxyTicket == null) {
                throw new BadCredentialsException("got null proxyticket, cannot attach to request, casTargetService: "
                        + casTargetService + ", authentication: " + authentication);
            } else {
                callback.setRequestHeader("CasSecurityTicket", proxyTicket);
                PERA.setProxyKayttajaHeaders(callback, authentication.getName());
                LOG.debug("attached proxyticket to request! user: " + authentication.getName() + ", ticket: " + proxyTicket);
            }
        }

        static class PERA {
            public static final String X_KUTSUKETJU_ALOITTAJA_KAYTTAJA_TUNNUS = "X-Kutsuketju.Aloittaja.KayttajaTunnus";
            public static final String X_PALVELUKUTSU_LAHETTAJA_KAYTTAJA_TUNNUS = "X-Palvelukutsu.Lahettaja.KayttajaTunnus";
            public static final String X_PALVELUKUTSU_LAHETTAJA_PROXY_AUTH = "X-Palvelukutsu.Lahettaja.ProxyAuth";

            public static void setKayttajaHeaders(HttpRequestBase req, String currentUser, String callAsUser) {
                req.setHeader(X_KUTSUKETJU_ALOITTAJA_KAYTTAJA_TUNNUS, currentUser);
                req.setHeader(X_PALVELUKUTSU_LAHETTAJA_KAYTTAJA_TUNNUS, callAsUser);
            }

            public static void setProxyKayttajaHeaders(ProxyAuthenticator.Callback callback, String currentUser) {
                callback.setRequestHeader(X_KUTSUKETJU_ALOITTAJA_KAYTTAJA_TUNNUS, currentUser);
                callback.setRequestHeader(X_PALVELUKUTSU_LAHETTAJA_KAYTTAJA_TUNNUS, currentUser);
                callback.setRequestHeader(X_PALVELUKUTSU_LAHETTAJA_PROXY_AUTH, "true");
            }
        }

        protected void proxyAuthenticateDev(Callback callback, Authentication authentication) {
            callback.setRequestHeader("CasSecurityTicket", "oldDeprecatedSecurity_REMOVE");
            String user = authentication.getName();
            String authorities = toString(authentication.getAuthorities());
            callback.setRequestHeader("oldDeprecatedSecurity_REMOVE_username", user);
            callback.setRequestHeader("oldDeprecatedSecurity_REMOVE_authorities", authorities);
            LOG.debug("DEV Proxy ticket! user: " + user + ", authorities: " + authorities);
        }

        public String getCachedProxyTicket(final String targetService, final Authentication authentication, final Callback callback) {
            String proxyTicket = obtainNewCasProxyTicket(targetService, authentication);
            if (callback != null) {
                callback.gotNewTicket(authentication, proxyTicket);
            }
            return proxyTicket;
        }

        public void clearTicket(String casTargetService) {
        }

        protected String obtainNewCasProxyTicket(String casTargetService, Authentication authentication) {
            if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
                throw new RuntimeException("current user is not authenticated");
            }
            String ticket = ((CasAuthenticationToken) authentication).getAssertion().getPrincipal().getProxyTicketFor(casTargetService);
            if (ticket == null) {
                throw new NullPointerException("obtainNewCasProxyTicket got null proxyticket, there must be something wrong with cas proxy authentication -scenario! check proxy callback works etc, targetService: "
                        + casTargetService + ", user: " + authentication.getName());
            }
            return ticket;
        }

        private String toString(Collection<? extends GrantedAuthority> authorities) {
            StringBuilder sb = new StringBuilder();
            for (GrantedAuthority authority : authorities) {
                sb.append(authority.getAuthority()).append(",");
            }
            return sb.toString();
        }

        public static interface Callback {
            void setRequestHeader(String key, String value);

            void gotNewTicket(Authentication authentication, String proxyTicket);
        }

    }

    public AbstractSecurityTicketOutInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }

    @Override
    public void handleMessage(final T message) throws Fault {
        final String casTargetService = getCasTargetService((String) message.get(Message.ENDPOINT_ADDRESS));
        proxyAuthenticator.proxyAuthenticate(casTargetService, authMode,
                new ProxyAuthenticator.Callback() {
                    @Override
                    public void setRequestHeader(String key, String value) {
                        LOG.info("setRequestHeader: " + key + "=" + value + " (targetService: " + casTargetService + ")");
                        ((HttpURLConnection) message.get("http.connection")).setRequestProperty(key, value);
                    }

                    @Override
                    public void gotNewTicket(Authentication authentication, String proxyTicket) {
                        LOG.info("gotNewTicket, auth: " + authentication.getName() + ", proxyTicket: "
                                + proxyTicket + ", (targetService: " + casTargetService + ")");
                    }
                });
    }

    @Override
    public void handleFault(Message message) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CasAuthenticationToken) {
            String casTargetService = getCasTargetService((String) message.get(Message.ENDPOINT_ADDRESS));
            String msgProxyTicket = ((HttpURLConnection) message.get("http.connection")).getRequestProperty("CasSecurityTicket");
            LOG.error("FAULT in request, targetService: " + casTargetService
                    + ", authentication: " + authentication.getName()
                    + ", msgProxyTicket: " + msgProxyTicket);
        }

        LOG.error("FAULT in request, message: " + message);
        try {
            LOG.error("\r\n{}", new GsonBuilder().setPrettyPrinting().create().toJson(message));
        } catch (Exception e) {
            LOG.error("Prettyprinting error message failed! {}", e.getMessage());
        }
    }

    /**
     * Get cas service from url string, get string before 4th '/' char. For
     * example:
     * <p/>
     * https://asd.asd.asd:8080/backend-service/asd/qwe/qwe2.foo?bar=asd --->
     * https://asd.asd.asd:8080/backend-service
     */
    private static String getCasTargetService(String url) {
        return url.replaceAll("(.*?//.*?/.*?)/.*", "$1") + "/j_spring_cas_security_check";
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public void setProxyAuthenticator(ProxyAuthenticator proxyAuthenticator) {
        this.proxyAuthenticator = proxyAuthenticator;
    }
}