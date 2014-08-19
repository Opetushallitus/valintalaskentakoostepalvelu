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
import com.google.common.collect.Maps;

import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.util.Kieli;
import fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Koekutsukirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Letter;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;

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
	private final HakukohdeResource tarjontaResource;

	@Autowired
	public KoekutsukirjeetKomponentti(HaeOsoiteKomponentti osoiteKomponentti,
			HakukohdeResource tarjontaResource) {
		this.osoiteKomponentti = osoiteKomponentti;
		this.tarjontaResource = tarjontaResource;
	}

	public LetterBatch valmistaKoekutsukirjeet(@Body List<Hakemus> hakemukset,
			@Property(OPH.HAKUOID) String hakuOid,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Property(OPH.LETTER_BODY_TEXT) String letterBodyText,
			@Property(OPH.TARJOAJAOID) String tarjoajaOid,
			@Property("tag") String tag,
			@Property("templateName") String templateName) throws Exception {
		try {
			LOG.info("Luodaan koekutsukirjeet {} hakemukselle. Hakukohde({})",
					hakemukset.size(), hakukohdeOid);
			final List<Letter> kirjeet = Lists.newArrayList();
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
			String hakukohdeNimiTietyllaKielella = "";
			String tarjoajaNimiTietyllaKielella = "";
			for (Hakemus hakemus : hakemukset) {
				Osoite addressLabel = osoiteKomponentti.haeOsoite(hakemus);

				hakukohdeNimiTietyllaKielella = hakukohdeNimi
						.getTeksti(opetuskieli);
				tarjoajaNimiTietyllaKielella = tarjoajaNimi
						.getTeksti(opetuskieli);

				// hakukohdeNimiTietyllaKielella,
				// tarjoajaNimiTietyllaKielella, letterBodyText,
				// customLetterContents
				Map<String, Object> replacements = Maps.newHashMap();
				replacements.put("koulu", hakukohdeNimiTietyllaKielella);
				replacements.put("koulutus", tarjoajaNimiTietyllaKielella);
				replacements.put("tulokset", customLetterContents);
				// new Kirje(addressLabel, languageCode, koulu, koulutus,
				// tulokset)
				kirjeet.add(new Letter(addressLabel, templateName, opetuskieli,
						replacements));
			}
			LOG.info("Luodaan koekutsukirjeet {} henkilolle", kirjeet.size());
			LetterBatch viesti = new LetterBatch(kirjeet);

			viesti.setApplicationPeriod(hakuOid);
			viesti.setFetchTarget(hakukohdeOid);
			viesti.setLanguageCode(opetuskieli);
			viesti.setOrganizationOid(tarjoajaOid);
			viesti.setTag(tag);
			viesti.setTemplateName(templateName);
			Map<String, Object> templateReplacements = Maps.newHashMap();
			templateReplacements.put("sisalto", letterBodyText);
			templateReplacements
					.put("hakukohde", hakukohdeNimiTietyllaKielella);
			templateReplacements.put("tarjoaja", tarjoajaNimiTietyllaKielella);
			// Ei jarkevia koekutsukirjeelle
			// templateReplacements.put("hakukohde", koulutus.getTeksti(
			// preferoituKielikoodi, vakioHakukohteenNimi(hakukohdeOid)));
			// templateReplacements.put("tarjoaja", koulu.getTeksti(
			// preferoituKielikoodi, vakioTarjoajanNimi(hakukohdeOid)));
			viesti.setTemplateReplacements(templateReplacements);
			return viesti;
		} catch (Exception e) {
			LOG.error("Koekutsukirjeiden luonti epäonnistui!");
			e.printStackTrace();
			throw e;
		}
	}
}
