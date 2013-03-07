package fi.vm.sade.valinta.kooste.paasykokeet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Controller
@Path("/valintakoelaskenta")
public class ValintakoelaskentaAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(ValintakoelaskentaAktivointiResource.class);

    @Autowired
    private ValintakoelaskentaAktivointiProxy valintalaskentaProxy;

    @GET
    @Path("/aktivoi")
    public void aktivoiValintalaskenta(@QueryParam("hakukohdeOid") String hakukohdeOid) {
        valintalaskentaProxy.aktivoiValintakoelaskenta(hakukohdeOid);
    }
}
