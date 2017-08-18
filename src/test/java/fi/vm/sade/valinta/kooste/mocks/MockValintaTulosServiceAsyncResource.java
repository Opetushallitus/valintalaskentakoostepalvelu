package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.collect.Lists;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.ValintatulosUpdateStatus;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.*;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.PoistaVastaanottoDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.TilaHakijalleDto;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoAikarajaMennytDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoRecordDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoResultDTO;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.*;
import static javax.ws.rs.core.Response.Status.OK;

@Service
public class MockValintaTulosServiceAsyncResource implements ValintaTulosServiceAsyncResource {

    @Override
    public Observable<List<ValintaTulosServiceDto>> getHaunValintatulokset(String hakuOid) {
        return Observable.just(Lists.newArrayList());
    }
    @Override
    public Observable<List<Muutoshistoria>> getMuutoshistoria(String hakemusOid, String valintatapajonoOid) {
        return Observable.just(emptyList());
    }

    @Override
    public Observable<ValintaTulosServiceDto> getHakemuksenValintatulos(String hakuOid, String hakemusOid) {
        return Observable.just(new ValintaTulosServiceDto());
    }

    @Override
    public Observable<String> getHakemuksenValintatulosAsString(String hakuOid, String hakemusOid) {
        return Observable.just("{}");
    }

    @Override
    public Observable<List<VastaanottoRecordDTO>> hakukohteenVastaanotot(String hakukohdeOid) {
        return Observable.just(Lists.newArrayList());
    }

    @Override
    public Observable<Void> poista(PoistaVastaanottoDTO poistaVastaanottoDTO) {
        return Observable.empty();
    }

    @Override
    public Observable<List<VastaanottoResultDTO>> tallenna(List<VastaanottoRecordDTO> tallennettavat) {
        return Observable.just(tallennettavat.stream().map(v -> {
            VastaanottoResultDTO dto = new VastaanottoResultDTO();
            dto.setHakemusOid(v.getHakemusOid());
            dto.setHakukohdeOid(v.getHakukohdeOid());
            dto.setHenkiloOid(v.getHenkiloOid());
            VastaanottoResultDTO.Result result = new VastaanottoResultDTO.Result();
            result.setStatus(OK.getStatusCode());
            dto.setResult(result);
            return dto;
        }).collect(Collectors.toList()));
    }

    @Override
    public Observable<List<HakemuksenVastaanottotila>> getVastaanottotilatByHakemus(String hakuOid, String hakukohdeOid) {
        return Observable.just(Lists.newArrayList());
    }

    @Override
    public Observable<List<Valintatulos>> findValintatulokset(String hakuOid, String hakukohdeOid) {
        return Observable.just(Lists.newArrayList());
    }

    @Override
    public Observable<List<Lukuvuosimaksu>> fetchLukuvuosimaksut(String hakukohdeOid, AuditSession session) {
        return Observable.just(Lists.newArrayList());
    }

    @Override
    public Observable<Void> saveLukuvuosimaksut(String hakukohdeOid, AuditSession session, List<LukuvuosimaksuMuutos> muutokset) {
        return Observable.empty();
    }


    @Override
    public Observable<List<Valintatulos>> findValintatuloksetIlmanHakijanTilaa(String hakuOid, String hakukohdeOid) {
        return Observable.just(Lists.newArrayList());
    }

    @Override
    public Observable<List<Valintatulos>> findValintatuloksetByHakemus(String hakuOid, String hakemusOid) {
        return Observable.just(Lists.newArrayList());
    }

    @Override
    public Observable<List<VastaanottoAikarajaMennytDTO>> findVastaanottoAikarajaMennyt(String hakuOid, String hakukohdeOid, Set<String> hakemusOids) {
        return Observable.just(Lists.newArrayList());
    }

    @Override
    public Observable<List<TilaHakijalleDto>> findTilahakijalle(String hakuOid, String hakukohdeOid, String valintatapajonoOid, Set<String> hakemusOids) {
        return Observable.just(Lists.newArrayList());
    }

    public Map<String,List<Valinnantulos>> erillishaunValinnantulokset = new HashMap<>();

    @Override
    public Observable<List<ValintatulosUpdateStatus>> postErillishaunValinnantulokset(AuditSession auditSession, String valintatapajonoOid, List<Valinnantulos> valinnantulokset) {
        erillishaunValinnantulokset.put(valintatapajonoOid, valinnantulokset);
        return Observable.just(Lists.newArrayList());
    }

    @Override
    public Observable<List<Valinnantulos>> getErillishaunValinnantulokset(AuditSession auditSession, String valintatapajonoOid) {
        return Observable.just(Lists.newArrayList());
    }

    @Override
    public Observable<HakukohdeDTO> getHakukohdeBySijoitteluajoPlainDTO(String hakuOid, String hakukohdeOid) {
        return Observable.just(new HakukohdeDTO());
    }

    @Override
    public Observable<HakijaPaginationObject> getKoulutuspaikalliset(String hakuOid, String hakukohdeOid) {
        return Observable.just(new HakijaPaginationObject());
    }

    @Override
    public Observable<HakijaPaginationObject> getKoulutuspaikalliset(String hakuOid) {
        return Observable.just(new HakijaPaginationObject());
    }

    @Override
    public Observable<HakijaDTO> getHakijaByHakemus(String hakuOid, String hakemusOid) {
        return Observable.just(new HakijaDTO());
    }

    @Override
    public Observable<HakijaPaginationObject> getKaikkiHakijat(String hakuOid, String hakukohdeOid) {
        return Observable.just(new HakijaPaginationObject());
    }

    @Override
    public Observable<HakijaPaginationObject> getHakijatIlmanKoulutuspaikkaa(String hakuOid) {
        return Observable.just(new HakijaPaginationObject());
    }
}
