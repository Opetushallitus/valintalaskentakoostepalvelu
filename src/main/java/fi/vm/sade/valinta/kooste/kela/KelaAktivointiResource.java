package fi.vm.sade.valinta.kooste.kela;

import java.io.InputStream;
import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import fi.vm.sade.rajapinnat.kela.tkuva.util.KelaUtil;
import fi.vm.sade.valinta.kooste.kela.proxy.TKUVAYHVAExportProxy;

@Path("kela")
@Controller
public class KelaAktivointiResource {

    public final static MediaType APPLICATION_TKUVAYHVA = new MediaType("application", "TKUVA.YHVA14");

    @Autowired
    private TKUVAYHVAExportProxy kelaExportProxy;

    @GET
    @Path("TKUVAYHVA/aktivoi")
    public Response aktivoiKelaTiedostonluonti(@QueryParam("hakuOid") String hakuOid) {
        try {

            InputStream input = kelaExportProxy.luoTKUVAYHVA(hakuOid);
            return Response.ok(input, APPLICATION_TKUVAYHVA)
                    .header("content-disposition", "inline; filename=" + KelaUtil.createTiedostoNimiYhva14(new Date()))
                    .build();
        } catch (Exception e) {
            return Response.noContent().build();
            // ok(input, APPLICATION_TKUVAYHVA).header("content-disposition",
            // "inline; filename=").build();
        }
    }
}
