package fi.vm.sade.valinta.kooste.parametrit.resource;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.parametrit.Parametrit;
import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;

/**
 * User: tommiha Date: 8/20/13 Time: 2:51 PM
 */
@Component
@Path("/parametrit")
@PreAuthorize("isAuthenticated()")
@Api(value = "/parametrit", description = "Ohjausparametrit palveluiden aktiviteettipäivämäärille")
public class ParametritResource {

    @Autowired
    private Parametrit parametrit;

    @Autowired
    private ParametriService parametriService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{hakuOid}")
    @ApiOperation(value = "Parametrien listaus", response = Response.class)
    public Response listParametrit(@PathParam("hakuOid") String hakuOid) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("pistesyotto", parametriService.pistesyottoEnabled(hakuOid));
        params.put("hakeneet", parametriService.hakeneetEnabled(hakuOid));
        params.put("harkinnanvaraiset", parametriService.harkinnanvaraisetEnabled(hakuOid));
        params.put("valintakoekutsut", parametriService.valintakoekutsutEnabled(hakuOid));
        params.put("valintalaskenta", parametriService.valintalaskentaEnabled(hakuOid));
        params.put("valinnanhallinta", parametriService.valinnanhallintaEnabled(hakuOid));
        params.put("hakijaryhmat", parametriService.hakijaryhmatEnabled(hakuOid));
        return Response.ok(params).build();
    }
}
