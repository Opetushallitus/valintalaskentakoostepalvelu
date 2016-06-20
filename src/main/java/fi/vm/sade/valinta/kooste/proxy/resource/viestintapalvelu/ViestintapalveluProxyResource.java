package fi.vm.sade.valinta.kooste.proxy.resource.viestintapalvelu;

import com.codepoetics.protonpack.StreamUtils;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto.LetterBatchCountDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import rx.Observable;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.codepoetics.protonpack.StreamUtils.*;
import static java.util.Arrays.*;
import static rx.Observable.*;

@Controller("ViestintapalveluProxyResource")
@Path("/proxy/viestintapalvelu")
public class ViestintapalveluProxyResource {

    @Autowired
    private ViestintapalveluAsyncResource viestintapalveluAsyncResource;

    @POST
    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @Path("/publish/haku/{hakuOid}")
    @Consumes("text/plain")
    public void julkaiseKirjeetOmillaSivuilla(@PathParam("hakuOid") String hakuOid,
                                              @QueryParam("asiointikieli") String asiointikieli,
                                              @QueryParam("kirjeenTyyppi") String kirjeenTyyppi,
                                              @Suspended AsyncResponse asyncResponse) {
        viestintapalveluAsyncResource.haeKirjelahetysJulkaistavaksi(hakuOid, kirjeenTyyppi, asiointikieli)
            .flatMap(batchIdOptional -> {
                if(batchIdOptional.isPresent()) {
                    return viestintapalveluAsyncResource.julkaiseKirjelahetys(batchIdOptional.get());
                } else {
                    throw new RuntimeException("Kirjelähetyksen ID:tä ei löytynyt.");
                }
            }).subscribe(
                batchIdOptional -> asyncResponse.resume(Response.ok(batchIdOptional.get()).build()),
                throwable -> errorResponse(String.format("Viestintäpalvelukutsu epäonnistui! %s",throwable.getMessage()), asyncResponse)
            );
    }

    @GET
    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @Path("/count/haku/{hakuOid}")
    @Consumes("application/json")
    public void valintatuloksetIlmanTilaaHakijalle(
            @PathParam("hakuOid") String hakuOid,
            @Suspended AsyncResponse asyncResponse) {
        setAsyncTimeout(asyncResponse,
                String.format("ViestintapalveluProxyResource -palvelukutsu on aikakatkaistu: /viestintapalvelu/haku/%s/tyyppi/--/kieli/--",
                        hakuOid));

        Observable<LetterBatchCountDto> hyvaksymiskirjeFi = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje", "fi");
        Observable<LetterBatchCountDto> hyvaksymiskirjeSv = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje", "sv");
        Observable<LetterBatchCountDto> hyvaksymiskirjeEn = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje", "en");

        Observable<LetterBatchCountDto> jalkiohjauskirjeFi = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje", "fi");
        Observable<LetterBatchCountDto> jalkiohjauskirjeSv = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje", "sv");
        Observable<LetterBatchCountDto> jalkiohjauskirjeEn = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje", "en");

        combineLatest(hyvaksymiskirjeFi, hyvaksymiskirjeSv, hyvaksymiskirjeEn, jalkiohjauskirjeFi, jalkiohjauskirjeSv, jalkiohjauskirjeEn, (hFi,hSv,hEn,jFi,jSv,jEn) -> ImmutableMap.of(
                "hyvaksymiskirje", ImmutableMap.of("fi",hFi, "sv",hSv, "en",hEn),
                "jalkiohjauskirje", ImmutableMap.of("fi",jFi, "sv",jSv, "en",jEn))).subscribe(
                letterCount -> {
                    asyncResponse.resume(Response.ok(letterCount,MediaType.APPLICATION_JSON_TYPE).build());
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
