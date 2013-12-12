package fi.vm.sade.valinta.kooste.valintakokeet.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.valintakokeet.route.HakukohteenValintakoelaskentaRoute;
import fi.vm.sade.valinta.kooste.valintakokeet.route.HaunValintakoelaskentaRoute;

/**
 * User: wuoti Date: 2.9.2013 Time: 12.28
 */
@Controller
@Path("valintakoelaskenta")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintakoelaskenta", description = "Valintakoelaskennan aktivointi")
public class HaunValintakoelaskentaAktivointiResource {
    private static final Logger LOG = LoggerFactory.getLogger(HaunValintakoelaskentaAktivointiResource.class);

    @Autowired
    private HaunValintakoelaskentaRoute haunValintakoelaskentaRoute;

    @Autowired
    private HakukohteenValintakoelaskentaRoute hakukohteenValintakoelaskentaRoute;

    @GET
    @Path("aktivoiHaunValintakoelaskenta")
    @ApiOperation(value = "Aktivoi valintakoelaskenta koko haulle", response = String.class)
    public String aktivoiHaunValintakoelaskenta(@QueryParam("hakuOid") String hakuOid) {
        if (StringUtils.isBlank(hakuOid)) {
            return "get parameter 'hakuOid' required";
        } else {
            LOG.info("Valintakoelaskenta for haku {}", hakuOid);
            haunValintakoelaskentaRoute.aktivoiValintakoelaskenta(hakuOid);
            return "in progress";
        }
    }

    @GET
    @Path("aktivoiHakukohteenValintakoelaskenta")
    @ApiOperation(value = "Aktivoi valintakoelaskenta hakukohteelle", response = String.class)
    public String aktivoiHakukohteenValintakoelaskenta(@QueryParam("hakukohdeOid") String hakukohdeOid) {
        if (StringUtils.isBlank(hakukohdeOid)) {
            return "get parameter 'hakukohdeOid' required";
        } else {
            LOG.info("Valintakoelaskenta for hakukohde {}", hakukohdeOid);
            hakukohteenValintakoelaskentaRoute.aktivoiValintakoelaskenta(hakukohdeOid);
            return "in progress";
        }
    }
}
