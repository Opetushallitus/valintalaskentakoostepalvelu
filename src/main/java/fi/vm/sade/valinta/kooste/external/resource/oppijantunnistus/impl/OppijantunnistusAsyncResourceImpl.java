package fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.impl;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.OppijantunnistusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensRequest;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensResponse;
import io.reactivex.Observable;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

public class OppijantunnistusAsyncResourceImpl extends UrlConfiguredResource implements OppijantunnistusAsyncResource {
  private static final Logger LOG = LoggerFactory.getLogger(OppijantunnistusAsyncResourceImpl.class);
  private static final MediaType EML_TYPE = new MediaType("message", "rfc822");

  public OppijantunnistusAsyncResourceImpl(
      @Qualifier("OppijantunnistusCasInterceptor") AbstractPhaseInterceptor casInterceptor) {
    super(TimeUnit.MINUTES.toMillis(20), casInterceptor);
  }

  @Override
  public Observable<TokensResponse> sendSecureLinks(TokensRequest tokensRequest) {
    LOG.info("Sending securelinks to {} recipients.", tokensRequest.getApplicationOidToEmailAddress().size());
    return postAsObservableLazily(getUrl("oppijan-tunnistus.tokens"), new TypeToken<TokensResponse>() {
    }.getType(), Entity.entity(gson().toJson(tokensRequest), MediaType.APPLICATION_JSON_TYPE), client -> {
      client.accept(MediaType.APPLICATION_JSON_TYPE);
      return client;
    });
  }

  @Override
  public Observable<Response> previewSecureLink(TokensRequest tokensRequest) {
    return getAsObservableLazily(getUrl("oppijan-tunnistus.preview.haku.template.lang", tokensRequest.getHakuOid(),
        tokensRequest.getTemplatename(), tokensRequest.getLang()), client -> {
          client.accept(EML_TYPE);
          client.query("callback-url", tokensRequest.getUrl());
          return client;
        });
  }
}
