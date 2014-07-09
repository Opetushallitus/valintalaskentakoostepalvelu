package fi.vm.sade.valinta.kooste.valintakokeet.route;

import fi.vm.sade.valinta.kooste.valintakokeet.dto.ValintakoeCacheRest;
import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

/**
 * @author Jussi Jartamo
 */
public interface ValintakoelaskentaMuistissaRoute {

	final String SEDA_VALINTAKOELASKENTA_MUISTISSA = "seda:valintakoelaskentamuistissa?"
			+
			// jos palvelin sammuu niin ei suorita loppuun tyojonoa
			"purgeWhenStopping=true" +
			// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
			"&waitForTaskToComplete=Never" +
			// tyojonossa on yksi tyostaja
			"&concurrentConsumers=1";

	/**
	 * @param hakuOid
	 * @param @Optional hakemusOids whitelist hakemuksille joille
	 *        valintakoelaskenta tehdään
	 */
	void aktivoiValintakoelaskenta(
			@Property("valintakoeCache") ValintakoeCacheRest valintakoeCache,
			@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI) DokumenttiProsessi prosessi,
			@Property(OPH.HAKUOID) String hakuOid,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);
	// @Property("hakemusOids") Collection<String> hakemusOids);

}
