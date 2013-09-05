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
import fi.vm.sade.valinta.kooste.valintalaskentatulos.proxy.ValintalaskentaTulosExcelProxy;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.proxy.ValintalaskentaTulosProxy;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Aktivoi valintalaskennan tulos service pyyntoja!
 */
@Controller
@Path("valintalaskentatulos")
public class ValintalaskentaTulosResource {

    private final static Logger LOG = LoggerFactory.getLogger(ValintalaskentaTulosResource.class);

    public final static MediaType APPLICATION_VND_MS_EXCEL = new MediaType("application", "vnd.ms-excel");
    @Autowired
    private ValintalaskentaTulosExcelProxy valintalaskentaTulosProxy;
    @Autowired
    private ValintalaskentaTulosProxy valintalaskentaTulos;

    @GET
    @Path("excel/aktivoi")
    @Produces("application/vnd.ms-excel")
    public Response haeTuloksetExcelMuodossa(@QueryParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("valintakoeOid") List<String> valintakoeOids) {
        try {
            InputStream input = valintalaskentaTulos.haeTuloksetXlsMuodossa(hakukohdeOid, valintakoeOids);
            return Response.ok(input, APPLICATION_VND_MS_EXCEL)
                    .header("content-disposition", "inline; filename=valintakoetulos.xls").build();
        } catch (Exception e) {
            // Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
            // todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
            // Ylläpitäjä voi lukea logeista todellisen syyn!
            LOG.error("Valintakoekutsut excelin luonti epäonnistui hakukohteelle {}, valintakoeoideille {}: {}",
                    new Object[] { hakukohdeOid, Arrays.toString(valintakoeOids.toArray()), e.getMessage() });
            return Response
                    .serverError()
                    .entity(ExcelExportUtil.exportGridAsXls(new Object[][] { new Object[] {
                            "Tarvittavien tietojen hakeminen epäonnistui!",
                            "Hakemuspalvelu saattaa olla ylikuormittunut!", "Yritä uudelleen!" } }))
                    .type(APPLICATION_VND_MS_EXCEL)
                    .header("content-disposition", "inline; filename=yritauudelleen.xls").build();
        }
    }

    @GET
    @Path("valintalaskentatulos/aktivoi")
    @Produces("application/vnd.ms-excel")
    public Response haeValintalaskentaTuloksetExcelMuodossa(@QueryParam("hakukohdeOid") String hakukohdeOid) {
        try {
            InputStream input = valintalaskentaTulosProxy.haeValintalaskennanTuloksetXlsMuodossa(hakukohdeOid);
            return Response.ok(input, APPLICATION_VND_MS_EXCEL)
                    .header("content-disposition", "inline; filename=valintalaskentatulos.xls").build();
        } catch (Exception e) {
            // Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
            // todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
            // Ylläpitäjä voi lukea logeista todellisen syyn!
            LOG.error("Valintakoekutsut excelin luonti epäonnistui hakukohteelle {}: {}", new Object[] { hakukohdeOid,
                    e.getMessage() });
            return Response
                    .serverError()
                    .entity(ExcelExportUtil.exportGridAsXls(new Object[][] { new Object[] {
                            "Tarvittavien tietojen hakeminen epäonnistui!",
                            "Hakemuspalvelu saattaa olla ylikuormittunut!", "Yritä uudelleen!" } }))
                    .type(APPLICATION_VND_MS_EXCEL)
                    .header("content-disposition", "inline; filename=yritauudelleen.xls").build();
        }
    }

}
