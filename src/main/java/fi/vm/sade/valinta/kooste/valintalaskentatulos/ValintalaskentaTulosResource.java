package fi.vm.sade.valinta.kooste.valintalaskentatulos;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

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

    public final static MediaType APPLICATION_VND_MS_EXCEL = new MediaType("application", "vnd.ms-excel");

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
            return Response.serverError().build();// ok(input,
                                                  // APPLICATION_VND_MS_EXCEL).build();
        }
    }
}
