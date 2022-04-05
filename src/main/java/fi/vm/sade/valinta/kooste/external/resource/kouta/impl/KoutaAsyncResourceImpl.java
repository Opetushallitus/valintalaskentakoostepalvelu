package fi.vm.sade.valinta.kooste.external.resource.kouta.impl;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.kouta.KoutaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.kouta.KoutaHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.kouta.dto.KoutaHakukohdeDTO;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class KoutaAsyncResourceImpl implements KoutaAsyncResource {
  private final UrlConfiguration urlConfiguration = UrlConfiguration.getInstance();
  private final HttpClient koutaClient;

  @Autowired
  public KoutaAsyncResourceImpl(@Qualifier("KoutaHttpClient") HttpClient koutaClient) {
    this.koutaClient = koutaClient;
  }

  @Override
  public CompletableFuture<KoutaHakukohde> haeHakukohde(String hakukohdeOid) {
    CompletableFuture<KoutaHakukohdeDTO> koutaF =
        this.koutaClient.getJson(
            urlConfiguration.url("kouta-internal.hakukohde.hakukohdeoid", hakukohdeOid),
            Duration.ofSeconds(10),
            new TypeToken<KoutaHakukohdeDTO>() {
            }.getType());
    return koutaF.thenApplyAsync(KoutaHakukohde::new);
  }

}
