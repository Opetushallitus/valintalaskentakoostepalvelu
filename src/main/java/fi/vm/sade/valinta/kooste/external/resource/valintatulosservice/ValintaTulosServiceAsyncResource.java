package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.*;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.PoistaVastaanottoDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.TilaHakijalleDto;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoAikarajaMennytDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoRecordDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoResultDTO;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import rx.Observable;

import java.util.List;
import java.util.Set;

public interface ValintaTulosServiceAsyncResource {
    DateTimeFormatter valintaTulosServiceCompatibleFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC();

    Observable<List<ValintaTulosServiceDto>> getHaunValintatulokset(String hakuOid);

    Observable<ValintaTulosServiceDto> getHakemuksenValintatulos(String hakuOid, String hakemusOid);

    Observable<String> getHakemuksenValintatulosAsString(String hakuOid, String hakemusOid);

    Observable<List<VastaanottoRecordDTO>> hakukohteenVastaanotot(String hakukohdeOid);

    Observable<Void> poista(PoistaVastaanottoDTO poistaVastaanottoDTO);

    Observable<List<VastaanottoResultDTO>> tallenna(List<VastaanottoRecordDTO> tallennettavat);

    Observable<List<HakemuksenVastaanottotila>> getVastaanottotilatByHakemus(String hakuOid, String hakukohdeOid);

    Observable<List<Valintatulos>> findValintatulokset(String hakuOid, String hakukohdeOid);

    Observable<List<Valintatulos>> findValintatuloksetIlmanHakijanTilaa(String hakuOid, String hakukohdeOid);

    Observable<List<Valintatulos>> findValintatuloksetByHakemus(String hakuOid, String hakemusOid);

    Observable<List<VastaanottoAikarajaMennytDTO>> findVastaanottoAikarajaMennyt(String hakuOid, String hakukohdeOid, Set<String> hakemusOids);

    Observable<List<TilaHakijalleDto>> findTilahakijalle(String hakuOid, String hakukohdeOid, String valintatapajonoOid, Set<String> hakemusOids);

    Observable<List<ValinnantulosUpdateStatus>> postErillishaunValinnantulokset(AuditSession auditSession, String valintatapajonoOid, List<Valinnantulos> valinnantulokset);

    Observable<List<Valinnantulos>> getErillishaunValinnantulokset(AuditSession auditSession, String valintatapajonoOid);
}
