package fi.vm.sade.valinta.kooste.external.resource.suorituspalvelu.impl;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakemuksenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.suorituspalvelu.SuorituspalveluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SuorituspalveluAsyncResourceImpl implements SuorituspalveluAsyncResource {
  // Suorituspalvelu palauttaa tiedot korkeintaan 5000 hakemukselle kerrallaan
  public static final int HAKEMUKSIA_KERRALLAAN = 5000;

  private final RestCasClient client;

  private final UrlConfiguration urlConfiguration;

  @Autowired
  public SuorituspalveluAsyncResourceImpl(
      @Qualifier("SuorituspalveluCasClient") RestCasClient client) {
    this.client = client;
    this.urlConfiguration = UrlConfiguration.getInstance();
  }

  @Override
  public CompletableFuture<List<HakemuksenHarkinnanvaraisuus>> haeHarkinnanvaraisuudet(
      List<String> hakemusOids) {
    if (hakemusOids.isEmpty()) {
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
    String url = this.urlConfiguration.url("suorituspalvelu.valinnat.harkinnanvaraisuus");
    return CompletableFutureUtil.sequence(
            Lists.partition(hakemusOids, HAKEMUKSIA_KERRALLAAN).stream()
                .map(
                    chunk ->
                        this.client.post(
                            url,
                            new TypeToken<List<HakemuksenHarkinnanvaraisuus>>() {},
                            Map.of("hakemusOids", chunk),
                            Collections.emptyMap(),
                            20 * 60 * 1000))
                .collect(Collectors.toList()))
        .thenApply(chunks -> chunks.stream().flatMap(List::stream).collect(Collectors.toList()));
  }
}
