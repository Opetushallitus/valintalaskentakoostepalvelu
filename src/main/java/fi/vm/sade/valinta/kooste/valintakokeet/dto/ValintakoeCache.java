package fi.vm.sade.valinta.kooste.valintakokeet.dto;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.hakemus.schema.HakukohdeTyyppi;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.AbstraktiTyo;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintaperusteetTyo;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Reitin exchange on elinkaari. Muuttujaan tallennetaan tilapaisesti
 *         kaikki valintakoelaskennassa tarvittavat tiedot.
 */
public class ValintakoeCache {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintakoeCache.class);
	private final ConcurrentHashMap<String, ValintaperusteetTyo<ValintakoeTyo>> valintaperusteetEsitiedot;

	public ValintakoeCache() {
		this.valintaperusteetEsitiedot = new ConcurrentHashMap<String, ValintaperusteetTyo<ValintakoeTyo>>();
	}

	public Collection<? extends AbstraktiTyo> hakukohteenEsitiedotOnSelvitettyJaSeuraavaksiEsitiedotTyojonoihin(
			HakemusTyyppi hakemusTyyppi) {
		Collection<String> hakukohdeOids = Collections2.transform(
				hakemusTyyppi.getHakutoive(),
				new Function<HakukohdeTyyppi, String>() {
					@Override
					public String apply(HakukohdeTyyppi hakukohdetyyppi) {
						return hakukohdetyyppi.getHakukohdeOid();
					}
				});
		//
		// Ei hakutoiveita joten hakemusta ei kasitella
		//
		if (hakukohdeOids.isEmpty()) {
			LOG.error("Hakemuksella({}) ei ole hakutoiveita!",
					hakemusTyyppi.getHakemusOid());
			return Collections.emptyList();
		}
		final Collection<AbstraktiTyo> tyot = Lists.newArrayList();
		ValintakoeTyo valintakoetyo = new ValintakoeTyo(hakemusTyyppi);
		Collection<ValintaperusteetTyo<ValintakoeTyo>> kaikkiValintaperusteetTyyppiEsitiedot = Lists
				.newArrayList();
		for (String hakukohdeOid : hakukohdeOids) {
			ValintaperusteetTyo<ValintakoeTyo> uusiEsitieto = new ValintaperusteetTyo<ValintakoeTyo>(
					hakukohdeOid);
			@SuppressWarnings("unchecked")
			ValintaperusteetTyo<ValintakoeTyo> aiempiTyosto = valintaperusteetEsitiedot
					.putIfAbsent(hakukohdeOid, uusiEsitieto);
			if (aiempiTyosto != null) {
				// Jokin säie on jo hakemassa tätä hakemusta. Joten ei laiteta
				// sitä työjonoon mutta laitetaan se riippuvuudeksi tälle
				// hakukohdeTyölle
				kaikkiValintaperusteetTyyppiEsitiedot.add(aiempiTyosto);
			} else {
				// Tämä säie ehti luoda esitiedon hakemukselle
				// Joten tehdään siitä työ myös työjonoon
				kaikkiValintaperusteetTyyppiEsitiedot.add(uusiEsitieto);
				tyot.add(uusiEsitieto);
			}
		}
		if (null != valintakoetyo
				.rekisteroiKuuntelijat(kaikkiValintaperusteetTyyppiEsitiedot)) {
			return Arrays.asList(valintakoetyo);
		}
		return tyot;
	}
}
