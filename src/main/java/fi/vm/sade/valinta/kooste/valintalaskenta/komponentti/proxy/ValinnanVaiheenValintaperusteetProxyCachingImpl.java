package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

/**
 * User: wuoti Date: 5.8.2013 Time: 9.28
 * <p/>
 * Cachettava proxy valintaperusteille. Tallentaa välimuistiin yksittäisten
 * valinnan vaiheiden valintaperusteiden hakuja
 */
@Component
public class ValinnanVaiheenValintaperusteetProxyCachingImpl implements ValinnanVaiheenValintaperusteetProxy {

    @Autowired
    private ValintaperusteProxy valintaperusteProxy;

    private LoadingCache<ValintaperusteetCacheKey, List<ValintaperusteetTyyppi>> valintaperusteetCache;

    @PostConstruct
    public void init() {
        valintaperusteetCache = CacheBuilder.newBuilder().recordStats().refreshAfterWrite(10, TimeUnit.MINUTES)
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build(new CacheLoader<ValintaperusteetCacheKey, List<ValintaperusteetTyyppi>>() {
                    @Override
                    public List<ValintaperusteetTyyppi> load(ValintaperusteetCacheKey key) throws Exception {
                        HakuparametritTyyppi params = new HakuparametritTyyppi();
                        params.setHakukohdeOid(key.getHakukohdeOid());
                        params.setValinnanVaiheJarjestysluku(key.getValinnanVaihejarjestysluku());

                        return ValinnanVaiheenValintaperusteetProxyCachingImpl.this.valintaperusteProxy
                                .haeValintaperusteet(Arrays.asList(params));
                    }
                });
    }

    @Override
    public List<ValintaperusteetTyyppi> haeValintaperusteet(String hakukohdeOid, int valinnanVaiheJarjestysluku) {

        try {
            return valintaperusteetCache.get(new ValintaperusteetCacheKey(hakukohdeOid, valinnanVaiheJarjestysluku));

        } catch (Exception e) {
            throw new RuntimeException("Can't fetch valintaperusteet, hakukohdeOid " + hakukohdeOid
                    + ", valinnanVaiheJarjestysluku " + valinnanVaiheJarjestysluku, e);
        }
    }

    private class ValintaperusteetCacheKey {
        private ValintaperusteetCacheKey(String hakukohdeOid, Integer valinnanVaihejarjestysluku) {
            this.hakukohdeOid = hakukohdeOid;
            this.valinnanVaihejarjestysluku = valinnanVaihejarjestysluku;
        }

        private String hakukohdeOid;
        private Integer valinnanVaihejarjestysluku;

        private String getHakukohdeOid() {
            return hakukohdeOid;
        }

        private void setHakukohdeOid(String hakukohdeOid) {
            this.hakukohdeOid = hakukohdeOid;
        }

        private Integer getValinnanVaihejarjestysluku() {
            return valinnanVaihejarjestysluku;
        }

        private void setValinnanVaihejarjestysluku(Integer valinnanVaihejarjestysluku) {
            this.valinnanVaihejarjestysluku = valinnanVaihejarjestysluku;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (o == this) {
                return true;
            } else if (o.getClass() != getClass()) {
                return false;
            } else {
                ValintaperusteetCacheKey k = (ValintaperusteetCacheKey) o;
                return new EqualsBuilder().append(hakukohdeOid, k.hakukohdeOid)
                        .append(valinnanVaihejarjestysluku, k.valinnanVaihejarjestysluku).isEquals();
            }
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(5, 41).append(hakukohdeOid).append(valinnanVaihejarjestysluku).toHashCode();
        }
    }
}
