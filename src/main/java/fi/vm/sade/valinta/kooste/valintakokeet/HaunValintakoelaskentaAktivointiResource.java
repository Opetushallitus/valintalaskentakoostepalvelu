package fi.vm.sade.valinta.kooste.valintakokeet;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 * User: wuoti
 * Date: 2.9.2013
 * Time: 12.28
 */
@Controller
@Path("valintakoelaskenta")
public class HaunValintakoelaskentaAktivointiResource {
    private static final Logger LOG = LoggerFactory.getLogger(HaunValintakoelaskentaAktivointiResource.class);

    @Autowired
    private HaunValintakoelaskentaAktivointiProxy haunValintakoelaskentaAktivointiProxy;

    @Autowired
    private HakukohteenValintakoelaskentaAktivointiProxy hakukohteenValintakoelaskentaAktivointiProxy;

    @GET
    @Path("aktivoiHaunValintakoelaskenta")
    public String aktivoiHaunValintakoelaskenta(@QueryParam("hakuOid") String hakuOid) {
        if (StringUtils.isBlank(hakuOid)) {
            return "get parameter 'hakuOid' required";
        } else {
            LOG.info("Valintakoelaskenta for haku {}", hakuOid);
            haunValintakoelaskentaAktivointiProxy.aktivoiValintakoelaskenta(hakuOid);
            return "in progress";
        }
    }

    @GET
    @Path("aktivoiHakukohteenValintakoelaskenta")
    public String aktivoiHakukohteenValintakoelaskenta(@QueryParam("hakukohdeOid") String hakukohdeOid) {
        if (StringUtils.isBlank(hakukohdeOid)) {
            return "get parameter 'hakukohdeOid' required";
        } else {
            LOG.info("Valintakoelaskenta for hakukohde {}", hakukohdeOid);
            hakukohteenValintakoelaskentaAktivointiProxy.aktivoiValintakoelaskenta(hakukohdeOid);
            return "in progress";
        }
    }
}
