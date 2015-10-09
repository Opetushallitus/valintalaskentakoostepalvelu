package fi.vm.sade.valinta.kooste.parametrit.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import fi.vm.sade.valinta.kooste.parametrit.dto.ParametritUIDTO;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Component("ParametritResource")
@Path("/parametrit")
@PreAuthorize("isAuthenticated()")
@Api(value = "/parametrit", description = "Ohjausparametrit palveluiden aktiviteettipäivämäärille")
public class ParametritResource {

    private static final Logger LOG = LoggerFactory.getLogger(ParametritResource.class);

    @Autowired
    private HakuParametritService hakuParametritService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{hakuOid}")
    @ApiOperation(value = "Parametrien listaus", response = Response.class)
    public Response listParametrit(@PathParam("hakuOid") String hakuOid) {


/*        Map<String, Object> params = new HashMap<String, Object>();
        params.put("pistesyotto", parametriService.pistesyottoEnabled(hakuOid));
        params.put("hakeneet", parametriService.hakeneetEnabled(hakuOid));
        params.put("harkinnanvaraiset", parametriService.harkinnanvaraisetEnabled(hakuOid));
        params.put("valintakoekutsut", parametriService.valintakoekutsutEnabled(hakuOid));
        params.put("valintalaskenta", parametriService.valintalaskentaEnabled(hakuOid));
        params.put("valinnanhallinta", parametriService.valinnanhallintaEnabled(hakuOid));
        params.put("hakijaryhmat", parametriService.hakijaryhmatEnabled(hakuOid));
*/

        ParametritUIDTO resp = new ParametritUIDTO();
        ParametritParser parser = hakuParametritService.getParametritForHaku(hakuOid);
        resp.pistesyotto = parser.pistesyottoEnabled();
        resp.hakeneet = parser.hakeneetEnabled();
        resp.harkinnanvaraiset = parser.harkinnanvaraisetEnabled();
        resp.valintakoekutsut = parser.valintakoekutsutEnabled();
        resp.valintalaskenta = parser.valintalaskentaEnabled();
        resp.valinnanhallinta = parser.valinnanhallintaEnabled();
        resp.hakijaryhmat = parser.hakijaryhmatEnabled();

        return Response.ok(resp).build();
    }
}
