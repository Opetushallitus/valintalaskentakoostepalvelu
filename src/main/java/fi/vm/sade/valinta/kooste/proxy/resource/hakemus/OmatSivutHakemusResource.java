package fi.vm.sade.valinta.kooste.proxy.resource.hakemus;

import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

@Controller
@Path("/proxy/valintatulos")
public class OmatSivutHakemusResource {

    private static final Logger LOG = LoggerFactory.getLogger(OmatSivutHakemusResource.class);

    @Autowired
    private ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;

    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @GET
    @Path("/haku/{hakuOid}/hakemusOid/{hakemusOid}")
    public void getValintatulos(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakemusOid") String hakemusOid,
            @Suspended final AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(30L, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(handler -> {
            LOG.error("getValintatulos proxy -palvelukutsu on aikakatkaistu: /valintatulos/haku/{hakuOid}/hakemusOid/{hakemusOid}", hakuOid, hakemusOid);
            handler.resume(Response.serverError()
                    .entity("Valintatulokset proxy -palvelukutsu on aikakatkaistu")
                    .build());
        });
        valintaTulosServiceAsyncResource.getHakemuksenValintatulosAsString(hakuOid, hakemusOid)
                .subscribe(
                        toiveenValintaTulokset ->
                                asyncResponse.resume(Response
                                        .ok()
                                        .header("Content-Type", "application/json")
                                        .entity(toiveenValintaTulokset)
                                        .build()),
                        error -> {
                            LOG.error("getHakemuksenValintatulosAsString throws", error);
                            asyncResponse.resume(Response.serverError().entity(error.getMessage()).build());
                        }
                );
    }
}
