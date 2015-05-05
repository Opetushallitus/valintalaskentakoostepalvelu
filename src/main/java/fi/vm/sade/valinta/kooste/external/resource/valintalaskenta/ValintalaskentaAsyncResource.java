package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta;

import java.util.List;
import java.util.function.Consumer;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
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

	Peruutettava lisaaTuloksia(String hakuOid, String hakukohdeOid, String tarjoajaOid, ValinnanvaiheDTO vaihe, Consumer<ValinnanvaiheDTO> callback, Consumer<Throwable> failureCallback);

	// @Path("laskejasijoittele")
	// @Consumes("application/json")
	// @Produces("text/plain")
	// @POST
	Peruutettava laskeJaSijoittele(List<LaskeDTO> lista,
			Consumer<String> callback, Consumer<Throwable> failureCallback);

	Peruutettava laskennantulokset(String hakuOid, String hakukohdeOid,
								   Consumer<List<ValintatietoValinnanvaiheDTO>> callback, Consumer<Throwable> failureCallback);


}
