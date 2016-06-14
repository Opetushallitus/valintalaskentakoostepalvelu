package fi.vm.sade.valinta.kooste.proxy.resource.viestintapalvelu;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

@Controller("ViestintapalveluProxyResource")
@Path("/proxy/viestintapalvelu")
public class ViestintapalveluProxyResource {

    @Autowired
    private ViestintapalveluAsyncResource viestintapalveluAsyncResource;

    @GET
    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @Path("/count/haku/{hakuOid}/tyyppi/{tyyppi}/kieli/{kieli}")
    @Consumes("application/json")
    public void valintatuloksetIlmanTilaaHakijalle(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("tyyppi") String tyyppi,
            @PathParam("kieli") String kieli,
            @Suspended AsyncResponse asyncResponse) {
        setAsyncTimeout(asyncResponse,
                String.format("ViestintapalveluProxyResource -palvelukutsu on aikakatkaistu: /viestintapalvelu/haku/%s/tyyppi/%s/kieli/%s",
                        hakuOid, tyyppi, kieli));
        viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, tyyppi, kieli).subscribe(
                letterCount -> {
                    asyncResponse.resume(Response.ok(letterCount,MediaType.APPLICATION_JSON_TYPE));
                },
                error -> {
                    errorResponse(String.format("Viestintäpalvelukutsu epäonnistui! %s",error.getMessage()), asyncResponse);
                }
        );
    }

    private void setAsyncTimeout(AsyncResponse response, String timeoutMessage) {
        response.setTimeout(5L, TimeUnit.MINUTES);
        response.setTimeoutHandler(asyncResponse -> {
            errorResponse(timeoutMessage, asyncResponse);
        });
    }

    private void errorResponse(String timeoutMessage, AsyncResponse asyncResponse) {
        asyncResponse.resume(Response.serverError()
                .entity(ImmutableMap.of("error", timeoutMessage))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
    }

}
