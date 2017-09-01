package fi.vm.sade.valinta.kooste.cas;

import fi.vm.sade.authentication.cas.CasClient;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class CasKoosteInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CasKoosteInterceptor.class);
    private static final Integer HTTP_401_UNAUTHORIZED = Integer.valueOf(401);
    public static final String JSESSIONID = "JSESSIONID";

    private final String webCasUrl;
    private final String targetService;
    private final String appClientUsername;
    private final String appClientPassword;

    private AtomicReference<CompletableFuture<String>> sessionCookiePromise;

    public CasKoosteInterceptor(String webCasUrl, String targetService, String appClientUsername, String appClientPassword) {
        super(Phase.PRE_PROTOCOL);
        this.webCasUrl = webCasUrl;
        this.targetService = targetService;
        this.appClientUsername = appClientUsername;
        this.appClientPassword = appClientPassword;
        this.sessionCookiePromise = new AtomicReference<>(CompletableFuture.completedFuture(null));
    }

    private boolean isRedirectToCas(Message message) {
        Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        if (!headers.containsKey("Location")) {
            return false;
        }
        for (String location : headers.get("Location")) {
            try {
                if (new URL(location).getPath().startsWith("/cas/login")) {
                    return true;
                }
            } catch(MalformedURLException e) {
            }
        }
        return false;
    }

    private String getResponseCookie(Message message, String cookieName) {
        Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        if (!headers.containsKey("Set-Cookie")) {
            return null;
        }
        for (String cookie : headers.get("Set-Cookie")) {
            if (cookie.startsWith(cookieName)) {
                return cookie.split(";")[0].split("=")[1];
            }
        }
        return null;
    }

    private String getRequestCookie(Message message, String cookieName) {
        Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        if (!headers.containsKey("Cookie")) {
            return null;
        }
        for (String cookie : headers.get("Cookie")) {
            if (cookie.startsWith(cookieName)) {
                return cookie.split(";")[0].split("=")[1];
            }
        }
        return null;
    }

    private void addCookie(Message message, String cookieName, String cookieValue) {
        Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        if (!headers.containsKey("Cookie")) {
            headers.put("Cookie", new ArrayList<>());
        }
        headers.get("Cookie").add(cookieName + "=" + cookieValue);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        try {
            boolean inbound = (Boolean) message.get(Message.INBOUND_MESSAGE);
            if (inbound) {
                boolean isUnauthorized = HTTP_401_UNAUTHORIZED.equals(message.get(Message.RESPONSE_CODE));
                if (isUnauthorized || isRedirectToCas(message)) {
                    Message request = message.getExchange().getOutMessage();
                    String session = getRequestCookie(request, JSESSIONID);
                    if (session == null) {
                        String serviceTicket = ((HttpURLConnection) request.get("http.connection")).getRequestProperty("CasSecurityTicket");
                        LOGGER.warn(String.format("Authentication to %s failed using service ticket %s", this.targetService, serviceTicket));
                    } else {
                        LOGGER.info(String.format("Authentication to %s failed using session %s", this.targetService, session));
                        this.sessionCookiePromise.updateAndGet(currentSessionPromise -> {
                            if (session.equals(currentSessionPromise.getNow(null))) {
                                return CompletableFuture.completedFuture(null);
                            } else {
                                return currentSessionPromise;
                            }
                        });
                    }
                } else {
                    String session = getResponseCookie(message, JSESSIONID);
                    if (session != null && this.sessionCookiePromise.get().complete(session)) {
                        LOGGER.info(String.format("New session %s for %s", session, this.targetService));
                    }
                }
            } else {
                CompletableFuture<String> p = this.sessionCookiePromise.get();
                String session;
                try {
                    session = p.get(10, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    LOGGER.warn("Fetching a session has taken over 10 seconds");
                    session = null;
                }
                if (session == null) {
                    if (this.sessionCookiePromise.compareAndSet(p, new CompletableFuture<>())) {
                        LOGGER.info(String.format("Fetching a new CAS service ticket for %s", this.targetService));
                        String serviceTicket = CasClient.getTicket(webCasUrl, appClientUsername, appClientPassword, targetService);
                        LOGGER.info(String.format("Got a service ticket %s for %s", serviceTicket, this.targetService));
                        ((HttpURLConnection) message.get("http.connection")).setRequestProperty("CasSecurityTicket", serviceTicket);
                    } else {
                        // TODO retry
                    }
                } else {
                    addCookie(message, JSESSIONID, session);
                }
            }
        } catch (Exception e) {
            LOGGER.error("handleMessage throws", e);
        }
    }
}
