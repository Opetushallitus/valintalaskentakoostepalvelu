package fi.vm.sade.valinta.kooste.external.resource.hakuapp;

import java.util.List;
import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ApplicationAsyncResource {

	Peruutettava getApplicationsByOid(String hakuOid, String hakukohdeOid,
			Consumer<List<Hakemus>> callback,
			Consumer<Throwable> failureCallback);

	Peruutettava getApplicationAdditionalData(String hakuOid,
			String hakukohdeOid,
			Consumer<List<ApplicationAdditionalDataDTO>> callback,
			Consumer<Throwable> failureCallback);
}
