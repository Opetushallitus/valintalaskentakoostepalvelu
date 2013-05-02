package fi.vm.sade.valinta.kooste.sijoittelu;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 *
 */
@Controller
@Path("/sijoittelu")
public class SijoitteluAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(SijoitteluAktivointiResource.class);

    @Autowired
    private SijoitteluAktivointiProxy sijoitteluaAktivointiProxy;

    @GET
    @Path("/aktivoi")
    public String aktivoiSijoittelu(@QueryParam("hakuOid") String hakuOid) {

        if(StringUtils.isBlank(hakuOid)) {
            return "get parameter 'hakuOid' required";
        } else {
            LOG.info("aktivoiSijoittelu haulle {}", hakuOid);
            sijoitteluaAktivointiProxy.aktivoiSijoittelu(hakuOid);
            return "in progress";
        }
    }
}
