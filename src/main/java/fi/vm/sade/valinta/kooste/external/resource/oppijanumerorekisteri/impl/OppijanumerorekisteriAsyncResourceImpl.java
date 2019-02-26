package fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.impl;

import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class OppijanumerorekisteriAsyncResourceImpl extends UrlConfiguredResource implements OppijanumerorekisteriAsyncResource {
    @Autowired
    public OppijanumerorekisteriAsyncResourceImpl(
            @Qualifier("OppijanumerorekisteriServiceRestClientCasInterceptor") AbstractPhaseInterceptor casInterceptor) {
        super(TimeUnit.HOURS.toMillis(1), casInterceptor);
    }

    public Observable<List<HenkiloPerustietoDto>> haeTaiLuoHenkilot(List<HenkiloCreateDTO> henkiloPrototyypit) {
        return postAsObservableLazily(getUrl("oppijanumerorekisteri-service.s2s.henkilo.findOrCreateMultiple"),
            new GenericType<List<HenkiloPerustietoDto>>() {}.getType(),
            Entity.entity(gson().toJson(henkiloPrototyypit), MediaType.APPLICATION_JSON_TYPE),
            ACCEPT_JSON);
    }

    public Single<Map<String, HenkiloPerustietoDto>> haeHenkilot(List<String> personOids) {
        String url = getUrl("oppijanumerorekisteri-service.henkilo.masterHenkilosByOidList");
        return Observable.fromIterable(personOids)
                .window(5000)
                .flatMap(chunk -> chunk.toList().toObservable())
                .flatMap(oids -> this.<List<String>, Map<String, HenkiloPerustietoDto>>postAsObservableLazily(url,
                        new GenericType<Map<String, HenkiloPerustietoDto>>() { }.getType(),
                        Entity.entity(oids, MediaType.APPLICATION_JSON_TYPE),
                        ACCEPT_JSON))
                .collectInto(new HashMap<>(personOids.size()), Map::putAll);
    }
}
