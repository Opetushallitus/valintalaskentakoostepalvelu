package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri;

import java.util.List;
import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface SuoritusrekisteriAsyncResource {

	Peruutettava getOppijatByHaku(String hakuOid,
			Consumer<List<Oppija>> callback, Consumer<Throwable> failureCallback);

	Peruutettava getOppijatByOrganisaatio(String organisaatioOid,
			Consumer<List<Oppija>> callback, Consumer<Throwable> failureCallback);

}
