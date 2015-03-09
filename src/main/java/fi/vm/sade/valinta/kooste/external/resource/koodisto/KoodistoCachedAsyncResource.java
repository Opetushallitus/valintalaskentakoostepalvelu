package fi.vm.sade.valinta.kooste.external.resource.koodisto;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Jussi Jartamo
 *
 * Wraps KoodistoAsyncResource with 1hour cache
 */
@Service
public class KoodistoCachedAsyncResource {

    private final Logger LOG = LoggerFactory.getLogger(KoodistoCachedAsyncResource.class);
    private final KoodistoAsyncResource koodistoAsyncResource;
    private final Cache<String, Map<String,Koodi>> koodistoCache =
            CacheBuilder.newBuilder().expireAfterAccess(
                    7, TimeUnit.HOURS
            ).build();

    @Autowired
    public KoodistoCachedAsyncResource(KoodistoAsyncResource koodistoAsyncResource) {
        this.koodistoAsyncResource = koodistoAsyncResource;
    }

    public Map<String,Koodi> haeKoodisto(String koodistoUri) {
        Map<String,Koodi> koodisto =
        koodistoCache.getIfPresent(koodistoUri);
        if(koodisto == null) {
            try {
                return koodistoCache.get(koodistoUri, () -> {
                    try {
                        List<Koodi> koodit = koodistoAsyncResource.haeKoodisto(koodistoUri).get();
                        Map<String, Koodi> koodistoMappaus = koodit.stream().collect(Collectors.toMap(a -> a.getKoodiArvo(), a -> a,
                                // Mergefunktiossa suuremmalla versiolla oleva palautetaan
                                (a,b) -> {
                                    if(a.getVersio() > b.getVersio()) {
                                        return a;
                                    }
                                    return b;
                                }));
                        return koodistoMappaus;
                    } catch (Exception e) {
                        LOG.error("Koodistosta luku epäonnistui: {} {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
                        throw new RuntimeException(e);
                    }
                });
            } catch(Exception e) {
                LOG.error("Koodistosta luku epäonnistui: {} {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
                throw new RuntimeException(e);
            }
        } else {
            return koodisto;
        }
    }

}
