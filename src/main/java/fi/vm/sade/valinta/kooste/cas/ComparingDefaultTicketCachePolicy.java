package fi.vm.sade.valinta.kooste.cas;

import fi.vm.sade.authentication.cas.DefaultTicketCachePolicy;

public class ComparingDefaultTicketCachePolicy extends DefaultTicketCachePolicy implements ComparingPolicy {

    @Override
    public String getTicketFromCacheExposed(String cacheKey) {
        return super.getTicketFromCache(cacheKey);
    }
    @Override
    public void putTicketToCacheExposed(String targetService, String user) {
        super.putTicketToCache(targetService, user);
    }
}
