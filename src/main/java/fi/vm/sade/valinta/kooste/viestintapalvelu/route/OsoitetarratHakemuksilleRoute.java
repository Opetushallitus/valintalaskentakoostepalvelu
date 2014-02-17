package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import java.util.List;
import java.util.concurrent.Future;

import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;

public interface OsoitetarratHakemuksilleRoute {
	final String DIRECT_OSOITETARRAT_HAKEMUKSILLE = "direct:osoitetarrat_hakemuksille_reitti";

	void osoitetarrotHakemuksilleAktivointi(
			@Property("hakemusOids") List<String> hakemusOids);

	Future<Void> osoitetarrotHakemuksilleAktivointiAsync(
			@Property("hakemusOids") List<String> hakemusOids,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);
}
