package fi.vm.sade.valinta.kooste.valintalaskentatulos.resource;

import static fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti.getTeksti;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.HakukohdeResource;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.excel.ValintalaskennanTulosExcel;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.JalkiohjaustulosExcelRoute;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.SijoittelunTulosExcelRoute;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.ValintalaskentaTulosExcelRoute;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.service.ValintakoekutsutExcelService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;

@Controller("ValintalaskentaExcelResource")
@Path("valintalaskentaexcel")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintalaskentaexcel", description = "Excel-raportteja")
public class ValintalaskentaExcelResource {
    private final static Logger LOG = LoggerFactory.getLogger(ValintalaskentaExcelResource.class);
    public final static MediaType APPLICATION_VND_MS_EXCEL = new MediaType("application", "vnd.ms-excel");
    @Autowired private ValintalaskentaTulosExcelRoute valintalaskentaTulosProxy;
    @Autowired private ValintakoekutsutExcelService valintakoekutsutExcelService;
    @Autowired private SijoittelunTulosExcelRoute sijoittelunTulosExcelProxy;
    @Autowired private JalkiohjaustulosExcelRoute jalkiohjaustulos;
    @Autowired private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;
    @Autowired private HakukohdeResource hakukohdeResource;
    @Autowired private HaeHakukohdeNimiTarjonnaltaKomponentti haeHakukohdeNimiTarjonnaltaKomponentti;

    @GET
    @Path("/jalkiohjaustulos/aktivoi")
    @Produces("application/vnd.ms-excel")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
    @ApiOperation(value = "Haun jälkiohjattavat Excel-raporttina", response = Response.class)
    public Response haeJalkiohjausTuloksetExcelMuodossa(@QueryParam("hakuOid") String hakuOid) {
        try {
            InputStream input = jalkiohjaustulos.luoXls(hakuOid);
            return Response.ok(input, APPLICATION_VND_MS_EXCEL).header("content-disposition", "inline; filename=jalkiohjaustulos.xls").build();
        } catch (Exception e) {
            LOG.error("Jälkiohjaustulosexcelin luonti epäonnistui haulle {}: {}", new Object[] {hakuOid, e.getMessage()});
            return Response.ok(ExcelExportUtil.exportGridAsXls(new Object[][] {new Object[] {"Tarvittavien tietojen hakeminen epäonnistui!", "Hakemuspalvelu saattaa olla ylikuormittunut!", "Yritä uudelleen!"}}), APPLICATION_VND_MS_EXCEL).header("content-disposition", "inline; filename=yritauudelleen.xls").build();
        }
    }

    @POST
    @Path("/valintakoekutsut/aktivoi")
    @Consumes("application/json")
    @Produces("application/json")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
    @ApiOperation(value = "Hakukohteen hyväksytyt Excel-raporttina", response = Response.class)
    public ProsessiId haeTuloksetExcelMuodossa(
    /* OPTIONAL */DokumentinLisatiedot lisatiedot, @QueryParam("hakuOid") String hakuOid, @QueryParam("hakukohdeOid") String hakukohdeOid) {
        if (lisatiedot == null) {
            throw new RuntimeException("ValintakoeOid on pakollinen!");
        }
        try {
            DokumenttiProsessi p = new DokumenttiProsessi("Valintalaskentaexcel", "Valintakoekutsut taulukkolaskenta tiedosto", "", Arrays.asList("valintakoekutsut", "taulukkolaskenta"));
            dokumenttiProsessiKomponentti.tuoUusiProsessi(p);
            valintakoekutsutExcelService.luoExcel(p, hakuOid, hakukohdeOid, lisatiedot.getValintakoeTunnisteet(), Sets.newHashSet(Optional.ofNullable(lisatiedot.getHakemusOids()).orElse(Collections.emptyList())));
            return p.toProsessiId();
        } catch (Exception e) {
            LOG.error("Valintakoekutsut excelin luonti epäonnistui hakukohteelle {}, valintakoeoideille {}: {}", new Object[] {hakukohdeOid, Arrays.toString(lisatiedot.getValintakoeTunnisteet().toArray()), e.getMessage()});
            throw new RuntimeException("Valintakoekutsut excelin luonti epäonnistui!", e);
        }
    }

    @POST
    @Path("/sijoitteluntulos/aktivoi")
    @Consumes("application/json")
    @Produces("application/json")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
    @ApiOperation(value = "Sijoittelun tulokset Excel-raporttina", response = Response.class)
    public ProsessiId haeSijoittelunTuloksetExcelMuodossa(@QueryParam("sijoitteluajoId") String sijoitteluajoId, @QueryParam("hakukohdeOid") String hakukohdeOid, @QueryParam("hakuOid") String hakuOid) throws Exception {
        try {
            DokumenttiProsessi p = new DokumenttiProsessi("Sijoitteluntulosexcel", "Sijoitteluntulokset taulukkolaskenta tiedosto", "", Arrays.asList("sijoitteluntulos", "taulukkolaskenta"));
            sijoittelunTulosExcelProxy.luoXls(p, hakukohdeOid, sijoitteluajoId, hakuOid, SecurityContextHolder.getContext().getAuthentication());
            dokumenttiProsessiKomponentti.tuoUusiProsessi(p);
            return p.toProsessiId();
        } catch (Exception e) {
            LOG.error("Sijoitteluntulos excelin luonti epäonnistui hakukohteelle {} ja sijoitteluajolle {}: {}", new Object[] {hakukohdeOid, sijoitteluajoId, e.getMessage()});
            throw e;
        }
    }

    @GET
    @Path("/valintalaskennantulos/aktivoi")
    @Produces("application/vnd.ms-excel")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
    @ApiOperation(value = "Valintalaskennan tulokset Excel-raporttina", response = Response.class)
    public Response haeValintalaskentaTuloksetExcelMuodossa(@QueryParam("hakukohdeOid") String hakukohdeOid) {
        try {
            final HakukohdeDTO hakukohdeDTO = haeHakukohdeNimiTarjonnaltaKomponentti.haeHakukohdeNimi(hakukohdeOid);
            final XSSFWorkbook workbook = ValintalaskennanTulosExcel.luoExcel(hakukohdeDTO, hakukohdeResource.hakukohde(hakukohdeOid));
            return Response.ok(Excel.export(workbook), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").header("content-disposition", "inline; filename=valintalaskennantulos.xlsx").build();
        } catch (Exception e) {
            LOG.error("Valintakoekutsut excelin luonti epäonnistui hakukohteelle {}: {}", new Object[] {hakukohdeOid, e.getMessage()});
            return Response.ok(ExcelExportUtil.exportGridAsXls(new Object[][] {new Object[] {"Tarvittavien tietojen hakeminen epäonnistui!", "Hakemuspalvelu saattaa olla ylikuormittunut!", "Yritä uudelleen!"}}), APPLICATION_VND_MS_EXCEL).header("content-disposition", "inline; filename=yritauudelleen.xls").build();
        }
    }
}
