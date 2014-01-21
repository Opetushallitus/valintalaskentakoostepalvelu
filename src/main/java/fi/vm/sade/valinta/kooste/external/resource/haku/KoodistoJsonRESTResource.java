package fi.vm.sade.valinta.kooste.external.resource.haku;

import fi.vm.sade.generic.rest.Cacheable;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.KoodistoUrheilija;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Created by kjsaila on 20/01/14.
 */
@Path("/json")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public interface KoodistoJsonRESTResource {

    public static final String KOODI_VERSIO = "koodiVersio";
    public static final String KOODI_URI = "koodiUri";
    public static final int ONE_HOUR = 60 * 60;

    @GET
    @Path("/relaatio/sisaltyy-alakoodit/{koodiUri}")
    @Cacheable(maxAgeSeconds = ONE_HOUR)
    public List<KoodistoUrheilija> getAlakoodis(@PathParam(KOODI_URI) String koodiUri,
                                       @QueryParam(KOODI_VERSIO) Integer koodiVersio);
}
