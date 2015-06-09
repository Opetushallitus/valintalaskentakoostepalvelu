package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta;

import java.util.List;
import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import rx.Observable;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ValintalaskentaAsyncResource {

	Peruutettava laske(LaskeDTO laskeDTO, Consumer<String> callback,
			Consumer<Throwable> failureCallback);

	@Deprecated
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

	Observable<List<ValintatietoValinnanvaiheDTO>> laskennantulokset(String hakukohdeOid);

	Observable<String> valintakokeet(LaskeDTO laskeDTO);
}
