package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import rx.Observable;

public interface TarjontaAsyncResource {
	Observable<HakuV1RDTO> haeHaku(String hakuOid);

	Observable<HakukohdeV1RDTO> haeHakukohde(String hakukohdeOid);
}
