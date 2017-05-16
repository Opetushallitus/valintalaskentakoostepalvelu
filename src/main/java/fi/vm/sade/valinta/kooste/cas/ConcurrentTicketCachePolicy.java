package fi.vm.sade.valinta.kooste.cas;

import fi.vm.sade.authentication.cas.TicketCachePolicy;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentTicketCachePolicy extends TicketCachePolicy implements ComparingPolicy {

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    @Override
    protected String getTicketFromCache(String cacheKey) {
        return cache.get(cacheKey);
    }

    @Override
    protected void putTicketToCache(String cacheKey, String ticket) {
        cache.compute(cacheKey, (a,b) -> ticket);
    }

    @Override
    public String getTicketFromCacheExposed(String cacheKey) {
        return getTicketFromCache(cacheKey);
    }

    @Override
    public void putTicketToCacheExposed(String cacheKey, String ticket) {
        putTicketToCache(cacheKey, ticket);
    }
}
