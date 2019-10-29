package fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.impl;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OppijanumerorekisteriAsyncResourceImpl extends UrlConfiguredResource implements OppijanumerorekisteriAsyncResource {
    private final HttpClient client;

    @Autowired
    public OppijanumerorekisteriAsyncResourceImpl(
            @Qualifier("OppijanumerorekisteriServiceRestClientCasInterceptor") AbstractPhaseInterceptor casInterceptor,
            @Qualifier("OppijanumerorekisteriHttpClient") HttpClient client
            ) {
        super(TimeUnit.HOURS.toMillis(1), casInterceptor);
        this.client = client;
    }

    public Observable<List<HenkiloPerustietoDto>> haeTaiLuoHenkilot(List<HenkiloCreateDTO> henkiloPrototyypit) {
        return postAsObservableLazily(getUrl("oppijanumerorekisteri-service.s2s.henkilo.findOrCreateMultiple"),
            new GenericType<List<HenkiloPerustietoDto>>() {}.getType(),
            Entity.entity(gson().toJson(henkiloPrototyypit), MediaType.APPLICATION_JSON_TYPE),
            ACCEPT_JSON);
    }

    public CompletableFuture<Map<String, HenkiloPerustietoDto>> haeHenkilot(List<String> personOids) {
        String url = getUrl("oppijanumerorekisteri-service.henkilo.masterHenkilosByOidList");
        return CompletableFutureUtil.sequence(Lists.partition(personOids, 5000).stream()
                .map(chunk -> this.client.<List<String>, Map<String, HenkiloPerustietoDto>>postJson(
                        url,
                        Duration.ofHours(1),
                        chunk,
                        new TypeToken<List<String>>() {}.getType(),
                        new TypeToken<Map<String, HenkiloPerustietoDto>>() {}.getType()
                ))
                .collect(Collectors.toList()))
                .thenApplyAsync(chunks -> chunks.stream()
                        .flatMap(m -> m.entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
}
