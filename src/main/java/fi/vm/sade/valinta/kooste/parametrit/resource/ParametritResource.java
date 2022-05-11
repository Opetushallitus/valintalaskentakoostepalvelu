package fi.vm.sade.valinta.kooste.parametrit.resource;

import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import fi.vm.sade.valinta.kooste.parametrit.dto.ParametritUIDTO;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component("ParametritResource")
@Path("/parametrit")
@PreAuthorize("isAuthenticated()")
@Api(value = "/parametrit", description = "Ohjausparametrit palveluiden aktiviteettipäivämäärille")
public class ParametritResource {

  private static final Logger LOG = LoggerFactory.getLogger(ParametritResource.class);

  @Autowired private HakuParametritService hakuParametritService;
  @Autowired private TarjontaAsyncResource tarjontaAsyncResource;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{hakuOid}")
  @ApiOperation(value = "Parametrien listaus", response = Response.class)
  public Response listParametrit(@PathParam("hakuOid") String hakuOid) {

    ParametritUIDTO resp = new ParametritUIDTO();
    ParametritParser parser = hakuParametritService.getParametritForHaku(hakuOid);
    resp.pistesyotto = parser.pistesyottoEnabled();
    resp.hakeneet = parser.hakeneetEnabled();
    resp.harkinnanvaraiset = parser.harkinnanvaraisetEnabled();
    resp.valintakoekutsut = parser.valintakoekutsutEnabled();
    resp.valintalaskenta = parser.valintalaskentaEnabled();
    resp.valinnanhallinta = parser.valinnanhallintaEnabled();
    resp.hakijaryhmat = parser.hakijaryhmatEnabled();
    resp.koetulostentallennus = parser.koetulostenTallentaminenEnabled();
    resp.koekutsujenmuodostaminen = parser.koekutsujenMuodostaminenEnabled();
    resp.harkinnanvarainenpaatostallennus = parser.harkinnanvarainenPaatosTallennusEnabled();

    return Response.ok(resp).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/hakukohderyhmat/{hakukohdeOid}")
  @ApiOperation(value = "Hakukohteen hakukohderyhmät", response = Response.class)
  public Response listHakukohderyhmat(@PathParam("hakukohdeOid") String hakukohdeOid) {

    CompletableFuture<List<String>> resultF =
        tarjontaAsyncResource.hakukohdeRyhmasForHakukohde(hakukohdeOid);
    try {
      List<String> result = resultF.get(1, TimeUnit.MINUTES);
      return Response.ok(result).build();
    } catch (Exception e) {
      LOG.error("Jotain meni vikaan hakukohderyhmien haussa", e);
      return Response.serverError().build();
    }
  }
}
