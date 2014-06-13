package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;

import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;

@Component
public class KirjeetHakukohdeCache {
	private final Logger LOG = LoggerFactory
			.getLogger(KirjeetHakukohdeCache.class);
	private final Cache<String, MetaHakukohde> metaHakukohdeCache = CacheBuilder
			.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).build();

	// private HaeHakukohdeNimiTarjonnaltaKomponentti tarjontaProxy;
	@Autowired
	private HakukohdeResource hakukohdeResource;

	public MetaHakukohde haeHakukohde(final String hakukohdeOid)
			throws Exception {

		return metaHakukohdeCache.get(hakukohdeOid,
				new Callable<MetaHakukohde>() {
					@Override
					public MetaHakukohde call() throws Exception {
						HakukohdeDTO hakukohde = hakukohdeResource
								.getByOID(hakukohdeOid);
						Teksti hakukohdeNimi = new Teksti(hakukohde
								.getHakukohdeNimi());
						Teksti tarjoajaNimi = new Teksti(hakukohde
								.getTarjoajaNimi());
						Collection<String> preferoitukieli = Sets.newTreeSet();
						for (String opetuskieli : hakukohde.getOpetuskielet()) {
							preferoitukieli.add(KieliUtil
									.normalisoiKielikoodi(opetuskieli));
						}
						if (preferoitukieli.contains(KieliUtil.SUOMI)) {
							return new MetaHakukohde(hakukohdeNimi,
									tarjoajaNimi, KieliUtil.SUOMI);
						} else if (preferoitukieli.contains(KieliUtil.RUOTSI)) {
							return new MetaHakukohde(hakukohdeNimi,
									tarjoajaNimi, KieliUtil.RUOTSI);
						}
						return new MetaHakukohde(hakukohdeNimi, tarjoajaNimi,
								KieliUtil.SUOMI);
						// LOG.error("preferoitukieli on {}", preferoitukieli);

					}

				});
	}
}
