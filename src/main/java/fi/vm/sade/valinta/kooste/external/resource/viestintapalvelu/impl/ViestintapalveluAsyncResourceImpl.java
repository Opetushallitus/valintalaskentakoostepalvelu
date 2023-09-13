package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.impl;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto.LetterBatchCountDto;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;
import io.reactivex.Observable;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ViestintapalveluAsyncResourceImpl implements ViestintapalveluAsyncResource {
  private final RestCasClient client;

  private final UrlConfiguration urlConfiguration;

  @Autowired
  public ViestintapalveluAsyncResourceImpl(
      @Qualifier("ViestintapalveluCasClient") RestCasClient client) {
    this.client = client;
    this.urlConfiguration = UrlConfiguration.getInstance();
  }

  @Override
  public CompletableFuture<LetterResponse> vieLetterBatch(LetterBatch letterBatch) {
    return this.client.post(
        this.urlConfiguration.url("viestintapalvelu.letter.async.letter"),
        new TypeToken<LetterResponse>() {},
        letterBatch,
        Collections.emptyMap(),
        20 * 60 * 60 * 1000);
  }

  @Override
  public CompletableFuture<LetterBatchStatusDto> haeLetterBatchStatus(String letterBatchId) {
    return this.client.get(
        this.urlConfiguration.url("viestintapalvelu.letter.async.letter.status", letterBatchId),
        new TypeToken<LetterBatchStatusDto>() {},
        Collections.emptyMap(),
        69 * 1000);
  }

  public Observable<LetterBatchCountDto> haeTuloskirjeenMuodostuksenTilanne(
      String hakuOid, String tyyppi, String kieli) {
    return Observable.fromFuture(
        this.client.get(
            this.urlConfiguration.url(
                "viestintapalvelu.luotettu.letter.count.type.language", hakuOid, tyyppi, kieli),
            new TypeToken<LetterBatchCountDto>() {},
            Collections.emptyMap(),
            10 * 1000));
  }

  @Override
  public CompletableFuture<List<TemplateHistory>> haeKirjepohja(
      String hakuOid,
      String tarjoajaOid,
      String templateName,
      String languageCode,
      String hakukohdeOid) {
    HashMap<String, String> query = new HashMap<>();
    query.put("applicationPeriod", hakuOid);
    query.put("oid", tarjoajaOid);
    query.put("templateName", templateName);
    query.put("languageCode", languageCode);
    query.put("tag", hakukohdeOid);
    return this.client.get(
        this.urlConfiguration.url("viestintapalvelu.template.gethistory", query),
        new TypeToken<List<TemplateHistory>>() {},
        Collections.emptyMap(),
        60 * 1000);
  }

  @Override
  public Observable<InputStream> haeOsoitetarrat(Osoitteet osoitteet) {
    return Observable.fromFuture(
        this.client
            .post(
                this.urlConfiguration.url("viestintapalvelu.addresslabel.sync.pdf"),
                osoitteet,
                Map.of("Accept", "application/octet-stream"),
                10 * 60 * 1000 // TODO: mikä on oikea timeout?
                )
            .thenApply(r -> r.getResponseBodyAsStream()));
  }

  @Override
  public Observable<Optional<Long>> haeKirjelahetysEPostille(
      String hakuOid, String kirjeenTyyppi, String asiointikieli) {
    return haeKirjelahetys(
        this.urlConfiguration.url("viestintapalvelu.luotettu.letter.getbatchidreadyforeposti"),
        hakuOid,
        kirjeenTyyppi,
        asiointikieli);
  }

  @Override
  public Observable<Optional<Long>> haeKirjelahetysJulkaistavaksi(
      String hakuOid, String kirjeenTyyppi, String asiointikieli) {
    return haeKirjelahetys(
        this.urlConfiguration.url("viestintapalvelu.luotettu.letter.getbatchidreadyforpublish"),
        hakuOid,
        kirjeenTyyppi,
        asiointikieli);
  }

  private Observable<Optional<Long>> haeKirjelahetys(
      String url, String hakuOid, String kirjeenTyyppi, String asiointikieli) {
    return Observable.fromFuture(
        this.client
            .get(
                url
                    + "?hakuoid="
                    + hakuOid
                    + "&type="
                    + kirjeenTyyppi
                    + "&language="
                    + asiointikieli,
                new TypeToken<String>() {},
                Map.of("Accept", "text/plain"),
                10 * 60 * 1000 // TODO: mikä on oikea timeout?
                )
            .thenApply(
                batchIdAsString ->
                    StringUtils.isNumeric(batchIdAsString)
                        ? Optional.of(Long.parseLong(batchIdAsString))
                        : Optional.empty()));
  }

  @Override
  public Observable<Optional<Long>> julkaiseKirjelahetys(Long batchId) {
    return Observable.fromFuture(
        this.client
            .get(
                this.urlConfiguration.url(
                    "viestintapalvelu.luotettu.letter.publishletterbatch", batchId),
                new TypeToken<String>() {},
                Map.of("Accept", "text/plain"),
                10 * 60 * 1000 // TODO: mikä on oikea timeout?
                )
            .thenApply(
                batchIdAsString ->
                    StringUtils.isNumeric(batchIdAsString)
                        ? Optional.of(Long.parseLong(batchIdAsString))
                        : Optional.empty()));
  }

  @Override
  public Observable<Map<String, String>> haeEPostiOsoitteet(Long batchId) {
    return Observable.fromFuture(
        this.client.get(
            this.urlConfiguration.url(
                "viestintapalvelu.luotettu.letter.getepostiadressesforletterbatch", batchId),
            new TypeToken<Map<String, String>>() {},
            Collections.emptyMap(),
            10 * 60 * 1000 // TODO: mikä on oikea timeout
            ));
  }
}
