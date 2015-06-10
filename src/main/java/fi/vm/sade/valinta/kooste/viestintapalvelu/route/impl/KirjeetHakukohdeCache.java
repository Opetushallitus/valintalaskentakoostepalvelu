package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;

import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.HakukohdeV1Resource;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
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
	private fi.vm.sade.valinta.kooste.external.resource.haku.HakukohdeV1Resource hakukohdeV1Resource;
	//private HakukohdeResource hakukohdeResource;
	
	public MetaHakukohde haeHakukohde(final String hakukohdeOid)
			throws Exception {

		return metaHakukohdeCache.get(hakukohdeOid,
				new Callable<MetaHakukohde>() {
					@Override
					public MetaHakukohde call() throws Exception {
						
						HakukohdeV1RDTO
						 hakukohde = hakukohdeV1Resource
								.findByOid(hakukohdeOid).getResult();
						Teksti hakukohdeNimi = new Teksti(hakukohde
								.getHakukohteenNimet());
						String opetuskieli = getOpetuskieli(hakukohde.getOpetusKielet());
						LOG.error("Hakukohdekieli({}) Oid({}) Opetuskieli({})",hakukohdeNimi.getKieli(), hakukohdeOid, opetuskieli);
						
						Teksti tarjoajaNimi = new Teksti(hakukohde
								.getTarjoajaNimet());
						
						/*
						
						*/
						return new MetaHakukohde(hakukohdeNimi, tarjoajaNimi,
								hakukohdeNimi.getKieli(),opetuskieli);
					}

				});
	}
	
	public static String getOpetuskieli(Collection<String> opetuskielet) {
		TreeSet<String> preferoitukieli = Sets.newTreeSet();
		for (String opetuskieli :opetuskielet) {
			preferoitukieli.add(KieliUtil
					.normalisoiKielikoodi(opetuskieli));
		}
		if (preferoitukieli.contains(KieliUtil.SUOMI)) {
			return  KieliUtil.SUOMI;
		} else if (preferoitukieli.contains(KieliUtil.RUOTSI)) {
			return KieliUtil.RUOTSI;
		} else if (preferoitukieli.contains(KieliUtil.ENGLANTI)) {
			return KieliUtil.ENGLANTI;
		}
		return KieliUtil.SUOMI;
	}
}
