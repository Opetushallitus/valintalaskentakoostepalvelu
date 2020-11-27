package fi.vm.sade.valinta.kooste.kela.resource;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.kela.dto.KelaCache;
import fi.vm.sade.valinta.kooste.kela.dto.KelaHakuFiltteri;
import fi.vm.sade.valinta.kooste.kela.dto.KelaLuonti;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.route.KelaFtpRoute;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import fi.vm.sade.valinta.sharedutils.ValintaResource;
import fi.vm.sade.valinta.sharedutils.ValintaperusteetOperation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller("KelaResource")
@Path("kela")
@PreAuthorize("isAuthenticated()")
@Api(value = "/kela", description = "Kela-dokumentin luontiin ja FTP-siirtoon")
public class KelaResource {
  private static final Logger LOG = LoggerFactory.getLogger(KelaResource.class);

  @Autowired private TarjontaAsyncResource tarjontaAsyncResource;

  @Autowired(required = false)
  private KelaRoute kelaRoute;

  @Autowired(required = false)
  private KelaFtpRoute kelaFtpRoute;

  @Autowired private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

  @POST
  @Path("/aktivoi")
  @Consumes("application/json")
  @Produces("application/json")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(value = "Kela-reitin aktivointi", response = ProsessiId.class)
  public ProsessiId aktivoiKelaTiedostonluonti(
      KelaHakuFiltteri hakuTietue, @Context HttpServletRequest request) {
    if (hakuTietue == null
        || hakuTietue.getHakuOids() == null
        || hakuTietue.getAlkupvm() == null
        || hakuTietue.getLoppupvm() == null
        || hakuTietue.getHakuOids().isEmpty()) {
      throw new RuntimeException(
          "Vähintään yksi hakuOid ja alku- ja loppupvm on annettava Kela-dokumentin luontia varten.");
    }
    String aineistonNimi = hakuTietue.getAineisto();
    Date alkuPvm = hakuTietue.getAlkupvm();
    Date loppuPvm = hakuTietue.getLoppupvm();
    String organisaationNimi = "OPH";
    KelaProsessi kelaProsessi =
        new KelaProsessi("Kela-dokumentin luonti", hakuTietue.getHakuOids());
    kelaRoute.aloitaKelaLuonti(
        kelaProsessi,
        new KelaLuonti(
            kelaProsessi.getId(),
            hakuTietue.getHakuOids(),
            aineistonNimi,
            organisaationNimi,
            new KelaCache(tarjontaAsyncResource),
            kelaProsessi,
            alkuPvm,
            loppuPvm));
    // SecurityContextHolder.getContext().getAuthentication()
    dokumenttiProsessiKomponentti.tuoUusiProsessi(kelaProsessi);
    Map<String, String> additionalAuditInfo = new HashMap<>();
    String allHakuOids = hakuTietue.getHakuOids().stream().collect(Collectors.joining(","));
    additionalAuditInfo.put("hakuOids", allHakuOids);
    additionalAuditInfo.put("aineisto", aineistonNimi);
    additionalAuditInfo.put("alkupvm", alkuPvm.toString());
    additionalAuditInfo.put("loppupvm", loppuPvm.toString());
    AuditLog.log(
        KoosteAudit.AUDIT,
        AuditLog.getUser(request),
        ValintaperusteetOperation.KELA_VASTAANOTTO_EXPORT_LUONTI,
        ValintaResource.DOKUMENTTI,
        allHakuOids,
        Changes.EMPTY,
        additionalAuditInfo);
    return kelaProsessi.toProsessiId();
  }

  @POST
  @Path("/laheta")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(value = "FTP-siirto", response = Response.class)
  public Response laheta(String documentId, @Context HttpServletRequest request) {
    LOG.info("Kela-ftp siirto aloitettu dokumentille: {}", documentId);
    AuditLog.log(
        KoosteAudit.AUDIT,
        AuditLog.getUser(request),
        ValintaperusteetOperation.KELA_VASTAANOTTO_EXPORT_LATAUS_FTP,
        ValintaResource.DOKUMENTTI,
        documentId,
        Changes.EMPTY);
    try {
      kelaFtpRoute.aloitaKelaSiirto(documentId);
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Kela-dokumentin siirto dokumentille: " + documentId + " epäonnistui.", e);
      return Response.serverError().build();
    }
  }
}
