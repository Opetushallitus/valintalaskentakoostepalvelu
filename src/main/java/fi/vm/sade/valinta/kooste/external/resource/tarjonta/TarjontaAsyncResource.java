package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import rx.Observable;

import java.util.function.Consumer;

public interface TarjontaAsyncResource {
	Observable<HakuV1RDTO> haeHaku(String hakuOid);

	Observable<HakukohdeDTO> haeHakukohde(String hakukohdeOid);

	Peruutettava haeHaku(String hakuOid, Consumer<HakuV1RDTO> callback, Consumer<Throwable> failureCallback);
}
