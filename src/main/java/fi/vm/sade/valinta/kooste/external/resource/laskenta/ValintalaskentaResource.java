package fi.vm.sade.valinta.kooste.external.resource.laskenta;

import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("valintalaskenta")
public interface ValintalaskentaResource {
  // @PreAuthorize(CRUD)
  @POST
  @Path("laske")
  @Consumes("application/json")
  @Produces("text/plain")
  String laske(LaskeDTO laskeDTO);

  // @PreAuthorize(CRUD)
  @POST
  @Path("valintakokeet")
  @Consumes("application/json")
  @Produces("text/plain")
  String valintakokeet(LaskeDTO laskeDTO);

  @POST
  @Path("laskekaikki")
  @Consumes("application/json")
  @Produces("text/plain")
  String laskeKaikki(LaskeDTO laskeDTO);
}
