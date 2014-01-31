package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.camel.Body;
import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Koekutsukirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;

@Component
public class KoekutsukirjeetKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(KoekutsukirjeetKomponentti.class);

	private final HaeOsoiteKomponentti osoiteKomponentti;
	private final HaeHakukohdeNimiTarjonnaltaKomponentti tarjontaProxy;

	@Autowired
	public KoekutsukirjeetKomponentti(HaeOsoiteKomponentti osoiteKomponentti,
			HaeHakukohdeNimiTarjonnaltaKomponentti tarjontaProxy) {
		this.osoiteKomponentti = osoiteKomponentti;
		this.tarjontaProxy = tarjontaProxy;
	}

	public Kirjeet<Koekutsukirje> valmistaKoekutsukirjeet(
			@Body List<Hakemus> hakemukset,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Property(OPH.LETTER_BODY_TEXT) String letterBodyText)
			throws Exception {
		final List<Koekutsukirje> kirjeet = Lists.newArrayList();
		// Custom contents?
		final List<Map<String, String>> customLetterContents = Collections
				.emptyList();

		final HakukohdeNimiRDTO nimi;
		try {
			nimi = tarjontaProxy.haeHakukohdeNimi(hakukohdeOid);
		} catch (Exception e) {
			LOG.error("Tarjonnalta ei saatu hakukohteelle({}) nimea!",
					hakukohdeOid);
			throw e;
		}
		final Teksti hakukohdeNimi = new Teksti(nimi.getHakukohdeNimi());

		for (Hakemus hakemus : hakemukset) {
			Osoite addressLabel = osoiteKomponentti.haeOsoite(hakemus);
			String languageCode = hakukohdeNimi.getKieli(); // hakukohteen kieli
															// koekutsuissa
			String hakukohde = hakukohdeNimi.getTeksti();

			kirjeet.add(new Koekutsukirje(addressLabel, languageCode,
					hakukohde, letterBodyText, customLetterContents));
		}

		return new Kirjeet<Koekutsukirje>(kirjeet);
	}

}
