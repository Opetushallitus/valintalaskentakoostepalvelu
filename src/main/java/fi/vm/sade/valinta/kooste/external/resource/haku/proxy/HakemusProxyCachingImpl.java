package fi.vm.sade.valinta.kooste.external.resource.haku.proxy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class HakemusProxyCachingImpl implements
        HakemusProxy {

    private static final Logger LOG = LoggerFactory
            .getLogger(HakemusProxyCachingImpl.class);

    @Autowired
    private ApplicationResource applicationResource;

    private Cache<String, Hakemus> hakemusCache;

    @Value("${valintakoostepalvelu.cache.hakemus.size:150000}")
    private int cacheSize;

    @PostConstruct
    public void init() {
        hakemusCache = CacheBuilder.newBuilder().recordStats()
                .maximumSize(cacheSize).build();
    }

    @Override
    public Hakemus haeHakemus(
            final String hakemusOid) throws ExecutionException {



        Hakemus hakemus = hakemusCache.get(hakemusOid,
                new Callable<Hakemus>() {
                    @Override
                    public Hakemus call()
                            throws Exception {
                        try {

                            return applicationResource
                                    .getApplicationByOid(hakemusOid);
                        } catch (Exception e) {
                            LOG.error(
                                    "HAKEMUSCACHE EPAONNISTUI[{}]: {}",
                                    new Object[]{hakemusOid, e.getMessage()});
                            e.printStackTrace();
                            throw e;
                        }
                    }
                });

        if(LOG.isDebugEnabled()) {
            LOG.debug(hakemusCache.stats().toString());
        }

        return hakemus;

    }

}
