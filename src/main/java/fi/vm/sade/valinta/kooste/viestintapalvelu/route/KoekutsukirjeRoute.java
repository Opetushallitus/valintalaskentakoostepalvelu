package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import java.util.concurrent.Future;

import org.apache.camel.Body;
import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface KoekutsukirjeRoute {

	void koekutsukirjeetAktivointi(
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Body String letterBodyText);

	Future<Void> koekutsukirjeetAktivointiAsync(
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Body String letterBodyText);

	final String DIRECT_KOEKUTSUKIRJEET = "direct:koekutsukirjeet";
}
