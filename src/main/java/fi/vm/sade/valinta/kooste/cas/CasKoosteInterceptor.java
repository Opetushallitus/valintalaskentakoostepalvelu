package fi.vm.sade.valinta.kooste.cas;

import com.google.common.cache.Cache;
import fi.vm.sade.authentication.cas.CasClient;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class CasKoosteInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final long ON_NEW_TICKET_WAIT_AT_LEAST = TimeUnit.SECONDS.toMillis(1L);
    private static final long NEVER_INVALIDATE_TICKET_LESS_THAN = TimeUnit.SECONDS.toMillis(5L);

    private static final Logger LOGGER = LoggerFactory.getLogger(CasKoosteInterceptor.class);
    private static class Ticket {
        public final Date created = new Date();
        public final String ticket;
        public final AtomicInteger counter = new AtomicInteger(0);
        public Ticket(String ticket) {
            this.ticket = ticket;
        }
        public boolean isOlderThanMilliseconds(long milliseconds) {
            final Date millisecondsAgo = new Date(System.currentTimeMillis() - milliseconds);
            return created.before(millisecondsAgo);
        }
    }

    private static final Integer HTTP_401_UNAUTHORIZED = Integer.valueOf(401);
    private final AtomicReference<Ticket> ticket = new AtomicReference<>();

    private final String webCasUrl;
    private final String targetService;
    private final String appClientUsername;
    private final String appClientPassword;

    public CasKoosteInterceptor(String webCasUrl, String targetService, String appClientUsername, String appClientPassword) {
        super(Phase.PRE_PROTOCOL);
        this.webCasUrl = webCasUrl;
        this.targetService = targetService;
        this.appClientUsername = appClientUsername;
        this.appClientPassword = appClientPassword;
    }
    private Ticket updateTicket(Ticket oldTicket) {
        if(oldTicket == null) {
            String ticket = CasClient.getTicket(webCasUrl, appClientUsername, appClientPassword, targetService);
            LOGGER.debug("Expensive fetch ticket happened: {}", ticket);
            return new Ticket(ticket);
        } else {
            return oldTicket;
        }
    }

    private Optional<String> tryToGetOutMessageSecurityTicket(Message message) {
        try {
            return Optional.ofNullable(((HttpURLConnection)((MessageImpl) message).getExchange().getOutMessage().get("http.connection")).getRequestProperty("CasSecurityTicket"));
        }catch (Exception e) {
            LOGGER.error("Couldn't get ticket used in out message: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    private void invalidateOldTicket(String oldTicket) {
        ticket.updateAndGet(currentTicket -> {
            if(currentTicket == null) {
                LOGGER.error("Current ticket can't be null because old ticket was " + oldTicket + "!");
                throw new RuntimeException("Current ticket can't be null because old ticket was " + oldTicket + "!");
            } else {
                final boolean currentTicketIsTheOneThatFailed = currentTicket.ticket.equals(oldTicket);
                final boolean isSoonEnoughToInvalidate = currentTicket.isOlderThanMilliseconds(NEVER_INVALIDATE_TICKET_LESS_THAN);
                if(currentTicketIsTheOneThatFailed && isSoonEnoughToInvalidate) {
                    LOGGER.debug("Invalidating ticket: {}", oldTicket);
                    return null; // invalidate
                } else {
                    LOGGER.debug("Didn't invalidate ticket({}) because currentTicketIsTheOneThatFailed({}) and isSoonEnoughToInvalidate({})",
                            oldTicket, currentTicketIsTheOneThatFailed, isSoonEnoughToInvalidate);
                    return currentTicket;
                }
            }
        });
    }
    private boolean isRedirectToCas(Message message) {
        Map<String, List<String>> headers = (Map<String, List<String>>)message.get(Message.PROTOCOL_HEADERS);
        List<String> locationHeader = headers.get("Location");
        if (locationHeader != null && locationHeader.size() > 0) {
            String location = locationHeader.get(0);
            try {
                URL url = new URL(location);
                String path = url.getPath();
                // We are only interested in CAS redirects
                if(path.startsWith("/cas/login")) {
                    return true;
                }
            } catch(Exception ex) {
            }
        }
        return false;
    }
    @Override
    public void handleMessage(Message message) throws Fault {
        boolean inbound = (Boolean) message.get(Message.INBOUND_MESSAGE);
        if (inbound) {
            final boolean isUnauthorized = HTTP_401_UNAUTHORIZED.equals(message.get(Message.RESPONSE_CODE));

            if (isUnauthorized || isRedirectToCas(message)) {
                tryToGetOutMessageSecurityTicket(message).ifPresent(this::invalidateOldTicket);
            }
        }
        else {
            final Ticket currentTicket = ticket.updateAndGet(this::updateTicket);
            waitIfNotFirstUsageAndTicketNotOlderThanSecond(currentTicket);
            HttpURLConnection httpConnection = (HttpURLConnection) message.get("http.connection");
            httpConnection.setRequestProperty("CasSecurityTicket", currentTicket.ticket);
        }
    }

    private void waitIfNotFirstUsageAndTicketNotOlderThanSecond(Ticket currentTicket) {
        final boolean isNotFirstUsage = currentTicket.counter.getAndIncrement() != 0;
        if(isNotFirstUsage) {
            while (!currentTicket.isOlderThanMilliseconds(ON_NEW_TICKET_WAIT_AT_LEAST)) {
                try {
                    Thread.sleep(100);
                } catch (Exception ignored) {}
            }
        }
    }
}
