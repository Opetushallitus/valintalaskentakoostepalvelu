package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta;

import java.util.List;
import java.util.function.Consumer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ValintalaskentaAsyncResource {

	Peruutettava laske(LaskeDTO laskeDTO, Consumer<String> callback,
			Consumer<Throwable> failureCallback);

	Peruutettava valintakokeet(LaskeDTO laskeDTO, Consumer<String> callback,
			Consumer<Throwable> failureCallback);

	Peruutettava laskeKaikki(LaskeDTO laskeDTO, Consumer<String> callback,
			Consumer<Throwable> failureCallback);

	// @Path("laskejasijoittele")
	// @Consumes("application/json")
	// @Produces("text/plain")
	// @POST
	Peruutettava laskeJaSijoittele(List<LaskeDTO> lista,
			Consumer<String> callback, Consumer<Throwable> failureCallback);

	Peruutettava laskennantulokset(String hakuOid, String hakukohdeOid,
								   Consumer<ValintatietoValinnanvaiheDTO> callback, Consumer<Throwable> failureCallback);


}
