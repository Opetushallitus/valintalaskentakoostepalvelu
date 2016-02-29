package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.HakemuksenVastaanottotila;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.PoistaVastaanottoDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoRecordDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ValintaTulosServiceAsyncResourceImpl extends HttpResource implements ValintaTulosServiceAsyncResource {

    @Autowired
    public ValintaTulosServiceAsyncResourceImpl(@Value("${valintalaskentakoostepalvelu.valintatulosservice.url:${host.ilb}}") String address) {
        super(address, TimeUnit.MINUTES.toMillis(30));
    }

    @Override
    public Observable<List<ValintaTulosServiceDto>> getHaunValintatulokset(String hakuOid) {
        return getAsObservable("/valinta-tulos-service/haku/" + hakuOid, new GenericType<List<ValintaTulosServiceDto>>() {}.getType());
    }

    @Override
    public Observable<ValintaTulosServiceDto> getHakemuksenValintatulos(String hakuOid, String hakemusOid) {
        return getAsObservable("/valinta-tulos-service/haku/" + hakuOid + "/hakemus/" + hakemusOid, ValintaTulosServiceDto.class);
    }

    @Override
    public Observable<List<HakemuksenVastaanottotila>> getVastaanottotilatByHakemus(String hakuOid, String hakukohdeOid) {
        String url = "/valinta-tulos-service/virkailija/haku/" + hakuOid + "/hakukohde/" + hakukohdeOid;
        return getAsObservable(url, new GenericType<List<HakemuksenVastaanottotila>>(){}.getType());
    }

    @Override
    public Observable<String> getHakemuksenValintatulosAsString(String hakuOid, String hakemusOid) {
        return getStringAsObservable("/valinta-tulos-service/haku/" + hakuOid + "/hakemus/" + hakemusOid);
    }

    @Override
    public Observable<List<Valintatulos>> findValintatulokset(String hakuOid, String hakukohdeOid) {
        return getAsObservable("/valinta-tulos-service/virkailija/valintatulos/haku/" + hakuOid + "/hakukohde/" + hakukohdeOid, new GenericType<List<Valintatulos>>() {}.getType());
    }

    @Override
    public Observable<List<VastaanottoRecordDTO>> hakukohteenVastaanotot(String hakukohdeOid) {
        return getAsObservable("/valinta-tulos-service/virkailija/vastaanotto/hakukohde/" + hakukohdeOid, new GenericType<List<VastaanottoRecordDTO>>() {}.getType());
    }

    @Override
    public Observable<Void> poista(PoistaVastaanottoDTO poistaVastaanottoDTO) {
        return postAsObservable("/valinta-tulos-service/virkailija/vastaanotto/poista", Void.class, Entity.json(poistaVastaanottoDTO));
    }

    @Override
    public Observable<List<VastaanottoResultDTO>> tallenna(List<VastaanottoRecordDTO> tallennettavat) {
        return postAsObservable("/valinta-tulos-service/virkailija/vastaanotto", new GenericType<List<VastaanottoResultDTO>>() {}.getType(), Entity.json(tallennettavat));
    }
}
