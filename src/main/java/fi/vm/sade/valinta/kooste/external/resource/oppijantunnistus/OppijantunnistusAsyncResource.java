package fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus;

import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensRequest;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensResponse;
import io.reactivex.Observable;
import javax.ws.rs.core.Response;

public interface OppijantunnistusAsyncResource {

  Observable<TokensResponse> sendSecureLinks(TokensRequest tokensRequest);

  Observable<Response> previewSecureLink(TokensRequest tokensRequest);
}
