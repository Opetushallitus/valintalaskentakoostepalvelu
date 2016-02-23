package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import com.wordnik.swagger.annotations.ApiOperation;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

@Controller("ValintaTulosServiceProxyResource")
@Path("/proxy/valintatulosservice")
public class ValintaTulosServiceProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(ValintaTulosServiceProxyResource.class);

    @Autowired
    private SijoitteluAsyncResource sijoitteluResource;

    @Autowired
    private ValintaTulosServiceAsyncResource valintaTulosServiceResource;

    @GET
    @Path("/haku/{hakuOid}/sijoitteluajo/{sijoitteluAjo}/hakukohde/{hakukohdeOid}")
    @Consumes("application/json")
    public void sijoittelunTulokset(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("sijoitteluAjo") String sijoitteluAjo,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(asyncResponse1 -> {
            LOG.error("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu: /haku/{}/sijoitteluajo/{}/hakukohde/{}", hakuOid, sijoitteluAjo, hakukohdeOid);
            asyncResponse1.resume(Response.serverError().entity("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu").build());
        });

        sijoitteluResource.getLatestHakukohdeBySijoittelu(hakuOid, sijoitteluAjo, hakukohdeOid, h -> {
            asyncResponse.resume(Response
                    .ok(h)
                    .header("Content-Type", "application/json").build());
        }, t -> {

        });

    }

}
