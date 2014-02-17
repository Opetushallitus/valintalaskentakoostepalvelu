package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import java.util.List;
import java.util.concurrent.Future;

import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;

public interface KoekutsukirjeHakemuksilleRoute {

	void koekutsukirjeetAktivointiHakemuksille(
			@Property("hakemusOids") List<String> hakemusOids,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Property(OPH.LETTER_BODY_TEXT) String letterBodyText);

	Future<Void> koekutsukirjeetAktivointiHakemuksilleAsync(
			@Property("hakemusOids") List<String> hakemusOids,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Property(OPH.LETTER_BODY_TEXT) String letterBodyText,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);

	final String DIRECT_KOEKUTSUKIRJEET_HAKEMUKSILLE = "direct:koekutsukirjeet_hakemuksille";
}
