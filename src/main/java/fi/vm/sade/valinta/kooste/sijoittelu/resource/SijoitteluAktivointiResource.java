package fi.vm.sade.valinta.kooste.sijoittelu.resource;

import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;
import fi.vm.sade.valinta.kooste.sijoittelu.Sijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.JatkuvaSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.proxy.SijoitteluAktivointiProxy;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 *
 */
@Controller
@Path("sijoittelu")
@PreAuthorize("isAuthenticated()")
public class SijoitteluAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(SijoitteluAktivointiResource.class);
    public static final String OPH_CRUD = "ROLE_APP_SIJOITTELU_CRUD_1.2.246.562.10.00000000001";

    @Autowired
    private SijoitteluAktivointiProxy sijoitteluAktivointiProxy;

    @Autowired
    private ParametriService parametriService;

    @GET
    @Path("aktivoi")
    public String aktivoiSijoittelu(@QueryParam("hakuOid") String hakuOid) {
        if(!parametriService.valinnanhallintaEnabled(hakuOid)) {
            return "no privileges.";
        }

        if(StringUtils.isBlank(hakuOid)) {
            return "get parameter 'hakuOid' required";
        } else {
            LOG.info("aktivoiSijoittelu haulle {}", hakuOid);
            sijoitteluAktivointiProxy.aktivoiSijoittelu(hakuOid);
            return "in progress";
        }
    }

    @GET
    @Path("jatkuva/aktivoi")
    @Secured({OPH_CRUD})
    public String aktivoiJatkuvassaSijoittelussa(@QueryParam("hakuOid") String hakuOid) {
        if(!parametriService.valinnanhallintaEnabled(hakuOid)) {
            return "no privileges.";
        }

        if(StringUtils.isBlank(hakuOid)) {
            return "get parameter 'hakuOid' required";
        } else {
            LOG.info("jatkuva sijoittelu aktivoitu haulle {}", hakuOid);
            // TODO: käyttöoikeus hakuun ja tarkastus samalla onko hakukohdetta
            Sijoittelu sijoittelu = JatkuvaSijoittelu.SIJOITTELU_HAUT.get(hakuOid);
            if(sijoittelu == null) {
                sijoittelu = new Sijoittelu();
                sijoittelu.setHakuOid(hakuOid);
                JatkuvaSijoittelu.SIJOITTELU_HAUT.put(hakuOid, sijoittelu);
            }
            return "aktivoitu";
        }
    }

    @GET
    @Path("jatkuva/poista")
    @Secured({OPH_CRUD})
    public String poistaJatkuvastaSijoittelusta(@QueryParam("hakuOid") String hakuOid) {
        if(!parametriService.valinnanhallintaEnabled(hakuOid)) {
            return "no privileges.";
        }

        if(StringUtils.isBlank(hakuOid)) {
            return "get parameter 'hakuOid' required";
        } else {
            LOG.info("jatkuva sijoittelu poistettu haulta {}", hakuOid);
            Sijoittelu remove = JatkuvaSijoittelu.SIJOITTELU_HAUT.remove(hakuOid);
            if(remove == null) {
                return "hakua ei löytynyt";
            }
            return "poistettu";
        }
    }

    @GET
    @Path("jatkuva/aktiiviset")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured({OPH_CRUD})
    public Map<String, Sijoittelu> aktiivisetSijoittelut() {
        return JatkuvaSijoittelu.SIJOITTELU_HAUT;
    }

    @GET
    @Path("jatkuva")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured({OPH_CRUD})
    public Sijoittelu jatkuvaTila(@QueryParam("hakuOid") String hakuOid) {
        if(StringUtils.isBlank(hakuOid)) {
            return null;
        } else {
            return JatkuvaSijoittelu.SIJOITTELU_HAUT.get(hakuOid);
        }
    }
}
