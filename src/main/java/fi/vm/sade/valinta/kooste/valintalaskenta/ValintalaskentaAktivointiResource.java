package fi.vm.sade.valinta.kooste.valintalaskenta;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;

/**
 * @author Jussi Jartamo
 */
@Controller
@Path("valintalaskenta")
public class ValintalaskentaAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaAktivointiResource.class);

    @Autowired
    private HakukohteenValintalaskentaAktivointiProxy hakukohteenValintalaskentaAktivointiProxy;

    @Autowired
    private HaunValintalaskentaAktivointiProxy haunValintalaskentaAktivointiProxy;

    @Autowired
    private ParametriService parametriService;

    @Value("${valintalaskentakoostepalvelu.valintaperusteService.url}")
    String valintaperusteServiceUrl;

    @Value("${valintalaskentakoostepalvelu.valintalaskentaService.url}")
    String valintalaskentaServiceUrl;

    @GET
    @Path("aktivoi")
    public Response aktivoiHakukohteenValintalaskenta(@QueryParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("valinnanvaihe") Integer valinnanvaihe) {
        try {
            if (StringUtils.isBlank(hakukohdeOid) || valinnanvaihe == null) {
                return Response.status(Response.Status.OK)
                        .entity("get parameter 'hakukohdeOid' and 'valinnanvaihe' required").build();
            } else {
                LOG.info("Valintalaskenta kohteelle {}", hakukohdeOid);
                hakukohteenValintalaskentaAktivointiProxy.aktivoiValintalaskenta(hakukohdeOid, valinnanvaihe);
                return Response.status(Response.Status.OK).entity("in progress").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Error aktivoi: {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("aktivoiHaunValintalaskenta")
    public Response aktivoiHaunValintalaskenta(@QueryParam("hakuOid") String hakuOid) {
        if (!parametriService.valintalaskentaEnabled(hakuOid)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            if (StringUtils.isBlank(hakuOid)) {
                return Response.status(Response.Status.OK).entity("get parameter 'hakuoid' required").build();
            } else {
                LOG.info("Suoritetaan valintalaskenta haulle {}", hakuOid);
                haunValintalaskentaAktivointiProxy.aktivoiValintalaskenta(hakuOid);
                return Response.status(Response.Status.OK).entity("in progress").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Error aktivoiHaunValintalaskenta: {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
