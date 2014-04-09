package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.camel.Body;
import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.util.Kieli;
import fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Koekutsukirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class KoekutsukirjeetKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(KoekutsukirjeetKomponentti.class);

	private final HaeOsoiteKomponentti osoiteKomponentti;
	private final HaeHakukohdeNimiTarjonnaltaKomponentti tarjontaProxy;
	private final HakukohdeResource tarjontaResource;
	private final KoodiService koodiService;

	@Autowired
	public KoekutsukirjeetKomponentti(KoodiService koodiService,
			HaeOsoiteKomponentti osoiteKomponentti,
			HaeHakukohdeNimiTarjonnaltaKomponentti tarjontaProxy,
			HakukohdeResource tarjontaResource) {
		this.koodiService = koodiService;
		this.osoiteKomponentti = osoiteKomponentti;
		this.tarjontaProxy = tarjontaProxy;
		this.tarjontaResource = tarjontaResource;
	}

	public Kirjeet<Koekutsukirje> valmistaKoekutsukirjeet(
			@Body List<Hakemus> hakemukset,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Property(OPH.LETTER_BODY_TEXT) String letterBodyText)
			throws Exception {
		try {
			LOG.info("Luodaan koekutsukirjeet {} hakemukselle. Hakukohde({})",
					hakemukset.size(), hakukohdeOid);
			final List<Koekutsukirje> kirjeet = Lists.newArrayList();
			// Custom contents?
			final List<Map<String, String>> customLetterContents = Collections
					.emptyList();

			// final HakukohdeNimiRDTO nimi;
			final HakukohdeDTO nimi;
			final String opetuskieli;
			try {
				// hakukohde =
				nimi = tarjontaResource.getByOID(hakukohdeOid);

				Collection<String> kielikoodit = Collections2.transform(
						nimi.getOpetuskielet(), new Function<String, String>() {
							@Override
							public String apply(
									String tarjonnanEpastandardiKoodistoUri) {
								return TarjontaUriToKoodistoUtil
										.cleanUri(tarjonnanEpastandardiKoodistoUri);
							}
						});
				opetuskieli = new Kieli(kielikoodit).getKieli();
				// tarjontaProxy.haeHakukohdeNimi(hakukohdeOid);
				// hakukohde.getHakukohdeNimi()
			} catch (Exception e) {
				LOG.error("Tarjonnalta ei saatu hakukohteelle({}) nimea!",
						hakukohdeOid);
				throw e;
			}
			final Teksti hakukohdeNimi = new Teksti(nimi.getHakukohdeNimi());
			final Teksti tarjoajaNimi = new Teksti(nimi.getTarjoajaNimi());

			// try {
			// if (nimi.getLiitteet() != null) {
			// for (HakukohdeLiiteDTO l : nimi.getLiitteet()) {
			// LOG.error("\r\n{}\r\n", new GsonBuilder()
			// .setPrettyPrinting().create().toJson(l));
			// }
			// } else {
			// LOG.error("NULL LIITTEET!");
			// }
			// } catch (Exception e) {
			// LOG.error("Ei voitu tulostaa liitteitä!");
			// }

			for (Hakemus hakemus : hakemukset) {
				Osoite addressLabel = osoiteKomponentti.haeOsoite(hakemus);

				String hakukohdeNimiTietyllaKielella = hakukohdeNimi
						.getTeksti(opetuskieli);
				String tarjoajaNimiTietyllaKielella = tarjoajaNimi
						.getTeksti(opetuskieli);

				kirjeet.add(new Koekutsukirje(addressLabel, opetuskieli,
						hakukohdeNimiTietyllaKielella,
						tarjoajaNimiTietyllaKielella, letterBodyText,
						customLetterContents));
			}
			LOG.info("Luodaan koekutsukirjeet {} henkilolle", kirjeet.size());
			return new Kirjeet<Koekutsukirje>(kirjeet);
		} catch (Exception e) {
			LOG.error("Koekutsukirjeiden luonti epäonnistui!");
			e.printStackTrace();
			throw e;
		}
	}
}
