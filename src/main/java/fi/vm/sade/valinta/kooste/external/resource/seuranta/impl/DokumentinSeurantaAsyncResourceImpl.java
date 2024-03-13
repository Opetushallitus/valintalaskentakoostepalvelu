package fi.vm.sade.valinta.kooste.external.resource.seuranta.impl;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.DokumentinSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.seuranta.dto.DokumenttiDto;
import fi.vm.sade.valinta.seuranta.dto.VirheilmoitusDto;
import io.reactivex.Observable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.asynchttpclient.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DokumentinSeurantaAsyncResourceImpl implements DokumentinSeurantaAsyncResource {
  private final RestCasClient restCasClient;

  private final UrlConfiguration urlConfiguration;

  @Autowired
  public DokumentinSeurantaAsyncResourceImpl(
      @Qualifier("SeurantaCasClient") RestCasClient restCasClient) {
    this.restCasClient = restCasClient;
    this.urlConfiguration = UrlConfiguration.getInstance();
  }

  public Observable<DokumenttiDto> paivitaDokumenttiId(String key, String dokumenttiId) {
    return Observable.fromFuture(
        this.restCasClient.postPlaintext(
            this.urlConfiguration.url(
                "seuranta-service.dokumentinseuranta.paivitadokumenttiid", key),
            new TypeToken<DokumenttiDto>() {},
            dokumenttiId,
            Map.of("Content-Type", "text/plain"),
            10 * 60 * 1000));
  }

  public Observable<String> luoDokumentti(String kuvaus) {
    return Observable.fromFuture(
        this.restCasClient.postPlaintext(
            this.urlConfiguration.url("seuranta-service.dokumentinseuranta"),
            kuvaus,
            Map.of("Content-Type", "text/plain"),
            10 * 60 * 1000).thenApply(Response::getResponseBody));
  }

  public Observable<DokumenttiDto> paivitaKuvaus(String key, String kuvaus) {
    return Observable.fromFuture(
        this.restCasClient.postPlaintext(
            this.urlConfiguration.url("seuranta-service.dokumentinseuranta.paivitakuvaus", key),
            new TypeToken<DokumenttiDto>() {},
            kuvaus,
            Map.of("Content-Type", "text/plain"),
            10 * 60 * 1000));
  }

  public Observable<DokumenttiDto> lisaaVirheilmoituksia(
      String key, List<VirheilmoitusDto> virheilmoitukset) {
    return Observable.fromFuture(
        this.restCasClient.post(
            this.urlConfiguration.url("seuranta-service.dokumentinseuranta.lisaavirheita", key),
            new TypeToken<DokumenttiDto>() {},
            virheilmoitukset,
            Collections.emptyMap(),
            10 * 60 * 1000));
  }
}
