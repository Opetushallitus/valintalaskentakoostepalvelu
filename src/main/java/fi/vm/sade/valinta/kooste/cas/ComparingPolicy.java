package fi.vm.sade.valinta.kooste.cas;

public interface ComparingPolicy {
    String getTicketFromCacheExposed(String cacheKey);
    void putTicketToCacheExposed(String cacheKey, String ticket);
}
