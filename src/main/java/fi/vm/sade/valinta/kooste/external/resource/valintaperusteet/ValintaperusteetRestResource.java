package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("valintaperusteet")
public interface ValintaperusteetRestResource {

    @GET
    @Path("valintatapajono/{hakukohdeOid}")
    @Produces(MediaType.APPLICATION_JSON)
    List<ValintatapajonoDTO> haeValintatapajonotSijoittelulle(@PathParam("hakukohdeOid") String hakukohdeOid);

    @GET
    @Path("{hakukohdeOid}")
    @Produces(MediaType.APPLICATION_JSON)
    List<ValintaperusteetDTO> haeValintaperusteet(@PathParam("hakukohdeOid") String hakukohdeOid, @QueryParam("vaihe") Integer valinnanVaiheJarjestysluku);

    @POST
    @Path("tuoHakukohde")
    @Consumes(MediaType.APPLICATION_JSON)
    Response tuoHakukohde(HakukohdeImportDTO hakukohde);
}
