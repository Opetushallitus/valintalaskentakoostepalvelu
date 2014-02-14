package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import java.util.List;
import java.util.concurrent.Future;

import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface JalkiohjauskirjeRoute {
	final String DIRECT_JALKIOHJAUSKIRJEET = "direct:jalkiohjauskirjeet";

	void jalkiohjauskirjeetAktivoi(
			@Property("hakemusOidit") List<String> hakemusOidit,
			@Property(OPH.HAKUOID) String hakuOid);

	Future<Void> jalkiohjauskirjeetAktivoiAsync(
			@Property("hakemusOidit") List<String> hakemusOidit,
			@Property(OPH.HAKUOID) String hakuOid,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);
}
