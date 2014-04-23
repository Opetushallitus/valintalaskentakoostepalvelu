package fi.vm.sade.valinta.kooste.pistesyotto.route;

import java.io.InputStream;

import org.apache.camel.Body;
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
public interface PistesyottoTuontiRoute {

	final String SEDA_PISTESYOTTO_TUONTI = "seda:pistesyotto_tuonti?" +
	// jos palvelin sammuu niin ei suorita loppuun tyojonoa
			"purgeWhenStopping=true" +
			// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
			"&waitForTaskToComplete=Never" +
			// tyojonossa on yksi tyostaja
			"&concurrentConsumers=1";

	void tuo(
			@Body InputStream file,
			@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI) DokumenttiProsessi prosessi,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Property(OPH.HAKUOID) String hakuOid,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);

}
