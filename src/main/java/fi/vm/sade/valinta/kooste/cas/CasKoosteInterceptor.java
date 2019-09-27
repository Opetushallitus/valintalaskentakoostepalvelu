package fi.vm.sade.valinta.kooste.cas;

import fi.vm.sade.javautils.cas.ApplicationSession;
import fi.vm.sade.javautils.cas.SessionToken;
import fi.vm.sade.javautils.cxf.OphCxfMessageUtil;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse.CAS_302_REDIRECT_MARKER;

public class CasKoosteInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CasKoosteInterceptor.class);
    private static final String EXCHANGE_SESSION_TOKEN = "SessionToken";

    private final ApplicationSession applicationSession;
    private final boolean legacySpringFilter;

    public CasKoosteInterceptor(ApplicationSession applicationSession,
                                boolean legacySpringFilter) {
        super(Phase.PRE_PROTOCOL);
        this.applicationSession = applicationSession;
        this.legacySpringFilter = legacySpringFilter;
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        try {
            if ((Boolean) message.get(Message.INBOUND_MESSAGE)) {
                handleInboundMessage(message);
            } else {
                handleOutboundMessage(message);
            }
        } catch (Exception e) {
            LOGGER.error("handleMessage throws", e);
        }
    }

    private void handleInboundMessage(Message message) {
        if (isUnauthorized(message) || isRedirectToCas(message)) {
            SessionToken session = (SessionToken) message.getExchange().get(EXCHANGE_SESSION_TOKEN);
            LOGGER.info(String.format("Authentication failed using session %s", session));
            this.applicationSession.invalidateSession(session);
            OphCxfMessageUtil.addHeader(message, CAS_302_REDIRECT_MARKER.getKey(), CAS_302_REDIRECT_MARKER.getValue());
        }
    }

    private void handleOutboundMessage(Message message) throws ExecutionException, InterruptedException, TimeoutException {
        SessionToken session = this.applicationSession.getSessionToken().get(20, TimeUnit.SECONDS);
        message.getExchange().put(EXCHANGE_SESSION_TOKEN, session);
        LOGGER.debug(String.format("Using session %s", session));
        if (this.legacySpringFilter) {
            LOGGER.debug(String.format("Using header CasSecurityTicket=%s", session.serviceTicket.serviceTicket));
            OphCxfMessageUtil.addHeader(message, "CasSecurityTicket", session.serviceTicket.serviceTicket);
        }
        OphCxfMessageUtil.appendToHeader(message, "Cookie", session.cookie.getName() + "=" + session.cookie.getValue(), ";");
    }

    private boolean isRedirectToCas(Message message) {
        for (String location : OphCxfMessageUtil.getHeader(message, "Location")) {
            try {
                if (new URL(location).getPath().startsWith("/cas/login")) {
                    return true;
                }
            } catch(MalformedURLException ignored) { }
        }
        return false;
    }

    private boolean isUnauthorized(Message message) {
        return ((Integer) 401).equals(message.get(Message.RESPONSE_CODE));
    }
}
