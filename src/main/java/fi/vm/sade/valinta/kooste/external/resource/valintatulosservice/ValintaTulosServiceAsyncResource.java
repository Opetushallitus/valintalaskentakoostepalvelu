package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice;

import java.util.List;
import java.util.concurrent.Future;

import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import java.util.function.Consumer;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ValintaTulosServiceAsyncResource {

	Future<List<ValintaTulosServiceDto>> getValintatulokset(String hakuOid);


	void getValintatulokset(String hakuOid, String hakukohdeOid,
							Consumer<List<ValintaTulosServiceDto>> vts,
							Consumer<Throwable> poikkeus);
}
