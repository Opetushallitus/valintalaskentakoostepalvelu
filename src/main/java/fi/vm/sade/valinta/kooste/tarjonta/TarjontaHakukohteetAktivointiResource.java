package fi.vm.sade.valinta.kooste.tarjonta;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang.StringUtils;
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
@Path("/tarjonta")
public class TarjontaHakukohteetAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(TarjontaHakukohteetAktivointiResource.class);

    @Autowired
    private TarjontaHakukohteetAktivointiProxy hakukohteetProxy;

    @GET
    @Path("/aktivoi")
    public String aktivoiValintalaskenta(@QueryParam("hakuOid") String hakuOid) {
        if (StringUtils.isBlank(hakuOid)) {
            return "get parameter 'hakukohdeOid required";
        } else {
            LOG.info("Haetaan tarjonnan hakukohteet for {}", hakuOid);
            hakukohteetProxy.aktivoiTarjontaHakukohteet(hakuOid);
            return "in progress";
        }
    }
}
