package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri;

import java.util.List;
import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;

public interface SuoritusrekisteriAsyncResource {

	Peruutettava getOppijatByHakukohde(String hakukohdeOid, String referenssiPvm,
									   Consumer<List<Oppija>> callback, Consumer<Throwable> failureCallback);

}
