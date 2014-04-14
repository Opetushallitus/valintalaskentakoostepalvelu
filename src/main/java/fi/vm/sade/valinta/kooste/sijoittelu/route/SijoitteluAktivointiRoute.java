package fi.vm.sade.valinta.kooste.sijoittelu.route;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface SijoitteluAktivointiRoute {

	final String SEDA_SIJOITTELU_AKTIVOI = "seda:sijoitteluAktivoi?" +
	// jos palvelin sammuu niin ei suorita loppuun tyojonoa
			"purgeWhenStopping=true" +
			// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
			"&waitForTaskToComplete=Never" +
			// tyojonossa on yksi tyostaja
			"&concurrentConsumers=1";

	void aktivoiSijoittelu(
			@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI) DokumenttiProsessi prosessi,
			@Property(OPH.HAKUOID) String hakuOid);
}
