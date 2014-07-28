package fi.vm.sade.valinta.kooste.valintakokeet.dto;

import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.AbstraktiTyo;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintaperusteetTyo;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakukohdeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
			HakemusDTO hakemusTyyppi) {
		Collection<String> hakukohdeOids = hakemusTyyppi.getHakukohteet().parallelStream().map(HakukohdeDTO::getOid).collect(Collectors.toSet());
		//
		// Ei hakutoiveita joten hakemusta ei kasitella
		//
		if (hakukohdeOids.isEmpty()) {
			LOG.error("Hakemuksella({}) ei ole hakutoiveita!",
					hakemusTyyppi.getHakemusoid());
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
