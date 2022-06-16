package fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.valinta.kooste.AuthorizationUtil;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakemuksenHarkinnanvaraisuus;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import fi.vm.sade.valinta.sharedutils.ValintaResource;
import fi.vm.sade.valinta.sharedutils.ValintaperusteetOperation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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

  @Autowired
  private HarkinnanvaraisuusAsyncResource harkinnanvaraisuusAsyncResource;

  @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @POST
  @Path("/hakemuksille")
  @Consumes("application/json")
  @Produces("application/json")
  @ApiOperation(value = "Hakemusten harkinnanvaraisuustiedot")
  public void hakemustenHarkinnanvaraisuustiedot(List<String> hakemusOids, @Suspended AsyncResponse asyncResponse,
      @Context HttpServletRequest request) {
    asyncResponse.setTimeout(1, TimeUnit.HOURS);

    String targetOids = String.join(",", hakemusOids);
    AuditLog.log(KoosteAudit.AUDIT, AuthorizationUtil.createAuditSession(request).asAuditUser(),
        ValintaperusteetOperation.HAKEMUS, // fixme, sharedutils
        ValintaResource.HAKEMUKSET, // fixme, sharedutils
        targetOids, Changes.EMPTY, Collections.emptyMap());

    CompletableFuture<List<HakemuksenHarkinnanvaraisuus>> result = harkinnanvaraisuusAsyncResource
        .getHarkinnanvaraisuudetForHakemukses(hakemusOids);
    result.thenApply(asyncResponse::resume).exceptionally(e -> {
      LOG.error("Hakemusten harkinnanvaraisuustietojen haku epäonnistui: ", e);
      return asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build());
    });
  }

  @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @POST
  @Path("/atarutiedoille")
  @Consumes("application/json")
  @Produces("application/json")
  @ApiOperation(value = "Synkatut harkinnanvaraisuudet atarutiedoille")
  public void hakemustenHarkinnanvaraisuustiedotAtarutiedoille(List<HakemuksenHarkinnanvaraisuus> atarutiedot,
      @Suspended AsyncResponse asyncResponse, @Context HttpServletRequest request) {
    asyncResponse.setTimeout(1, TimeUnit.HOURS);

    String targetOids = atarutiedot.stream().map(HakemuksenHarkinnanvaraisuus::getHakemusOid)
        .collect(Collectors.joining(","));
    AuditLog.log(KoosteAudit.AUDIT, AuthorizationUtil.createAuditSession(request).asAuditUser(),
        ValintaperusteetOperation.HAKEMUS, // fixme, sharedutils
        ValintaResource.HAKEMUKSET, // fixme, sharedutils
        targetOids, Changes.EMPTY, Collections.emptyMap());

    CompletableFuture<List<HakemuksenHarkinnanvaraisuus>> result = harkinnanvaraisuusAsyncResource
        .getSyncedHarkinnanvaraisuudes(atarutiedot);
    result.thenApply(asyncResponse::resume).exceptionally(e -> {
      LOG.error("Hakemusten harkinnanvaraisuustietojen haku epäonnistui: ", e);
      return asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build());
    });
  }
}
