package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import java.util.Collection;
import java.util.concurrent.Future;

import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaCache;

public interface ValintalaskentaMuistissaRoute {

	void aktivoiValintalaskenta(
			@Property("valintalaskentaCache") ValintalaskentaCache valintalaskentaCache,
			@Property("hakukohdeOids") Collection<String> hakukohdeOids,
			@Property(OPH.HAKUOID) String hakuOid);

	Future<Void> aktivoiValintalaskentaAsync(
			@Property("valintalaskentaCache") ValintalaskentaCache valintalaskentaCache,
			@Property("hakukohdeOids") Collection<String> hakukohdeOids,
			@Property(OPH.HAKUOID) String hakuOid,
			@Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);
}
