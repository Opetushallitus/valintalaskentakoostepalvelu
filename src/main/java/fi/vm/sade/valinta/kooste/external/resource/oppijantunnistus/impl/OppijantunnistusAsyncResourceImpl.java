package fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.impl;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.OppijantunnistusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensRequest;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

public class OppijantunnistusAsyncResourceImpl extends HttpResource implements OppijantunnistusAsyncResource {
    private final static Logger LOG = LoggerFactory.getLogger(OppijantunnistusAsyncResourceImpl.class);
    private final static MediaType EML_TYPE = new MediaType("message", "rfc822");

    @Autowired
    public OppijantunnistusAsyncResourceImpl(@Value("${valintalaskentakoostepalvelu.oppijantunnistus.rest.url}") String address) {
        super(address, TimeUnit.MINUTES.toMillis(20));
    }

    @Override
    public Observable<TokensResponse> sendSecureLinks(TokensRequest tokensRequest) {
        LOG.info("Sending securelinks to {} recipients.", tokensRequest.getApplicationOidToEmailAddress().size());
        return postAsObservable(
                "/api/v1/tokens",
                new TypeToken<TokensResponse>(){
                }.getType(),
                Entity.entity(gson().toJson(tokensRequest), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<Response> previewSecureLink(TokensRequest tokensRequest) {
        return getAsObservable(
                "/api/v1/preview/haku/" + tokensRequest.getHakuOid() + "/template/" + tokensRequest.getTemplatename() + "/lang/" + tokensRequest.getLang(),
                client -> {
                    client.accept(EML_TYPE);
                    client.query("callback-url", tokensRequest.getUrl());
                    return client;
                }
        );
    }
}
