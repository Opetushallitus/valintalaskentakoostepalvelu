package fi.vm.sade.valinta.kooste.hakuimport.resource;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakuImportRoute;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakukohdeImportRoute;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import fi.vm.sade.valinta.sharedutils.ValintaResource;
import fi.vm.sade.valinta.sharedutils.ValintaperusteetOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("HakuImportResource")
@RequestMapping("/resources/hakuimport")
@PreAuthorize("isAuthenticated()")
@Tag(name = "/hakuimport", description = "Haun tuontiin tarjonnalta")
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

  @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Hauntuontireitin tila",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = List.class)))
      })
  public Collection<ProsessiJaStatus<HakuImportProsessi>> status() {
    return hakuImportValvomo.getUusimmatProsessitJaStatukset();
  }

  @GetMapping(value = "/aktivoi")
  @Operation(
      summary = "Haun tuonnin aktivointi",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public String aktivoiHakuImport(
      @RequestParam(value = "hakuOid", required = false) String hakuOid,
      HttpServletRequest request) {
    if (!hakuParametritService.getParametritForHaku(hakuOid).valinnanhallintaEnabled()) {
      String errorMessage = "no privileges";
      AuditLog.log(
          KoosteAudit.AUDIT,
          AuditLog.getUser(request),
          ValintaperusteetOperation.HAKU_TUONNIN_AKTIVOINTI,
          ValintaResource.HAKU,
          hakuOid,
          Changes.EMPTY,
          Map.of("error", errorMessage));
      return errorMessage;
    }

    if (StringUtils.isBlank(hakuOid)) {
      String errorMessage = "get parameter 'hakuOid' required";
      AuditLog.log(
          KoosteAudit.AUDIT,
          AuditLog.getUser(request),
          ValintaperusteetOperation.HAKU_TUONNIN_AKTIVOINTI,
          ValintaResource.HAKU,
          "",
          Changes.EMPTY,
          Map.of("error", errorMessage));
      return errorMessage;
    } else {
      LOG.info("Haku import haulle {}", hakuOid);
      AuditLog.log(
          KoosteAudit.AUDIT,
          AuditLog.getUser(request),
          ValintaperusteetOperation.HAKU_TUONNIN_AKTIVOINTI,
          ValintaResource.HAKU,
          hakuOid,
          Changes.EMPTY);
      hakuImportAktivointiRoute.asyncAktivoiHakuImport(hakuOid);
      return "in progress";
    }
  }

  @GetMapping(value = "/hakukohde")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @Operation(
      summary = "Hakukohde tuonnin aktivointi",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public String aktivoiHakukohdeImport(
      @RequestParam(value = "hakukohdeOid", required = false) String hakukohdeOid,
      HttpServletRequest request) {

    if (StringUtils.isBlank(hakukohdeOid)) {
      String errorMessage = "get parameter 'hakukohde' required";
      AuditLog.log(
          KoosteAudit.AUDIT,
          AuditLog.getUser(request),
          ValintaperusteetOperation.HAKUKOHDE_TUONNIN_AKTIVOINTI,
          ValintaResource.HAKUKOHDE,
          "",
          Changes.EMPTY,
          Map.of("error", errorMessage));
      return errorMessage;
    } else {
      LOG.info("Hakukohde import hakukohteelle {}", hakukohdeOid);
      AuditLog.log(
          KoosteAudit.AUDIT,
          AuditLog.getUser(request),
          ValintaperusteetOperation.HAKUKOHDE_TUONNIN_AKTIVOINTI,
          ValintaResource.HAKUKOHDE,
          hakukohdeOid,
          Changes.EMPTY);
      hakukohdeImportRoute.asyncAktivoiHakukohdeImport(
          hakukohdeOid,
          new HakuImportProsessi("Hakukohde", "Hakukhode"),
          SecurityContextHolder.getContext().getAuthentication());
      return "in progress";
    }
  }
}
