package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import java.util.function.Consumer;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;

public interface HakuV1AsyncResource {

	// @GET
	// @Path("/{oid}")
	// @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	// @ApiOperation(value = "Palauttaa haun annetulla oid:lla", notes =
	// "Palauttaa haun annetulla oid:lla", response = HakuV1RDTO.class)
	//
	Peruutettava findByOid(String oid,
			Consumer<ResultV1RDTO<HakuV1RDTO>> callback,
			Consumer<Throwable> failureCallback);
}
