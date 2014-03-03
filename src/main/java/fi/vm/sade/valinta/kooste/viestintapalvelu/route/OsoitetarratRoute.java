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
public interface OsoitetarratRoute {

	final String SEDA_OSOITETARRAT = "seda:osoitetarrat?" +
	// jos palvelin sammuu niin ei suorita loppuun tyojonoa
			"purgeWhenStopping=true" +
			// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
			"&waitForTaskToComplete=Never" +
			// tyojonossa on yksi tyostaja
			"&concurrentConsumers=1";

	void osoitetarratAktivointi(
			@Property("DokumenttiTyyppi") DokumenttiTyyppi tyyppi,
			@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI) DokumenttiProsessi prosessi,
			@Property("hakemusOids") List<String> hakemusOids,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			// pitaisikohan olla vain bodyssa ja kasitella yksi oid kerrallaan?
			@Property("valintakoeOid") List<String> valintakoeOid,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);

	void osoitetarratAktivointi(
			@Property("DokumenttiTyyppi") DokumenttiTyyppi tyyppi,
			@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI) DokumenttiProsessi prosessi,
			@Property("hakemusOids") List<String> hakemusOids,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Property(OPH.HAKUOID) String hakuOid,
			@Property(OPH.SIJOITTELUAJOID) Long sijoitteluajoId,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);

	void osoitetarratAktivointi(
			@Property("DokumenttiTyyppi") DokumenttiTyyppi tyyppi,
			@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI) DokumenttiProsessi prosessi,
			@Property("hakemusOids") List<String> hakemusOids,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);
}
