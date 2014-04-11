package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaCache;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaMuistissaProsessi;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

public interface ValintalaskentaMuistissaRoute {

	final String SEDA_VALINTALASKENTA_MUISTISSA = "seda:valintalaskenta_muistissa"
			+
			//
			"?purgeWhenStopping=true&waitForTaskToComplete=Never" +
			//
			"&concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentamuistissa.threadpoolsize:2}";

	// yksikko testausta varten
	void aktivoiValintalaskenta(
			@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI) ValintalaskentaMuistissaProsessi prosessi,
			@Property("valintalaskentaCache") ValintalaskentaCache valintalaskentaCache,
			// @Property("hakukohdeOids") Collection<String> hakukohdeOids,
			@Property(OPH.HAKUOID) String hakuOid,
			@Property("valinnanvaihe") Integer valinnanvaihe,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);

}
