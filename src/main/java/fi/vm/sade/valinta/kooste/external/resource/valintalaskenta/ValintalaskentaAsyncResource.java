package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta;

import java.util.List;
import java.util.function.Consumer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ValintalaskentaAsyncResource {

	void laske(LaskeDTO laskeDTO, Consumer<String> callback,
			Consumer<Throwable> failureCallback);

	void valintakokeet(LaskeDTO laskeDTO, Consumer<String> callback,
			Consumer<Throwable> failureCallback);

	void laskeKaikki(LaskeDTO laskeDTO, Consumer<String> callback,
			Consumer<Throwable> failureCallback);
}
