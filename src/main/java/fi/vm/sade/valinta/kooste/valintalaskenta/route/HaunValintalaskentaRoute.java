package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import java.util.List;
import java.util.concurrent.Future;

import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;

/**
 * User: wuoti Date: 27.5.2013 Time: 9.05
 */
public interface HaunValintalaskentaRoute {

	/**
	 * Camel route description.
	 */
	String DIRECT_VALINTALASKENTA_HAULLE = "direct:valintalaskenta_haulle";

	void aktivoiValintalaskenta(@Property(OPH.HAKUOID) String hakuOid);

	Future<Void> aktivoiValintalaskentaAsync(
			@Property("hakukohdeOids") List<String> hakukohdeOids,
			@Property(OPH.HAKUOID) String hakuOid,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);
}
