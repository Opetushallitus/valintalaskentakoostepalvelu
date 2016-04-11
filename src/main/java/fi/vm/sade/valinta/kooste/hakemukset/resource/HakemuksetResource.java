package fi.vm.sade.valinta.kooste.hakemukset.resource;

import com.google.common.base.Preconditions;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.hakemukset.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.hakemukset.service.ValinnanvaiheenValintakoekutsutService;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

import static fi.vm.sade.auditlog.valintaperusteet.LogMessage.builder;
import static fi.vm.sade.valinta.kooste.KoosteAudit.AUDIT;
import static java.util.Arrays.asList;


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
    public void hakemuksetValinnanvaiheelle(@QueryParam("hakuOid") String hakuOid, @QueryParam("valinnanvaiheOid") String valinnanvaiheOid, @Suspended AsyncResponse asyncResponse) {
        Preconditions.checkNotNull(hakuOid);
        Preconditions.checkNotNull(valinnanvaiheOid);
        asyncResponse.setTimeout(3, TimeUnit.MINUTES);
        AUDIT.log(builder()
                .id(KoosteAudit.username())
                .valinnanvaiheOid(valinnanvaiheOid)
                .hakuOid(hakuOid)
                .setOperaatio(ValintaperusteetOperation.VALINNANVAIHEEN_HAKEMUKSET_HAKU)
                .build());

        LOG.warn("Aloitetaan hakemusten listaaminen valinnenvaiheelle {} haussa {}", valinnanvaiheOid, hakuOid);
        Long started = System.currentTimeMillis();

        authorityCheckService.getAuthorityCheckForRoles(
                asList("ROLE_APP_HAKEMUS_READ_UPDATE", "ROLE_APP_HAKEMUS_READ", "ROLE_APP_HAKEMUS_CRUD", "ROLE_APP_HAKEMUS_LISATIETORU", "ROLE_APP_HAKEMUS_LISATIETOCRUD"),
                authCheck -> {
                    valinnanvaiheenValintakoekutsutService.hae(valinnanvaiheOid, hakuOid,
                            hakemusDTOs -> {
                                long duration = (System.currentTimeMillis() - started) / 1000;
                                LOG.warn("hakemusten listaaminen valinnenvaiheelle {} haussa {} kesti {} sekuntia", valinnanvaiheOid, hakuOid, duration);
                                asyncResponse.resume(Response.ok(hakemusDTOs).build());
                            },
                            exception -> {
                                long duration = (System.currentTimeMillis() - started) / 1000;
                                LOG.error("hakemusten listaaminen epäonnistui (valinnenvaihe {}, haku {}, kesto {} sekuntia", valinnanvaiheOid, hakuOid, duration);
                                asyncResponse.cancel();
                            }
                    );
                },
                exception -> {
                    LOG.error("hakemusten listaaminen epäonnistui, authCheck failed", exception);
                    asyncResponse.resume(Response.serverError().entity(exception).build());
                });
    }
}
