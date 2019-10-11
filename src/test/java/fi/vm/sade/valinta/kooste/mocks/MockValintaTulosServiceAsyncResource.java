package fi.vm.sade.valinta.kooste.mocks;

import static java.util.Collections.emptyList;
import com.google.common.collect.Lists;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.ValintatulosUpdateStatus;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Lukuvuosimaksu;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.LukuvuosimaksuMuutos;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Muutoshistoria;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Valinnantulos;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.TilaHakijalleDto;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoAikarajaMennytDTO;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
    public Observable<String> getHakemuksenValintatulosAsString(String hakuOid, String hakemusOid) {
        return Observable.just("{}");
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
    public Observable<String> saveLukuvuosimaksut(String hakukohdeOid, AuditSession session, List<LukuvuosimaksuMuutos> muutokset) {
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
        /*return Observable.just(
            valinnantulokset.stream().map(v -> {
                return new ValintatulosUpdateStatus(200, "Kaikki ok", valintatapajonoOid, v.getHakemusOid());
            }).collect(Collectors.toList())
        );*/
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
    public CompletableFuture<List<HakijaDTO>> getKoulutuspaikalliset(String hakuOid, String hakukohdeOid) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public CompletableFuture<List<HakijaDTO>> getKoulutuspaikalliset(String hakuOid) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public CompletableFuture<HakijaDTO> getHakijaByHakemus(String hakuOid, String hakemusOid) {
        return CompletableFuture.completedFuture(new HakijaDTO());
    }

    @Override
    public CompletableFuture<List<HakijaDTO>> getKaikkiHakijat(String hakuOid, String hakukohdeOid) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public CompletableFuture<List<HakijaDTO>> getHakijatIlmanKoulutuspaikkaa(String hakuOid) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }
}
