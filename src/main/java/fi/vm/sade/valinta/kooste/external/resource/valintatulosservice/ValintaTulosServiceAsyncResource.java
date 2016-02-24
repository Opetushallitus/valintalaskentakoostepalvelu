package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice;

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.HakemuksenVastaanottotila;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.PoistaVastaanottoDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoRecordDTO;
import rx.Observable;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

public interface ValintaTulosServiceAsyncResource {
    Observable<List<ValintaTulosServiceDto>> getHaunValintatulokset(String hakuOid);

    Observable<ValintaTulosServiceDto> getHakemuksenValintatulos(String hakuOid, String hakemusOid);

    Observable<String> getHakemuksenValintatulosAsString(String hakuOid, String hakemusOid);

    Observable<List<VastaanottoRecordDTO>> hakukohteenVastaanotot(String hakukohdeOid);

    Observable<Void> poista(PoistaVastaanottoDTO poistaVastaanottoDTO);

    Observable<Void> tallenna(List<VastaanottoRecordDTO> tallennettavat);

    Observable<List<HakemuksenVastaanottotila>> getVastaanottotilatByHakemus(String hakuOid, String hakukohdeOid);
}
