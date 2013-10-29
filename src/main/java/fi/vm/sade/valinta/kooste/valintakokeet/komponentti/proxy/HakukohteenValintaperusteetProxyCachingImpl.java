package fi.vm.sade.valinta.kooste.valintakokeet.komponentti.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy.ValintaperusteProxy;

/**
 * User: wuoti Date: 5.8.2013 Time: 12.59
 * <p/>
 * Cachettava proxy valintaperusteille. Tallentaa välimuistiin hakukohteen
 * kaikkien valinnan vaiheiden valintaperusteet.
 */
@Component
public class HakukohteenValintaperusteetProxyCachingImpl implements HakukohteenValintaperusteetProxy {

    @Autowired
    private ValintaperusteProxy valintaperusteProxy;

    private Cache<String, List<ValintaperusteetTyyppi>> valintaperusteetCache;

    @PostConstruct
    public void init() {
        valintaperusteetCache = CacheBuilder.newBuilder().recordStats().expireAfterWrite(24, TimeUnit.HOURS).build();
    }

    @Override
    public List<ValintaperusteetTyyppi> haeValintaperusteet(String hakukohdeOid) {
        Set<String> oids = new HashSet<String>();
        oids.add(hakukohdeOid);

        return haeValintaperusteet(oids);
    }

    @Override
    public List<ValintaperusteetTyyppi> haeValintaperusteet(Set<String> hakukohdeOids) {
        try {

            List<ValintaperusteetTyyppi> result = new ArrayList<ValintaperusteetTyyppi>();

            List<HakuparametritTyyppi> notCached = new ArrayList<HakuparametritTyyppi>();
            for (String hk : hakukohdeOids) {
                List<ValintaperusteetTyyppi> vps = valintaperusteetCache.getIfPresent(hk);
                if (vps == null) {
                    HakuparametritTyyppi param = new HakuparametritTyyppi();
                    param.setHakukohdeOid(hk);
                    notCached.add(param);
                } else {
                    result.addAll(vps);
                }
            }

            if (!notCached.isEmpty()) {
                result.addAll(fetchAndCacheResults(notCached));
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Can't fetch valintaperusteet, hakukohdeOids " + hakukohdeOids, e);
        }
    }

    private List<ValintaperusteetTyyppi> fetchAndCacheResults(List<HakuparametritTyyppi> notCached) {
        List<ValintaperusteetTyyppi> result = new ArrayList<ValintaperusteetTyyppi>();

        if (!notCached.isEmpty()) {
            List<ValintaperusteetTyyppi> vps = valintaperusteProxy.haeValintaperusteet(notCached);

            Map<String, List<ValintaperusteetTyyppi>> map = new HashMap<String, List<ValintaperusteetTyyppi>>();
            for (ValintaperusteetTyyppi vp : vps) {
                if (!map.containsKey(vp.getHakukohdeOid())) {
                    map.put(vp.getHakukohdeOid(), new ArrayList());
                }

                result.add(vp);
                map.get(vp.getHakukohdeOid()).add(vp);
            }

            for (Map.Entry<String, List<ValintaperusteetTyyppi>> e : map.entrySet()) {
                valintaperusteetCache.put(e.getKey(), e.getValue());
            }
        }

        return result;
    }
}
