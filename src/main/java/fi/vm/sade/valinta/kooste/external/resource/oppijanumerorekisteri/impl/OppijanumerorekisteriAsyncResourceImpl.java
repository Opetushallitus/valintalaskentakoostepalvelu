package fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.impl;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloViiteDto;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import io.reactivex.Observable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class OppijanumerorekisteriAsyncResourceImpl extends UrlConfiguredResource
    implements OppijanumerorekisteriAsyncResource {
  private final RestCasClient client;

  @Autowired
  public OppijanumerorekisteriAsyncResourceImpl(
      @Qualifier("OppijanumerorekisteriServiceRestClientCasInterceptor")
          AbstractPhaseInterceptor casInterceptor,
      @Qualifier("OppijanumerorekisteriCasClient") RestCasClient client) {
    super(TimeUnit.HOURS.toMillis(1), casInterceptor);
    this.client = client;
  }

  public Observable<List<HenkiloPerustietoDto>> haeTaiLuoHenkilot(
      List<HenkiloCreateDTO> henkiloPrototyypit) {
    return postAsObservableLazily(
        getUrl("oppijanumerorekisteri-service.s2s.henkilo.findOrCreateMultiple"),
        new GenericType<List<HenkiloPerustietoDto>>() {}.getType(),
        Entity.entity(gson().toJson(henkiloPrototyypit), MediaType.APPLICATION_JSON_TYPE),
        ACCEPT_JSON);
  }

  public CompletableFuture<List<HenkiloViiteDto>> haeHenkiloOidDuplikaatit(Set<String> personOids) {
    String url = getUrl("oppijanumerorekisteri-service.s2s.duplicatesByPersonOids");
    Map<String, Set<String>> henkiloSearchParams = new HashMap<>();
    henkiloSearchParams.put("henkiloOids", personOids);
    CompletableFuture<List<HenkiloViiteDto>> fut =
        this.client.post(
            url,
            new TypeToken<List<HenkiloViiteDto>>() {},
            henkiloSearchParams,
            Collections.emptyMap(),
            60 * 60 * 1000);
    return fut;
  }

  public CompletableFuture<Map<String, HenkiloPerustietoDto>> haeHenkilot(List<String> personOids) {
    String url = getUrl("oppijanumerorekisteri-service.henkilo.masterHenkilosByOidList");
    return CompletableFutureUtil.sequence(
            Lists.partition(personOids, 5000).stream()
                .map(
                    chunk ->
                        this.client.post(
                            url,
                            new TypeToken<Map<String, HenkiloPerustietoDto>>() {},
                            chunk,
                            Map.of("Content-Type", "application/json"),
                            60 * 60 * 1000))
                .collect(Collectors.toList()))
        .thenApplyAsync(
            chunks ->
                chunks.stream()
                    .flatMap(m -> m.entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }
}
