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
import fi.vm.sade.service.valintaperusteet.dto.model.Kieli;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Metadata;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Organisaatio;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Yhteystieto;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;

public class LueHakijapalvelunOsoite {
	private static final String KAYNTI_TYYPPI = "kaynti";
	private static final String POSTI_TYYPPI = "posti";
	private static final String ULKOMAINEN_POSTI_TYYPPI = "ulkomainen_posti";
	
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
			String preferoitukielikoodi, Organisaatio organisaatio,
			Teksti organisaationimi) {
		if (organisaatio == null) {
			LOG.warn("Yritettiin hakea hakijapalvelun osoitetta tyhjasta organisaatio oliosta!");
			return null;
		}
		if (organisaatio.getMetadata().getHakutoimistonNimi() == null) {
			LOG.warn("Yritettiin hakea hakijapalvelun organisaatiosta jossa ei hakutoimistoa!");
			return null;
		}
		
		List<Yhteystieto> yhteystiedot = Optional
				.ofNullable(
						Optional.ofNullable(organisaatio.getMetadata())
								.orElse(new Metadata())
								.getYhteystiedot())
				.orElse(Collections.emptyList()).stream()
				.filter(kiinnostavaYhteystieto()).collect(Collectors.toList());
		List<Yhteystieto> puhelinnumerot = Optional
				.ofNullable(
						Optional.ofNullable(organisaatio.getMetadata())
								.orElse(new Metadata())
								.getYhteystiedot())
				.orElse(Collections.emptyList()).stream()
				.filter(puhelinnumero()).collect(Collectors.toList());
		List<Yhteystieto> emailit = Optional
				.ofNullable(
						Optional.ofNullable(organisaatio.getMetadata())
								.orElse(new Metadata())
								.getYhteystiedot())
				.orElse(Collections.emptyList()).stream()
				.filter(email()).collect(Collectors.toList());
		if (yhteystiedot.isEmpty()) {
			LOG.warn(
					"Organisaatiolla {} ei ole tyyppia posti tai kaynti olevaa hakijapalvelun osoitetta!",
					organisaatio.getOid());
			return null;
		}
		LOG.error("!!!!!!!!!!!!!! preferoitukieli "+preferoitukielikoodi);
		String numero = null;
		String email = null;
		if(!puhelinnumerot.isEmpty()) {
			numero = puhelinnumerot.get(0).getNumero();
		}
		if(!emailit.isEmpty()) {
			email = emailit.get(0).getEmail();
		}
		if (KieliUtil.ENGLANTI.equals(preferoitukielikoodi)) {
			List<Yhteystieto> englanninkielisetYhteystiedot = yhteystiedot
					.stream().filter(englanninkieliset())
					.collect(Collectors.toList());
			List<Yhteystieto> englanninkielisetNumerot = puhelinnumerot
					.stream().filter(englanninkieliset())
					.collect(Collectors.toList());
			List<Yhteystieto> englanninkielisetEmailit = emailit
					.stream().filter(englanninkieliset())
					.collect(Collectors.toList());
			if(!englanninkielisetNumerot.isEmpty()) {
				numero = englanninkielisetNumerot.get(0).getNumero();
			}
			if(!englanninkielisetEmailit.isEmpty()) {
				email = englanninkielisetEmailit.get(0).getEmail();
			}
			List<Yhteystieto> postit = englanninkielisetYhteystiedot
					.stream().filter(ulkomainenPostiTyyppia())
					.collect(Collectors.toList());
			if (!postit.isEmpty()) {
				return osoiteKomponentti.haeOsoiteYhteystiedoista(postit
						.iterator().next(), KieliType.EN, organisaatio.getMetadata().getHakutoimistonNimi().get("kieli_en#1"),email, numero);
			}
			if (!englanninkielisetYhteystiedot.isEmpty()) {
				return osoiteKomponentti.haeOsoiteYhteystiedoista(
						englanninkielisetYhteystiedot.iterator().next(),
						KieliType.EN, organisaatio.getMetadata().getHakutoimistonNimi().get("kieli_en#1"),email, numero);
			}
		}
		if (KieliUtil.RUOTSI.equals(preferoitukielikoodi)) {
			List<Yhteystieto> ruotsinkielisetYhteystiedot = yhteystiedot
					.stream().filter(ruotsinkieliset())
					.collect(Collectors.toList());

			List<Yhteystieto> postit = ruotsinkielisetYhteystiedot
					.stream().filter(postiTyyppia())
					.collect(Collectors.toList());
			
			List<Yhteystieto> ruotsinkielisetNumerot = puhelinnumerot
					.stream().filter(ruotsinkieliset())
					.collect(Collectors.toList());
			List<Yhteystieto> ruotsinkielisetEmailit = emailit
					.stream().filter(ruotsinkieliset())
					.collect(Collectors.toList());
			if(!ruotsinkielisetNumerot.isEmpty()) {
				numero = ruotsinkielisetNumerot.get(0).getNumero();
			}
			if(!ruotsinkielisetEmailit.isEmpty()) {
				email = ruotsinkielisetEmailit.get(0).getEmail();
			}
			if (!postit.isEmpty()) {
				return osoiteKomponentti.haeOsoiteYhteystiedoista(postit
						.iterator().next(), KieliType.SV, organisaatio.getMetadata().getHakutoimistonNimi().get("kieli_sv#1"), email, numero);
			}
			if (!ruotsinkielisetYhteystiedot.isEmpty()) {
				return osoiteKomponentti.haeOsoiteYhteystiedoista(
						ruotsinkielisetYhteystiedot.iterator().next(),
						KieliType.SV, organisaatio.getMetadata().getHakutoimistonNimi().get("kieli_sv#1"), email, numero);
			}
		}

		List<Yhteystieto> suomenkielisetNumerot = puhelinnumerot
				.stream().filter(suomenkieliset())
				.collect(Collectors.toList());
		List<Yhteystieto> suomenkielisetEmailit = emailit
				.stream().filter(suomenkieliset())
				.collect(Collectors.toList());
		if(!suomenkielisetNumerot.isEmpty()) {
			numero = suomenkielisetNumerot.get(0).getNumero();
		}
		if(!suomenkielisetEmailit.isEmpty()) {
			email = suomenkielisetEmailit.get(0).getEmail();
		}
		List<Yhteystieto> suomenkielisetYhteystiedot = yhteystiedot
				.stream().filter(suomenkieliset())
				.collect(Collectors.toList());
		List<Yhteystieto> suomenkielisetPostit = suomenkielisetYhteystiedot
				.stream().filter(postiTyyppia())
				.collect(Collectors.toList());
		if(!suomenkielisetPostit.isEmpty()) {
			return osoiteKomponentti.haeOsoiteYhteystiedoista(suomenkielisetPostit.iterator()
					.next(), KieliType.FI, organisaatio.getMetadata().getHakutoimistonNimi().get("kieli_fi#1"), email, numero);
		}
		List<Yhteystieto> postit = yhteystiedot.stream()
				.filter(postiTyyppia()).collect(Collectors.toList());
		if (!postit.isEmpty()) {
			return osoiteKomponentti.haeOsoiteYhteystiedoista(postit.iterator()
					.next(), KieliType.FI, organisaatio.getMetadata().getHakutoimistonNimi().get("kieli_fi#1"), email, numero);
		}
		if (!yhteystiedot.isEmpty()) {
			return osoiteKomponentti.haeOsoiteYhteystiedoista(yhteystiedot
					.iterator().next(), KieliType.FI, organisaatio.getMetadata().getHakutoimistonNimi().get("kieli_fi#1"), email, numero);
		}
		return null;
	}

	private static Predicate<Yhteystieto> postiTyyppia() {
		return yhteystiedot -> POSTI_TYYPPI.equals(yhteystiedot.getOsoiteTyyppi());
	}

	private static Predicate<Yhteystieto> ulkomainenPostiTyyppia() {
		return yhteystiedot -> ULKOMAINEN_POSTI_TYYPPI.equals(yhteystiedot.getOsoiteTyyppi());
	}

	
	private static Predicate<Yhteystieto> kiinnostavaYhteystieto() {
		return yhteystiedot -> kiinnostavatYhteystiedot.contains(yhteystiedot.getOsoiteTyyppi()
				);
	}

	private static Predicate<Yhteystieto> puhelinnumero() {
		return yhteystiedot -> "puhelin".equals(yhteystiedot.getTyyppi()) && !StringUtils.isBlank(yhteystiedot.getNumero());
	}
	private static Predicate<Yhteystieto> email() {
		return yhteystiedot -> !StringUtils.isBlank(yhteystiedot.getEmail());
	}
	private static Predicate<Yhteystieto> suomenkieliset() {
		return yhteystiedot -> KieliUtil.SUOMI.equals(KieliUtil
				.normalisoiKielikoodi(TarjontaUriToKoodistoUtil
						.cleanUri(yhteystiedot.getKieli())));
	}
	private static Predicate<Yhteystieto> ruotsinkieliset() {
		return yhteystiedot -> KieliUtil.RUOTSI.equals(KieliUtil
				.normalisoiKielikoodi(TarjontaUriToKoodistoUtil
						.cleanUri(yhteystiedot.getKieli())));
	}
	private static Predicate<Yhteystieto> englanninkieliset() {
		return yhteystiedot -> KieliUtil.ENGLANTI.equals(KieliUtil
				.normalisoiKielikoodi(TarjontaUriToKoodistoUtil
						.cleanUri(yhteystiedot.getKieli())));
	}
	
}
