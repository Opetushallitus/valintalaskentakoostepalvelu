package fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.impl;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.OppijantunnistusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensRequest;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensResponse;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import io.reactivex.Observable;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OppijantunnistusAsyncResourceImpl implements OppijantunnistusAsyncResource {
  private static final Logger LOG =
      LoggerFactory.getLogger(OppijantunnistusAsyncResourceImpl.class);

  private final RestCasClient restCasClient;

  private final UrlConfiguration urlConfiguration;

  public OppijantunnistusAsyncResourceImpl(
      @Qualifier("OppijantunnistusCasClient") RestCasClient restCasClient) {
    this.restCasClient = restCasClient;
    this.urlConfiguration = UrlConfiguration.getInstance();
  }

  @Override
  public Observable<TokensResponse> sendSecureLinks(TokensRequest tokensRequest) {
    LOG.info(
        "Sending securelinks to {} recipients.",
        tokensRequest.getApplicationOidToEmailAddress().size());
    return Observable.fromFuture(
        this.restCasClient.post(
            this.urlConfiguration.url("oppijan-tunnistus.tokens"),
            new com.google.gson.reflect.TypeToken<>() {},
            tokensRequest,
            Collections.emptyMap(),
            10 * 60 * 1000));
  }

  @Override
  public Observable<byte[]> previewSecureLink(TokensRequest tokensRequest) {
    return Observable.fromFuture(
        this.restCasClient.get(
            this.urlConfiguration.url(
                "oppijan-tunnistus.preview.haku.template.lang",
                tokensRequest.getHakuOid(),
                tokensRequest.getTemplatename(),
                tokensRequest.getLang()),
            new TypeToken<>() {},
            Map.of("Accept", "message/rfc822"),
            10 * 60 * 1000));
  }
}
