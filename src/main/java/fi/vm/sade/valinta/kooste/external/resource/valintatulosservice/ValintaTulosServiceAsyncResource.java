package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice;

import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import rx.Observable;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ValintaTulosServiceAsyncResource {

	Observable<List<ValintaTulosServiceDto>> getHaunValintatulokset(String hakuOid);

	Observable<ValintaTulosServiceDto> getHakemuksenValintatulos(String hakuOid, String hakemusOid);

	Observable<String> getHakemuksenValintatulosAsString(String hakuOid, String hakemusOid);
}
