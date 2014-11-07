package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import fi.vm.sade.koodisto.service.types.common.KieliType;
import fi.vm.sade.koodisto.util.KoodistoHelper;
import fi.vm.sade.organisaatio.resource.dto.OrganisaatioMetaDataRDTO;
import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Metadata;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Organisaatio;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Yhteystieto;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;

public class LueHakijapalvelunOsoite {
	private static final String KAYNTI_TYYPPI = "kaynti";
	private static final String POSTI_TYYPPI = "posti";
	public static final Set<String> kiinnostavatYhteystiedot = Collections
			.unmodifiableSet(Sets.newHashSet(Arrays.asList(KAYNTI_TYYPPI,
					POSTI_TYYPPI)));
	private static final Logger LOG = LoggerFactory
			.getLogger(LueHakijapalvelunOsoite.class);

	/**
	 * Preferoi posti-tyyppisia ja palauttaa vaan kaynti ja posti tyyppisista
	 * osoitteen.
	 * 
	 * @param preferoitukielikoodi
	 * @param organisaatio
	 * @return
	 */
	public static Osoite lueHakijapalvelunOsoite(
			HaeOsoiteKomponentti osoiteKomponentti,
			String preferoitukielikoodi, Organisaatio organisaatio) {
		if (organisaatio == null) {
			LOG.warn("Yritettiin hakea hakijapalvelun osoitetta tyhjasta organisaatio oliosta!");
			return null;
		}
		List<Yhteystieto> yhteystiedot = Optional
				.ofNullable(
						Optional.ofNullable(organisaatio.getMetadata())
								.orElse(new Metadata())
								.getYhteystiedot())
				.orElse(Collections.emptyList()).stream()
				.filter(kiinnostavaYhteystieto()).collect(Collectors.toList());
		if (yhteystiedot.isEmpty()) {
			LOG.warn(
					"Organisaatiolla {} ei ole tyyppia posti tai kaynti olevaa hakijapalvelun osoitetta!",
					organisaatio.getOid());
			return null;
		}

		if (KieliUtil.RUOTSI.equals(preferoitukielikoodi)) {
			List<Yhteystieto> ruotsinkielisetYhteystiedot = yhteystiedot
					.stream().filter(ruotsinkieliset())
					.collect(Collectors.toList());

			List<Yhteystieto> postit = ruotsinkielisetYhteystiedot
					.stream().filter(postiTyyppia())
					.collect(Collectors.toList());
			if (!postit.isEmpty()) {
				return osoiteKomponentti.haeOsoiteYhteystiedoista(postit
						.iterator().next(), KieliType.SV);
			}
			if (!ruotsinkielisetYhteystiedot.isEmpty()) {
				return osoiteKomponentti.haeOsoiteYhteystiedoista(
						ruotsinkielisetYhteystiedot.iterator().next(),
						KieliType.SV);
			}
		}
		List<Yhteystieto> postit = yhteystiedot.stream()
				.filter(postiTyyppia()).collect(Collectors.toList());
		if (!postit.isEmpty()) {
			return osoiteKomponentti.haeOsoiteYhteystiedoista(postit.iterator()
					.next(), KieliType.FI);
		}
		if (!yhteystiedot.isEmpty()) {
			return osoiteKomponentti.haeOsoiteYhteystiedoista(yhteystiedot
					.iterator().next(), KieliType.FI);
		}
		return null;
	}

	private static Predicate<Yhteystieto> postiTyyppia() {
		return yhteystiedot -> POSTI_TYYPPI.equals(yhteystiedot.getOsoiteTyyppi());
	}

	private static Predicate<Yhteystieto> kiinnostavaYhteystieto() {
		return yhteystiedot -> kiinnostavatYhteystiedot.contains(yhteystiedot.getOsoiteTyyppi()
				);
	}

	private static Predicate<Yhteystieto> ruotsinkieliset() {
		return yhteystiedot -> KieliUtil.RUOTSI.equals(KieliUtil
				.normalisoiKielikoodi(TarjontaUriToKoodistoUtil
						.cleanUri(yhteystiedot.getKieli())));
	}
}
