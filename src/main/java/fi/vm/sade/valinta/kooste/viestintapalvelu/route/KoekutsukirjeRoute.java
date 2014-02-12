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
public interface KoekutsukirjeRoute {

	void koekutsukirjeetAktivointi(@Property(OPH.HAKEMUSOID) String hakemusOid,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Property("valintakoeOid") List<String> valintakoeOid,
			@Property(OPH.LETTER_BODY_TEXT) String letterBodyText);

	Future<Void> koekutsukirjeetAktivointiAsync(
			@Property(OPH.HAKEMUSOID) String hakemusOid,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Property("valintakoeOid") List<String> valintakoeOid,
			@Property(OPH.LETTER_BODY_TEXT) String letterBodyText,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);

	final String DIRECT_KOEKUTSUKIRJEET = "direct:koekutsukirjeet";
}
