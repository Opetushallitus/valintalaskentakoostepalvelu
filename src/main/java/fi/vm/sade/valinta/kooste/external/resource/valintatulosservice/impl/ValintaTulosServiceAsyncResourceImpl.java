package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl;

import java.util.List;

import javax.ws.rs.core.GenericType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import java.util.concurrent.TimeUnit;
import rx.Observable;

@Service
public class ValintaTulosServiceAsyncResourceImpl extends HttpResource implements ValintaTulosServiceAsyncResource {

    @Autowired
    public ValintaTulosServiceAsyncResourceImpl(@Value("${host.ilb}") String address) {
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
    public Observable<String> getHakemuksenValintatulosAsString(String hakuOid, String hakemusOid) {
        return getStringAsObservable("/valinta-tulos-service/haku/" + hakuOid + "/hakemus/" + hakemusOid);
    }
}
