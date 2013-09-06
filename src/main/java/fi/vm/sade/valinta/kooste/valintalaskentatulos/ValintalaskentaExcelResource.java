package fi.vm.sade.valinta.kooste.valintalaskentatulos;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import fi.vm.sade.valinta.kooste.valintalaskentatulos.export.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.proxy.SijoittelunTulosExcelProxy;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.proxy.ValintalaskentaTulosExcelProxy;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.proxy.ValintalaskentaTulosProxy;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Aktivoi valintalaskennan tulos service pyyntoja!
 */
@Controller
@Path("valintalaskentaexcel")
public class ValintalaskentaExcelResource {

    private final static Logger LOG = LoggerFactory.getLogger(ValintalaskentaExcelResource.class);

    public final static MediaType APPLICATION_VND_MS_EXCEL = new MediaType("application", "vnd.ms-excel");
    @Autowired
    private ValintalaskentaTulosExcelProxy valintalaskentaTulosProxy;
    @Autowired
    private ValintalaskentaTulosProxy valintalaskentaTulos;
    @Autowired
    private SijoittelunTulosExcelProxy sijoittelunTulosExcelProxy;

    @GET
    @Path("valintakoekutsut/aktivoi")
    @Produces("application/vnd.ms-excel")
    public Response haeTuloksetExcelMuodossa(@QueryParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("valintakoeOid") List<String> valintakoeOids) {
        try {
            InputStream input = valintalaskentaTulos.luoXls(hakukohdeOid, valintakoeOids);
            return Response.ok(input, APPLICATION_VND_MS_EXCEL)
                    .header("content-disposition", "inline; filename=valintakoetulos.xls").build();
        } catch (Exception e) {
            // Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
            // todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
            // Ylläpitäjä voi lukea logeista todellisen syyn!
            LOG.error("Valintakoekutsut excelin luonti epäonnistui hakukohteelle {}, valintakoeoideille {}: {}",
                    new Object[] { hakukohdeOid, Arrays.toString(valintakoeOids.toArray()), e.getMessage() });
            return Response
                    .ok(ExcelExportUtil.exportGridAsXls(new Object[][] { new Object[] {
                            "Tarvittavien tietojen hakeminen epäonnistui!",
                            "Hakemuspalvelu saattaa olla ylikuormittunut!", "Yritä uudelleen!" } }),
                            APPLICATION_VND_MS_EXCEL)
                    .header("content-disposition", "inline; filename=yritauudelleen.xls").build();
        }
    }

    @GET
    @Path("sijoitteluntulos/aktivoi")
    @Produces("application/vnd.ms-excel")
    public Response haeSijoittelunTuloksetExcelMuodossa(@QueryParam("sijoitteluajoId") Long sijoitteluajoId,
            @QueryParam("hakukohdeOid") String hakukohdeOid, @QueryParam("hakuOid") String hakuOid) {
        try {
            InputStream input = sijoittelunTulosExcelProxy.luoXls(hakukohdeOid, sijoitteluajoId, hakuOid);
            return Response.ok(input, APPLICATION_VND_MS_EXCEL)
                    .header("content-disposition", "inline; filename=valintalaskentatulos.xls").build();
        } catch (Exception e) {
            // Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
            // todennäköisin syy on sijoittelupalvelun tai hakemuspalvelun
            // ylikuormittumisessa!
            // Ylläpitäjä voi lukea logeista todellisen syyn!
            LOG.error("Sijoitteluntulos excelin luonti epäonnistui hakukohteelle {} ja sijoitteluajolle {}: {}",
                    new Object[] { hakukohdeOid, sijoitteluajoId, e.getMessage() });
            return Response
                    .ok(ExcelExportUtil.exportGridAsXls(new Object[][] { new Object[] {
                            "Tarvittavien tietojen hakeminen epäonnistui!",
                            "Hakemuspalvelu saattaa olla ylikuormittunut!", "Yritä uudelleen!" } }),
                            APPLICATION_VND_MS_EXCEL)
                    .header("content-disposition", "inline; filename=yritauudelleen.xls").build();
        }
    }

    @GET
    @Path("valintalaskennantulos/aktivoi")
    @Produces("application/vnd.ms-excel")
    public Response haeValintalaskentaTuloksetExcelMuodossa(@QueryParam("hakukohdeOid") String hakukohdeOid) {
        try {
            InputStream input = valintalaskentaTulosProxy.luoXls(hakukohdeOid);
            return Response.ok(input, APPLICATION_VND_MS_EXCEL)
                    .header("content-disposition", "inline; filename=valintalaskentatulos.xls").build();
        } catch (Exception e) {
            // Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
            // todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
            // Ylläpitäjä voi lukea logeista todellisen syyn!
            LOG.error("Valintakoekutsut excelin luonti epäonnistui hakukohteelle {}: {}", new Object[] { hakukohdeOid,
                    e.getMessage() });
            return Response
                    .ok(ExcelExportUtil.exportGridAsXls(new Object[][] { new Object[] {
                            "Tarvittavien tietojen hakeminen epäonnistui!",
                            "Hakemuspalvelu saattaa olla ylikuormittunut!", "Yritä uudelleen!" } }),
                            APPLICATION_VND_MS_EXCEL)
                    .header("content-disposition", "inline; filename=yritauudelleen.xls").build();
        }
    }

}
