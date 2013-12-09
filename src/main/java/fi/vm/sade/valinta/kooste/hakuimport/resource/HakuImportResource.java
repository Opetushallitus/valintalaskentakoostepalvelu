package fi.vm.sade.valinta.kooste.hakuimport.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import fi.vm.sade.valinta.kooste.hakuimport.route.HakuImportRoute;
import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;

/**
 * User: wuoti Date: 20.5.2013 Time: 10.34
 */
@Path("hakuimport")
@Controller
@PreAuthorize("isAuthenticated()")
public class HakuImportResource {
    private static final Logger LOG = LoggerFactory.getLogger(HakuImportResource.class);

    @Autowired
    private HakuImportRoute hakuImportAktivointiRoute;

    @Autowired
    private ParametriService parametriService;

    @GET
    @Path("aktivoi")
    public String aktivoiHakuImport(@QueryParam("hakuOid") String hakuOid) {
        if (!parametriService.valinnanhallintaEnabled(hakuOid)) {
            return "no privileges.";
        }

        if (StringUtils.isBlank(hakuOid)) {
            return "get parameter 'hakuOid' required";
        } else {
            LOG.info("Haku import haulle {}", hakuOid);
            hakuImportAktivointiRoute.aktivoiHakuImport(hakuOid);
            return "in progress";
        }
    }
}
