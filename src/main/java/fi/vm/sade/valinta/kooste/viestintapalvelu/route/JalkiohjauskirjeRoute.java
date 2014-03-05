package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import java.util.List;

import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface JalkiohjauskirjeRoute {
	final String SEDA_JALKIOHJAUSKIRJEET = "seda:jalkiohjauskirjeet?" +
	// jos palvelin sammuu niin ei suorita loppuun tyojonoa
			"purgeWhenStopping=true" +
			// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
			"&waitForTaskToComplete=Never" +
			// tyojonossa on yksi tyostaja
			"&concurrentConsumers=1";

	void jalkiohjauskirjeetAktivoi(
			@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI) DokumenttiProsessi prosessi,
			@Property("hakemusOids") List<String> hakemusOidit,
			@Property(OPH.HAKUOID) String hakuOid,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);
}
