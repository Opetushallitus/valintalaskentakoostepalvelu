package fi.vm.sade.valinta.kooste.external.resource.haku.proxy;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

@Component
public class HakemusProxyCachingImpl implements HakemusProxy,
		HakemusCacheInvalidator {

	private static final Logger LOG = LoggerFactory
			.getLogger(HakemusProxyCachingImpl.class);

	private final ApplicationResource applicationResource;
	private final Cache<String, Hakemus> hakemusCache;

	@Autowired
	public HakemusProxyCachingImpl(
			ApplicationResource applicationResource,
			@Value("${valintakoostepalvelu.cache.hakemus.size:150000}") int cacheSize) {
		this.hakemusCache = CacheBuilder.newBuilder().recordStats()
				.maximumSize(cacheSize).build();
		this.applicationResource = applicationResource;
	}

	@Override
	public Hakemus haeHakemus(final String hakemusOid)
			throws ExecutionException {

		Hakemus hakemus = hakemusCache.get(hakemusOid, new Callable<Hakemus>() {
			@Override
			public Hakemus call() throws Exception {
				try {

					return applicationResource.getApplicationByOid(hakemusOid);
				} catch (Exception e) {
					LOG.error("HAKEMUSCACHE EPAONNISTUI[{}]: {}", new Object[] {
							hakemusOid, e.getMessage() });
					e.printStackTrace();
					throw e;
				}
			}
		});

		if (LOG.isDebugEnabled()) {
			LOG.debug(hakemusCache.stats().toString());
		}

		return hakemus;

	}

	@Override
	public void invalidateAll() {
		hakemusCache.invalidateAll();
	}
}
