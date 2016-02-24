package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice;

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.HakemuksenVastaanottotila;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import rx.Observable;

import java.util.List;
import java.util.Map;

public interface ValintaTulosServiceAsyncResource {
    Observable<List<ValintaTulosServiceDto>> getHaunValintatulokset(String hakuOid);

    Observable<ValintaTulosServiceDto> getHakemuksenValintatulos(String hakuOid, String hakemusOid);

    Observable<String> getHakemuksenValintatulosAsString(String hakuOid, String hakemusOid);

    Observable<List<HakemuksenVastaanottotila>> getVastaanottotilatByHakemus(String hakuOid, String hakukohdeOid);
}
