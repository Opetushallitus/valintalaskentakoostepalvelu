package fi.vm.sade.valinta.kooste.proxy.resource.viestintapalvelu;

import static java.util.concurrent.TimeUnit.MINUTES;
import static io.reactivex.Observable.combineLatest;
import com.google.common.collect.ImmutableMap;

import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RyhmasahkopostiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto.LetterBatchCountDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import io.reactivex.Observable;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Controller("ViestintapalveluProxyResource")
@Path("/proxy/viestintapalvelu")
public class ViestintapalveluProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(ViestintapalveluProxyResource.class);

    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final RyhmasahkopostiAsyncResource ryhmasahkopostiAsyncResource;

    @Autowired
    public ViestintapalveluProxyResource(ViestintapalveluAsyncResource viestintapalveluAsyncResource,
                                         RyhmasahkopostiAsyncResource ryhmasahkopostiAsyncResource) {
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.ryhmasahkopostiAsyncResource = ryhmasahkopostiAsyncResource;
    }

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
        if (countDto.letterBatchId == null) {
            return countDto;
        }
        Optional<Long> groupEmailId = ryhmasahkopostiAsyncResource.haeRyhmasahkopostiIdByLetterObservable(countDto.letterBatchId).timeout(5, MINUTES).blockingFirst();
        return groupEmailId.map(aLong ->
            new LetterBatchCountDto(
                countDto.letterBatchId,
                countDto.letterTotalCount,
                countDto.letterReadyCount,
                countDto.letterErrorCount,
                countDto.letterPublishedCount,
                countDto.readyForPublish,
                false,
                aLong))
            .orElse(countDto);
    }

    @GET
    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @Path("/count/haku/{hakuOid}")
    @Consumes("application/json")
    public void countLettersForHaku(
            @PathParam("hakuOid") String hakuOid,
            @Suspended AsyncResponse asyncResponse) {
        setAsyncTimeout(asyncResponse,
                String.format("ViestintapalveluProxyResource -palvelukutsu on aikakatkaistu: /viestintapalvelu/haku/%s/tyyppi/--/kieli/--",
                        hakuOid));

        Observable<LetterBatchCountDto> hyvaksymiskirjeFi = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje", "fi").map(this::haeRyhmasahkopostiId);
        Observable<LetterBatchCountDto> hyvaksymiskirjeSv = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje", "sv").map(this::haeRyhmasahkopostiId);
        Observable<LetterBatchCountDto> hyvaksymiskirjeEn = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje", "en").map(this::haeRyhmasahkopostiId);

        Observable<LetterBatchCountDto> jalkiohjauskirjeFi = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje", "fi").map(this::haeRyhmasahkopostiId);
        Observable<LetterBatchCountDto> jalkiohjauskirjeSv = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje", "sv").map(this::haeRyhmasahkopostiId);
        Observable<LetterBatchCountDto> jalkiohjauskirjeEn = viestintapalveluAsyncResource.haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje", "en").map(this::haeRyhmasahkopostiId);

        combineLatest(hyvaksymiskirjeFi, hyvaksymiskirjeSv, hyvaksymiskirjeEn, jalkiohjauskirjeFi, jalkiohjauskirjeSv, jalkiohjauskirjeEn, (hFi,hSv,hEn,jFi,jSv,jEn) -> ImmutableMap.of(
                "hyvaksymiskirje", ImmutableMap.of("fi",hFi, "sv",hSv, "en",hEn),
                "jalkiohjauskirje", ImmutableMap.of("fi",jFi, "sv",jSv, "en",jEn))).subscribe(
                letterCount ->
                    asyncResponse.resume(Response.ok(letterCount,MediaType.APPLICATION_JSON_TYPE).build()),
                error -> {
                    LOG.error("Viestintäpalvelukutsu epäonnistui!", error);
                    errorResponse(String.format("Viestintäpalvelukutsu epäonnistui! %s",error.getMessage()), asyncResponse);
                }
        );
    }

    private void setAsyncTimeout(AsyncResponse response, String timeoutMessage) {
        response.setTimeout(5L, MINUTES);
        response.setTimeoutHandler(asyncResponse -> errorResponse(timeoutMessage, asyncResponse));
    }

    private void errorResponse(String timeoutMessage, AsyncResponse asyncResponse) {
        asyncResponse.resume(Response.serverError()
                .entity(ImmutableMap.of("error", timeoutMessage))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
    }
}
