package fi.vm.sade.valinta.kooste.valintaperusteet;

import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoJarjestyskriteereillaDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
@Path("V2valintaperusteet")
@PreAuthorize("isAuthenticated()")
@Api(value = "/V2valintaperusteet", description = "V2 valintaperusteet")
public class ValintaperusteetResourceV2 {
    private final ValintaperusteetAsyncResource resource;
    private final Logger LOG = LoggerFactory.getLogger(ValintaperusteetResourceV2.class);

    @Autowired
    public ValintaperusteetResourceV2(ValintaperusteetAsyncResource resource) {
        this.resource = resource;
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_VALINTAPERUSTEET_READ', 'ROLE_APP_VALINTAPERUSTEET_CRUD')")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/hakukohde/{hakukohdeOid}/kayttaaValintalaskentaa")
    @ApiOperation(value = "Käyttääkö hakukohde valintalaskentaa", response = ValintaperusteetResourceResult.class)
    public void kayttaaValintalaskentaa(@PathParam("hakukohdeOid") String hakukohdeOid,
                                        @Suspended AsyncResponse asyncResponse) {
        try {
            asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
            asyncResponse.setTimeoutHandler(asyncResponse1 -> {
                LOG.error("Valintaperusteet -kutsu aikakatkaistiin hakukohteelle {}", hakukohdeOid);
                asyncResponse1.resume(Response.serverError().entity("Valintaperusteet -kutsu aikakatkaistiin").build());
            });
            resource.haeValintaperusteet(hakukohdeOid, null)
                    .subscribe(valintaperusteetDTOs -> {
                                boolean kayttaaValintalaskentaa = !valintaperusteetDTOs.stream()
                                        .filter(v -> v.getViimeinenValinnanvaihe() == v.getValinnanVaihe().getValinnanVaiheJarjestysluku())
                                        .filter(v -> v.getValinnanVaihe().getValintatapajono().stream().anyMatch(ValintatapajonoJarjestyskriteereillaDTO::getKaytetaanValintalaskentaa))
                                        .collect(Collectors.toList())
                                        .isEmpty();

                                asyncResponse.resume(Response.ok().entity(new ValintaperusteetResourceResult(kayttaaValintalaskentaa)).build());
                            },
                            e -> {
                                LOG.error("Valintaperusteet -kutsu epäonnistui hakukohteelle " + hakukohdeOid, e);
                                asyncResponse.resume(Response.serverError().entity("Valintaperusteet -kutsu epäonnistui" + e.getMessage()).build());
                            }
                    );
        } catch (Exception e) {
            String msg = "Odottamaton virhe valintalaskentapäättelyssä hakukohteelle " + hakukohdeOid;
            LOG.error(msg, e);
            asyncResponse.resume(Response.serverError().entity(msg).build());
        }
    }

    public static class ValintaperusteetResourceResult {
        public boolean kayttaaValintalaskentaa;

        public ValintaperusteetResourceResult() {
        }

        public ValintaperusteetResourceResult(boolean kayttaaValintalaskentaa) {
            this.kayttaaValintalaskentaa = kayttaaValintalaskentaa;
        }
    }
}
