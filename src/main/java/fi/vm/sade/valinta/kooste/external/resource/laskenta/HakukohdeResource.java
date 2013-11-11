package fi.vm.sade.valinta.kooste.external.resource.laskenta;

import fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.ValinnanvaiheDTO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

public interface HakukohdeResource {

    @GET
    @Path("hakukohde/{hakukohdeoid}/valinnanvaihe")
    @Produces(MediaType.APPLICATION_JSON)
    List<ValinnanvaiheDTO> hakukohde(@PathParam("hakukohdeoid") String hakukohdeoid);
}
