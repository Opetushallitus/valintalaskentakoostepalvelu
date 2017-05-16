package fi.vm.sade.valinta.kooste.cas;

import fi.vm.sade.authentication.cas.DefaultTicketCachePolicy;
import fi.vm.sade.authentication.cas.TicketCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComparingTicketCachePolicy extends TicketCachePolicy {
    private static final Logger LOG = LoggerFactory.getLogger(ComparingTicketCachePolicy.class);
    private final ComparingPolicy primaryCachePolicy;
    private final ComparingPolicy secondaryCachePolicy = new ComparingDefaultTicketCachePolicy();

    public ComparingTicketCachePolicy(ComparingPolicy primaryCachePolicy) {
        this.primaryCachePolicy = primaryCachePolicy;
    }

    @Override
    protected String getTicketFromCache(String cacheKey) {
        String secondaryTicket = secondaryCachePolicy.getTicketFromCacheExposed(cacheKey);
        String primaryTicket = primaryCachePolicy.getTicketFromCacheExposed(cacheKey);
        if(secondaryTicket == null) {
            if(primaryTicket != null) {
                LOG.error("Tickets didn't match: {} != {}" ,secondaryTicket , primaryTicket);
            }
        } else {
            if(!secondaryTicket.equals(primaryTicket)) {
                LOG.error("Tickets didn't match: {} != {}" ,secondaryTicket , primaryTicket);
            }
        }
        return primaryTicket;
    }

    @Override
    protected void putTicketToCache(String cacheKey, String ticket) {
        secondaryCachePolicy.putTicketToCacheExposed(cacheKey,ticket);
        primaryCachePolicy.putTicketToCacheExposed(cacheKey, ticket);
    }
}
