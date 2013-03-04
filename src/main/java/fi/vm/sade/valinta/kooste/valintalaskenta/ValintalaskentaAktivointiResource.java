package fi.vm.sade.valinta.kooste.valintalaskenta;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Controller
@Path("/valintalaskenta")
public class ValintalaskentaAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaAktivointiResource.class);

    @Autowired
    private ValintalaskentaAktivointiProxy valintalaskentaProxy;

    @GET
    @Path("/aktivoi")
    public void aktivoiValintalaskenta(@QueryParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("valinnanvaihe") Integer valinnanvaihe) {
        valintalaskentaProxy.aktivoiValintalaskenta(hakukohdeOid, valinnanvaihe);
    }
}
