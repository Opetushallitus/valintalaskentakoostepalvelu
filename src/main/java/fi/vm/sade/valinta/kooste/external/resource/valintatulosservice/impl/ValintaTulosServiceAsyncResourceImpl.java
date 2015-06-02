package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.GenericType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class ValintaTulosServiceAsyncResourceImpl extends HttpResource implements ValintaTulosServiceAsyncResource {

    @Autowired
    public ValintaTulosServiceAsyncResourceImpl(@Value("${host.ilb}") String address) {
        super(address);
    }

    @Override
    public Observable<List<ValintaTulosServiceDto>> getHaunValintatulokset(String hakuOid) {
        return getAsObservable("/valinta-tulos-service/haku/" + hakuOid, new GenericType<List<ValintaTulosServiceDto>>() {
        }.getType());
    }

    @Override
    public Observable<ValintaTulosServiceDto> getHakemuksenValintatulos(String hakuOid, String hakemusOid) {
        return getAsObservable("/valinta-tulos-service/haku/" + hakuOid + "/hakemus/" + hakemusOid, ValintaTulosServiceDto.class);
    }

    @Override
    public Observable<String> getHakemuksenValintatulosAsString(String hakuOid, String hakemusOid) {
        return getAsObservable("/valinta-tulos-service/haku/" + hakuOid + "/hakemus/" + hakemusOid)
                .flatMap(response -> {
                    try {
                        return Observable.just(StringUtils.trimToEmpty(IOUtils.toString((InputStream) response.getEntity())));
                    } catch (IOException e) {
                        return Observable.error(e);
                    }
                });
    }
}
