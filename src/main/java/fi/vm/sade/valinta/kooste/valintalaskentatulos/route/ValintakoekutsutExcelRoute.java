package fi.vm.sade.valinta.kooste.valintalaskentatulos.route;

import java.util.List;

import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

public interface ValintakoekutsutExcelRoute {

	final String SEDA_VALINTAKOE_EXCEL = "seda:valintakoekutsut_xls?" +
	// jos palvelin sammuu niin ei suorita loppuun tyojonoa
			"purgeWhenStopping=true" +
			// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
			"&waitForTaskToComplete=Never" +
			// tyojonossa on yksi tyostaja
			"&concurrentConsumers=1";

	void luoXls(
			@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI) DokumenttiProsessi prosessi,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Property("valintakoeOid") List<String> valintakoeOids,
			@Property("hakemusOids") List<String> hakemusOids,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);
}
