package fi.vm.sade.valinta.kooste.valintakokeet.komponentti.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

/**
 * User: wuoti Date: 5.8.2013 Time: 12.59
 * <p/>
 * Cachettava proxy valintaperusteille. Tallentaa välimuistiin hakukohteen
 * kaikkien valinnan vaiheiden valintaperusteet.
 */
@Component
public class HakukohteenValintaperusteetProxyCachingImpl implements
		HakukohteenValintaperusteetProxy {

	private static final Logger LOG = LoggerFactory
			.getLogger(HakukohteenValintaperusteetProxyCachingImpl.class);

	@Autowired
	private ValintaperusteService valintaperusteService;

	private Cache<String, List<ValintaperusteetTyyppi>> valintaperusteetCache;

	@PostConstruct
	public void init() {
		valintaperusteetCache = CacheBuilder.newBuilder().recordStats()
				.expireAfterWrite(10, TimeUnit.MINUTES).build();
	}

	@Override
	public List<ValintaperusteetTyyppi> haeValintaperusteet(String hakukohdeOid)
			throws ExecutionException {
		Set<String> oids = new HashSet<String>();
		oids.add(hakukohdeOid);

		return haeValintaperusteet(oids);
	}

	@Override
	public List<ValintaperusteetTyyppi> haeValintaperusteet(
			Set<String> hakukohdeOids) throws ExecutionException {

		List<ValintaperusteetTyyppi> result = new ArrayList<ValintaperusteetTyyppi>();

		for (final String hk : hakukohdeOids) {
			List<ValintaperusteetTyyppi> vps = valintaperusteetCache.get(hk,
					new Callable<List<ValintaperusteetTyyppi>>() {
						@Override
						public List<ValintaperusteetTyyppi> call()
								throws Exception {
							try {
								HakuparametritTyyppi param = new HakuparametritTyyppi();
								param.setHakukohdeOid(hk);

								return valintaperusteService
										.haeValintaperusteet(Arrays
												.asList(param));
							} catch (Exception e) {
								LOG.error(
										"VALINTAPERUSTEETCACHE EPAONNISTUI[{}]: {}",
										new Object[] { hk, e.getMessage() });
								e.printStackTrace();
								throw e;
							}
						}
					});

			result.addAll(vps);
		}

		return result;

	}

}
