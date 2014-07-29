package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy;

import java.util.List;

import javax.annotation.PostConstruct;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetRestResource;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * User: wuoti Date: 5.8.2013 Time: 9.28
 * <p/>
 * Cachettava proxy valintaperusteille. Tallentaa välimuistiin yksittäisten
 * valinnan vaiheiden valintaperusteiden hakuja
 */
@Component
public class ValinnanVaiheenValintaperusteetProxyCachingImpl implements ValinnanVaiheenValintaperusteetProxy {

    private static final Logger LOG = LoggerFactory
            .getLogger(ValinnanVaiheenValintaperusteetProxyCachingImpl.class);

    @Autowired
    private ValintaperusteetRestResource valintaperusteProxy;

    //private LoadingCache<ValintaperusteetCacheKey, List<ValintaperusteetTyyppi>> valintaperusteetCache;
    private LoadingCache<ValintaperusteetCacheKey, List<ValintaperusteetDTO>> valintaperusteetCache;

    @Value("${valintakoostepalvelu.cache.valinnanvaiheenvalintaperusteet.size:5000}")
    private int cacheSize;

    @PostConstruct
    public void init() {
        valintaperusteetCache = CacheBuilder.newBuilder().recordStats().maximumSize(cacheSize)
                .build(new CacheLoader<ValintaperusteetCacheKey, List<ValintaperusteetDTO>>() {
                    @Override
                    public List<ValintaperusteetDTO> load(ValintaperusteetCacheKey key) throws Exception {

                        return ValinnanVaiheenValintaperusteetProxyCachingImpl.this.valintaperusteProxy
                                .haeValintaperusteet(key.getHakukohdeOid(), key.getValinnanVaihejarjestysluku());
                    }
                });
    }

    @Override
    public List<ValintaperusteetDTO> haeValintaperusteet(String hakukohdeOid, int valinnanVaiheJarjestysluku) {

        try {
            if(LOG.isDebugEnabled()) {
                LOG.debug(valintaperusteetCache.stats().toString());
            }
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
