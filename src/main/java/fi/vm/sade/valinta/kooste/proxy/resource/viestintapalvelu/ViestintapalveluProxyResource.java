package fi.vm.sade.valinta.kooste.proxy.resource.viestintapalvelu;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RyhmasahkopostiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto.LetterBatchCountDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import rx.Observable;
import rx.observables.BlockingObservable;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static rx.Observable.*;

@Controller("ViestintapalveluProxyResource")
@Path("/proxy/viestintapalvelu")
public class ViestintapalveluProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(ViestintapalveluProxyResource.class);

    @Autowired
    private ViestintapalveluAsyncResource viestintapalveluAsyncResource;

    @Autowired
    private RyhmasahkopostiAsyncResource ryhmasahkopostiAsyncResource;

    @POST
    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @Path("/publish/haku/{hakuOid}")
    @Consumes("application/json")
    @Produces("text/plain")
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

    private LetterBatchCountDto haeRyhmasahkopostiId(LetterBatchCountDto countDto) {
        if(countDto.letterBatchId == null) {
            return countDto;
        }
        Optional<Long> groupEmailId = BlockingObservable.from(ryhmasahkopostiAsyncResource.haeRyhmasahkopostiIdByLetterObservable(countDto.letterBatchId)).first();
        if(groupEmailId.isPresent()) {
            return new LetterBatchCountDto(countDto.letterBatchId, countDto.letterTotalCount, countDto.letterReadyCount, countDto.letterErrorCount, countDto.letterPublishedCount, countDto.readyForPublish, false, groupEmailId.get());
        }
        return countDto;
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

        Observable<LetterBatchCountDto> hyvaksymiskirjeFi = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje", "fi").map(count -> haeRyhmasahkopostiId(count));
        Observable<LetterBatchCountDto> hyvaksymiskirjeSv = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje", "sv").map(count -> haeRyhmasahkopostiId(count));;
        Observable<LetterBatchCountDto> hyvaksymiskirjeEn = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje", "en").map(count -> haeRyhmasahkopostiId(count));;

        Observable<LetterBatchCountDto> jalkiohjauskirjeFi = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje", "fi").map(count -> haeRyhmasahkopostiId(count));;
        Observable<LetterBatchCountDto> jalkiohjauskirjeSv = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje", "sv").map(count -> haeRyhmasahkopostiId(count));;
        Observable<LetterBatchCountDto> jalkiohjauskirjeEn = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje", "en").map(count -> haeRyhmasahkopostiId(count));;

        combineLatest(hyvaksymiskirjeFi, hyvaksymiskirjeSv, hyvaksymiskirjeEn, jalkiohjauskirjeFi, jalkiohjauskirjeSv, jalkiohjauskirjeEn, (hFi,hSv,hEn,jFi,jSv,jEn) -> ImmutableMap.of(
                "hyvaksymiskirje", ImmutableMap.of("fi",hFi, "sv",hSv, "en",hEn),
                "jalkiohjauskirje", ImmutableMap.of("fi",jFi, "sv",jSv, "en",jEn))).subscribe(
                letterCount -> {
                    asyncResponse.resume(Response.ok(letterCount,MediaType.APPLICATION_JSON_TYPE).build());
                },
                error -> {
                    LOG.error("Viestintäpalvelukutsu epäonnistui!", error);
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
