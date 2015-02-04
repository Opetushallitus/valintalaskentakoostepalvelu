package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;

public interface TarjontaAsyncResource {

	// @GET
	// @Path("/{oid}")
	// @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	// @ApiOperation(value = "Palauttaa haun annetulla oid:lla", notes =
	// "Palauttaa haun annetulla oid:lla", response = HakuV1RDTO.class)
	//
	Future<HakuV1RDTO> haeHaku(String hakuOid);
	
	Future<HakukohdeDTO> haeHakukohde(String hakukohdeOid);

	Peruutettava haeHakukohde(String hakuOid, String hakukohdeOid, Consumer<HakukohdeDTO> callback, Consumer<Throwable> failureCallback);
}
