package fi.vm.sade.valinta.kooste.hakuimport.route;

import java.util.concurrent.Future;

import org.apache.camel.Body;
import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;

/**
 * User: wuoti Date: 20.5.2013 Time: 10.24
 */
public interface HakukohdeImportRoute {

	final String DIRECT_HAKUKOHDE_IMPORT = "direct:hakuimport_tarjonnasta_koostepalvelulle";

	Future<Void> asyncAktivoiHakukohdeImport(
			@Body String hakukohdeOid,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);
}
