package fi.vm.sade.valinta.kooste.hakemukset.resource;

import com.google.common.base.Preconditions;

import fi.vm.sade.authentication.business.service.Authorizer;
import fi.vm.sade.valinta.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.hakemukset.service.AmmatillisenKielikoeMigrationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


@Controller("AmmatillisenKielikoeMigrationResource")
@Path("ammatillisenKielikoeTulokset")
@PreAuthorize("isAuthenticated()")
@Api(value = "/ammatillisenKielikoeTulokset", description = "Ammatillisen koulutuksen kielikoetulokset")
public class AmmatillisenKielikoeMigrationResource {
    private static final Logger LOG = LoggerFactory.getLogger(AmmatillisenKielikoeMigrationResource.class);

    @Autowired
    private Authorizer authorizer;

    @Autowired
    private AmmatillisenKielikoeMigrationService ammatillisenKielikoeMigrationService;

    @Value("${root.organisaatio.oid}")
    private String rootOrganisationOid;

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_CRID')")
    @POST
    @Path("/migroiSureen")
    @Produces("application/json")
    @ApiOperation(value = "Migroi ammatillisen koulutuksen kielikokeen tuloksia hakemukselta Suoritusrekisteriin", response = AmmatillisenKielikoeMigrationService.Result.class)
    public void migroiAmmatillisenKielikoetulksetSureen(@QueryParam("since") @ApiParam(value = "since", example = "2016-10-05") String sinceStr,
                                                        @Suspended AsyncResponse asyncResponse) throws ParseException {
        assertUserIsOph();
        Preconditions.checkNotNull(sinceStr);
        Date since = new SimpleDateFormat("yyyy-MM-dd").parse(sinceStr);
        asyncResponse.setTimeout(60, TimeUnit.MINUTES);

        LOG.info(String.format("Aloitetaan ammatillisen kielikoetulosten migraatio Suoritusrekisteriin ajankohdasta %s lähtien ", since));
        Long started = System.currentTimeMillis();

        ammatillisenKielikoeMigrationService.migroiKielikoetuloksetSuoritusrekisteriin(since, KoosteAudit.username(),
            successResult -> {
                long durationSeconds = (System.currentTimeMillis() - started) / 1000;
                LOG.info(String.format("Migraatio meni virheittä läpi (käsiteltiin osallistumiset hetkestä %s lähtien, kesti %s sekuntia, tulos: %s)", since, durationSeconds, successResult));
                asyncResponse.resume(Response.ok(successResult).build());
            },
            (message, exception) -> {
                long durationSeconds = (System.currentTimeMillis() - started) / 1000;
                logException(since, message, exception, durationSeconds);
                asyncResponse.resume(Response.serverError().entity(message).build());
            });
    }

    private void logException(Date since, String message, Throwable exception, long durationSeconds) {
        String messageToLog;
        if (exception instanceof HttpExceptionWithResponse) {
            messageToLog = message + " : " + ((HttpExceptionWithResponse) exception).contentToString();
        } else {
            messageToLog = message;
        }
        LOG.error(String.format("Migraatio epäonnistui virheeseen (käsiteltiin osallistumiset hetkestä %s lähtien, kesti %s sekuntia): %s", since, durationSeconds, messageToLog), exception);
    }

    private void assertUserIsOph() {
        authorizer.checkOrganisationAccess(rootOrganisationOid, "ROLE_APP_HAKEMUS_CRUD");
    }
}
