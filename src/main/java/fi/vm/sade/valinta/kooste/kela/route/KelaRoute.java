package fi.vm.sade.valinta.kooste.kela.route;

import java.util.Collection;

import org.apache.camel.Body;
import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

/**
 * @author Jussi Jartamo
 */
public interface KelaRoute {

	/**
	 * Aloittaa Kela-siirtodokumentin luonnin.
	 */
	void aloitaKelaLuonti(
			@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI) KelaProsessi prosessi,
			//
			@Body Collection<String> hakuOids,
			//
			// @Property(PROPERTY_LUKUVUOSI) Date lukuvuosi,
			//
			// @Property(PROPERTY_POIMINTAPAIVAMAARA) Date poimintapaivamaara,
			//
			@Property(PROPERTY_AINEISTONNIMI) String aineistonNimi,
			//
			@Property(PROPERTY_ORGANISAATIONNIMI) String organisaationNimi,
			//
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);

	/**
	 * Camel route description.
	 */
	final String SEDA_KELA_LUONTI = "seda:kela_luonti?" +
	// jos palvelin sammuu niin ei suorita loppuun tyojonoa
			"purgeWhenStopping=true" +
			// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
			"&waitForTaskToComplete=Never" +
			// tyojonossa on yksi tyostaja
			"&concurrentConsumers=1";
	/**
	 * Camel route description.
	 */
	final String KELA_SIIRTO = "direct:kela_siirto";

	// jos palvelin sammuu niin ei suorita loppuun tyojonoa
	// "purgeWhenStopping=true" +
	// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
	// "&waitForTaskToComplete=Never" +
	// tyojonossa on yksi tyostaja
	// "&concurrentConsumers=1";
	/**
	 * Camel route description.
	 */
	final String DIRECT_KELA_FAILED = "direct:kela_failed";
	/**
	 * Property hakuOid
	 */
	final String PROPERTY_HAKUOID = "hakuOid";
	/**
	 * Property lukuvuosi
	 */
	final String PROPERTY_LUKUVUOSI = "lukuvuosi";
	/**
	 * Property poimintapaivamaara
	 */
	final String PROPERTY_POIMINTAPAIVAMAARA = "poimintapaivamaara";
	/**
	 * Property aineistonNimi
	 */
	final String PROPERTY_AINEISTONNIMI = "aineistonNimi";
	/**
	 * Property organisaationNimi
	 */
	final String PROPERTY_ORGANISAATIONNIMI = "organisaationNimi";
	/**
	 * Property lukuvuosi
	 */
	final String PROPERTY_DOKUMENTTI_ID = "dokumenttiId";
}
