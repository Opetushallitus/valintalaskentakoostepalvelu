package fi.vm.sade.valinta.kooste.hakuimport.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import fi.vm.sade.valinta.sharedutils.ValintaResource;
import fi.vm.sade.valinta.sharedutils.ValintaperusteetOperation;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakuImportRoute;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakukohdeImportRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;

@Controller("HakuImportResource")
@Path("hakuimport")
@PreAuthorize("isAuthenticated()")
@Api(value = "/hakuimport", description = "Haun tuontiin tarjonnalta")
public class HakuImportResource {
    private static final Logger LOG = LoggerFactory.getLogger(HakuImportResource.class);

    @Autowired(required = false)
    private HakuImportRoute hakuImportAktivointiRoute;
    @Autowired(required = false)
    private HakukohdeImportRoute hakukohdeImportRoute;
    @Autowired(required = false)
    private HakuParametritService hakuParametritService;

    @Autowired(required = false)
    @Qualifier("hakuImportValvomo")
    private ValvomoService<HakuImportProsessi> hakuImportValvomo;

    @GET
    @Path("/status")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Hauntuontireitin tila", response = Collection.class)
    public Collection<ProsessiJaStatus<HakuImportProsessi>> status() {
        return hakuImportValvomo.getUusimmatProsessitJaStatukset();
    }

    @GET
    @Path("/aktivoi")
    @ApiOperation(value = "Haun tuonnin aktivointi", response = String.class)
    public String aktivoiHakuImport(@QueryParam("hakuOid") String hakuOid, @Context HttpServletRequest request) {
        if (!hakuParametritService.getParametritForHaku(hakuOid).valinnanhallintaEnabled()) {
            String errorMessage = "no privileges";
            AuditLog.log(KoosteAudit.AUDIT, AuditLog.getUser(request), ValintaperusteetOperation.HAKU_TUONNIN_AKTIVOINTI,
                    ValintaResource.HAKU, hakuOid, Changes.EMPTY, Map.of("error", errorMessage));
            return errorMessage;
        }

        if (StringUtils.isBlank(hakuOid)) {
            String errorMessage = "get parameter 'hakuOid' required";
            AuditLog.log(KoosteAudit.AUDIT, AuditLog.getUser(request), ValintaperusteetOperation.HAKU_TUONNIN_AKTIVOINTI,
                    ValintaResource.HAKU, "", Changes.EMPTY, Map.of("error", errorMessage));
            return errorMessage;
        } else {
            LOG.info("Haku import haulle {}", hakuOid);
            AuditLog.log(KoosteAudit.AUDIT, AuditLog.getUser(request), ValintaperusteetOperation.HAKU_TUONNIN_AKTIVOINTI,
                    ValintaResource.HAKU, hakuOid, Changes.EMPTY);
            hakuImportAktivointiRoute.asyncAktivoiHakuImport(hakuOid);
            return "in progress";
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
    @GET
    @Path("/hakukohde")
    @ApiOperation(value = "Hakukohde tuonnin aktivointi", response = String.class)
    public String aktivoiHakukohdeImport(
            @QueryParam("hakukohdeOid") String hakukohdeOid, @Context HttpServletRequest request) {

        if (StringUtils.isBlank(hakukohdeOid)) {
            String errorMessage = "get parameter 'hakukohde' required";
            AuditLog.log(KoosteAudit.AUDIT, AuditLog.getUser(request), ValintaperusteetOperation.HAKUKOHDE_TUONNIN_AKTIVOINTI,
                    ValintaResource.HAKUKOHDE, "", Changes.EMPTY, Map.of("error", errorMessage));
            return errorMessage;
        } else {
            LOG.info("Hakukohde import hakukohteelle {}", hakukohdeOid);
            AuditLog.log(KoosteAudit.AUDIT, AuditLog.getUser(request), ValintaperusteetOperation.HAKUKOHDE_TUONNIN_AKTIVOINTI,
                    ValintaResource.HAKUKOHDE, hakukohdeOid, Changes.EMPTY);
            hakukohdeImportRoute.asyncAktivoiHakukohdeImport(hakukohdeOid, new HakuImportProsessi("Hakukohde", "Hakukhode"), SecurityContextHolder.getContext().getAuthentication());
            return "in progress";
        }
    }
}
