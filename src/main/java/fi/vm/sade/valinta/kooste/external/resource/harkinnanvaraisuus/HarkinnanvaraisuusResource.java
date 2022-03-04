package fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus;

import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakemuksenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller("HarkinnanvaraisuusResource")
@Path("harkinnanvaraisuus")
@PreAuthorize("isAuthenticated()")
@Api(value = "/harkinnanvaraisuus", description = "Hakemusten harkinnanvaraisuustiedot")
public class HarkinnanvaraisuusResource {
  private static final Logger LOG = LoggerFactory.getLogger(HarkinnanvaraisuusResource.class);

  @Autowired private AuthorityCheckService authorityCheckService;

  @Autowired private HarkinnanvaraisuusAsyncResource harkinnanvaraisuusAsyncResource;

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @POST
  @Path("/hakemuksille")
  @Consumes("application/json")
  @Produces("application/json")
  @ApiOperation(
      value = "Hakemusten harkinnanvaraisuustiedot") // response fixme response = ???.class
  public void hakemuksetHarkinnanvaraisuustiedot(
      List<String> hakemusOids,
      @Suspended AsyncResponse asyncResponse,
      @Context HttpServletRequest request) {
    asyncResponse.setTimeout(1, TimeUnit.HOURS);

    // todo auditlog

    CompletableFuture<List<HakemuksenHarkinnanvaraisuus>> result =
        harkinnanvaraisuusAsyncResource.getHarkinnanvaraisuudetForHakemuksesOnlyFromAtaru(
            hakemusOids);
    result
        .thenApply(asyncResponse::resume)
        .exceptionally(
            e ->
                asyncResponse.resume(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build()));
  }
}
