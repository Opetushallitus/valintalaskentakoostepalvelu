package fi.vm.sade.valinta.kooste.valintatapajono.route;

import static fi.vm.sade.valinta.kooste.OPH.HAKUKOHDEOID;
import static fi.vm.sade.valinta.kooste.OPH.HAKUOID;
import static fi.vm.sade.valinta.kooste.OPH.VALINTAPAJONOOID;
import static fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI;

import org.apache.camel.Body;
import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

public interface ValintatapajonoTuontiRoute {
	final String SEDA_VALINTATAPAJONO_TUONTI = "seda:valintatapajono_tuonti?" +
	// jos palvelin sammuu niin ei suorita loppuun tyojonoa
			"purgeWhenStopping=true" +
			// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
			"&waitForTaskToComplete=Never" +
			// tyojonossa on yksi tyostaja
			"&concurrentConsumers=5";

	void tuo(@Property(PROPERTY_VALVOMO_PROSESSI) DokumenttiProsessi prosessi,
			@Property(HAKUOID) String hakuOid,
			@Property(HAKUKOHDEOID) String hakukohdeOid,
			@Property(VALINTAPAJONOOID) String valintatapajonoOid,
			@Body byte[] xlsx);

}
