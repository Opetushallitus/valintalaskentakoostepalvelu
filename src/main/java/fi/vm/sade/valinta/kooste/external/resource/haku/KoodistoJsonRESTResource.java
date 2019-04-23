package fi.vm.sade.valinta.kooste.external.resource.haku;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.KoodistoUrheilija;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/json")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public interface KoodistoJsonRESTResource {
    String KOODI_VERSIO = "koodiVersio";
    String KOODI_URI = "koodiUri";

    @GET
    @Path("/relaatio/sisaltyy-alakoodit/{koodiUri}")
    List<KoodistoUrheilija> getAlakoodis(@PathParam(KOODI_URI) String koodiUri, @QueryParam(KOODI_VERSIO) Integer koodiVersio);
}
