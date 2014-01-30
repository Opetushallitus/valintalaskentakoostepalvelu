package fi.vm.sade.valinta.kooste;

import org.apache.cxf.message.Message;
import org.springframework.security.core.Authentication;

import fi.vm.sade.authentication.cas.TicketCachePolicy;

public class NoTicketCachePolicy implements TicketCachePolicy {

    public NoTicketCachePolicy() {
    }

    @Override
    public String getTicketFromCache(Message message, String targetService, Authentication auth) {
        return null;
    }

    @Override
    public void putTicketToCache(Message message, String targetService, Authentication auth, String ticket) {
    }

}