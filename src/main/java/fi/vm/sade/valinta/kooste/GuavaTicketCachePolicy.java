package fi.vm.sade.valinta.kooste;

import java.util.concurrent.TimeUnit;

import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import fi.vm.sade.authentication.cas.TicketCachePolicy;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
// @Component("guavaTicketCachePolicy")
public class GuavaTicketCachePolicy implements TicketCachePolicy {

    private final Cache<String, String> ticketCache;

    public GuavaTicketCachePolicy(
            @Value("${valintalaskentakoostepalvelu.guavaTicketCachePolicy.cacheExpirationTimeoutMinutes:10}") Long cacheExpirationTimeoutMinutes) {
        this.ticketCache = CacheBuilder.newBuilder().expireAfterWrite(cacheExpirationTimeoutMinutes, TimeUnit.MINUTES)
                .build();

    }

    @Override
    public String getTicketFromCache(Message message, String targetService, Authentication auth) {
        return ticketCache.getIfPresent(cacheKey(auth, targetService));
    }

    @Override
    public void putTicketToCache(Message message, String targetService, Authentication auth, String ticket) {
        ticketCache.put(cacheKey(auth, targetService), ticket);
    }

    private String cacheKey(Authentication auth, String targetService) {
        return auth.hashCode() + "_" + targetService;
    }
}
