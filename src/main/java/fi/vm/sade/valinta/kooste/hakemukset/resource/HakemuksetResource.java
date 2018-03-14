package fi.vm.sade.valinta.kooste.hakemukset.resource;

import static java.util.Arrays.asList;
import com.google.common.base.Preconditions;

import fi.vm.sade.sharedutils.AuditLog;
import fi.vm.sade.sharedutils.ValintaResource;
import fi.vm.sade.sharedutils.ValintaperusteetOperation;
import fi.vm.sade.valinta.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.hakemukset.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.hakemukset.service.ValinnanvaiheenValintakoekutsutService;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller("HakemuksetResource")
@Path("hakemukset")
@PreAuthorize("isAuthenticated()")
@Api(value = "/hakemukset", description = "Hakemusten hakeminen")
public class HakemuksetResource {
    private static final Logger LOG = LoggerFactory.getLogger(HakemuksetResource.class);

    @Autowired
    private AuthorityCheckService authorityCheckService;

    @Autowired
    private ValinnanvaiheenValintakoekutsutService valinnanvaiheenValintakoekutsutService;

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @GET
    @Path("/valinnanvaihe")
    @Produces("application/json")
    @ApiOperation(value = "Valinnanvaiheen hakemusten listaus", response = HakemusDTO.class)
    public void hakemuksetValinnanvaiheelle(@QueryParam("hakuOid") String hakuOid,
                                            @QueryParam("valinnanvaiheOid") String valinnanvaiheOid,
                                            @Suspended AsyncResponse asyncResponse,
                                            @Context HttpServletRequest request) {
        Preconditions.checkNotNull(hakuOid);
        Preconditions.checkNotNull(valinnanvaiheOid);
        asyncResponse.setTimeout(10, TimeUnit.MINUTES);

        Map<String, String> additionalAuditInfo = new HashMap<>();
        additionalAuditInfo.put("hakuOid", hakuOid);
        additionalAuditInfo.put("ValinnanvaiheOid",valinnanvaiheOid);
        AuditLog.log(KoosteAudit.AUDIT, AuditLog.getUser(request), ValintaperusteetOperation.VALINNANVAIHEEN_HAKEMUKSET_HAKU, ValintaResource.HAKEMUKSET, valinnanvaiheOid, null, null, additionalAuditInfo);

        LOG.warn("Aloitetaan hakemusten listaaminen valinnanvaiheelle {} haussa {}", valinnanvaiheOid, hakuOid);
        Long started = System.currentTimeMillis();

        authorityCheckService.getAuthorityCheckForRoles(
                asList("ROLE_APP_HAKEMUS_READ_UPDATE", "ROLE_APP_HAKEMUS_READ", "ROLE_APP_HAKEMUS_CRUD", "ROLE_APP_HAKEMUS_LISATIETORU", "ROLE_APP_HAKEMUS_LISATIETOCRUD")
        ).subscribe(
                authCheck -> {
                    valinnanvaiheenValintakoekutsutService.hae(valinnanvaiheOid, hakuOid, authCheck,
                            hakemusDTOs -> {
                                long duration = (System.currentTimeMillis() - started) / 1000;
                                LOG.info("hakemusten listaaminen valinnanvaiheelle {} haussa {} kesti {} sekuntia", valinnanvaiheOid, hakuOid, duration);
                                asyncResponse.resume(Response.ok(hakemusDTOs).build());
                            },
                            exception -> {
                                long duration = (System.currentTimeMillis() - started) / 1000;
                                if (exception instanceof ValinnanvaiheenValintakoekutsutService.ValinnanvaiheelleEiLoydyValintaryhmiaException) {
                                    LOG.error(String.format("%s : kesto %d sekuntia", exception.getMessage(), duration));
                                    Map<String,String> responseContent = new HashMap<>();
                                    responseContent.put("message", exception.getMessage());
                                    asyncResponse.resume(Response
                                        .status(Response.Status.BAD_REQUEST)
                                        .entity(responseContent)
                                        .build());
                                } else {
                                    LOG.error(
                                        String.format("hakemusten listaaminen epäonnistui (valinnanvaihe %s, haku %s, kesto %d sekuntia", valinnanvaiheOid, hakuOid, duration),
                                        exception);
                                    asyncResponse.cancel();
                                }
                            }
                    );
                }, exception -> {
                    LOG.error(HttpExceptionWithResponse.appendWrappedResponse("hakemusten listaaminen epäonnistui, authCheck failed", exception), exception);
                    asyncResponse.resume(Response.serverError().entity(exception).build());
                }
        );
    }
}
