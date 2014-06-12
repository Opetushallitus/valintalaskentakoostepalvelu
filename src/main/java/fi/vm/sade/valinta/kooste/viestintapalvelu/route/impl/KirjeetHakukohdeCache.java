package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;

@Component
public class KirjeetHakukohdeCache {

	private final Cache<String, MetaHakukohde> metaHakukohdeCache = CacheBuilder
			.newBuilder().softValues().expireAfterWrite(12, TimeUnit.HOURS)
			.build();

	@Autowired
	private HaeHakukohdeNimiTarjonnaltaKomponentti tarjontaProxy;

	public MetaHakukohde haeHakukohde(final String hakukohdeOid)
			throws Exception {

		return metaHakukohdeCache.get(hakukohdeOid,
				new Callable<MetaHakukohde>() {
					@Override
					public MetaHakukohde call() throws Exception {
						HakukohdeNimiRDTO nimi = tarjontaProxy
								.haeHakukohdeNimi(hakukohdeOid);
						Teksti hakukohdeNimi = new Teksti(nimi
								.getHakukohdeNimi());
						Teksti tarjoajaNimi = new Teksti(nimi.getTarjoajaNimi());
						return new MetaHakukohde(hakukohdeNimi, tarjoajaNimi);
					}

				});
	}
}
