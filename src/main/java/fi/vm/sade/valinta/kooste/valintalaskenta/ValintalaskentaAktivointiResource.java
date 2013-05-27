package fi.vm.sade.valinta.kooste.valintalaskenta;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 * @author Jussi Jartamo
 */
@Controller
@Path("/valintalaskenta")
public class ValintalaskentaAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaAktivointiResource.class);

    @Autowired
    private HakukohteenValintalaskentaAktivointiProxy hakukohteenValintalaskentaAktivointiProxy;

    @Autowired
    private HaunValintalaskentaAktivointiProxy haunValintalaskentaAktivointiProxy;

    @GET
    @Path("/aktivoi")
    public String aktivoiHakukohteenValintalaskenta(@QueryParam("hakukohdeOid") String hakukohdeOid,
                                                    @QueryParam("valinnanvaihe") Integer valinnanvaihe) {

        if (StringUtils.isBlank(hakukohdeOid) || valinnanvaihe == null) {
            return "get parameter 'hakukohdeOid' and 'valinnanvaihe' required";
        } else {
            LOG.info("Valintalaskenta kohteelle {}", hakukohdeOid);
            hakukohteenValintalaskentaAktivointiProxy.aktivoiValintalaskenta(hakukohdeOid, valinnanvaihe);
            return "in progress";
        }
    }

    @GET
    @Path("aktivoiHaunValintalaskenta")
    public String aktivoiHaunValintalaskenta(@QueryParam("hakuOid") String hakuOid) {
        if (StringUtils.isBlank(hakuOid)) {
            return "get parameter 'hakuoid' required";
        } else {
            LOG.info("Suoritetaan valintalaskenta haulle {}", hakuOid);
            haunValintalaskentaAktivointiProxy.aktivoiValintalaskenta(hakuOid);
            return "in progress";
        }
    }
}
