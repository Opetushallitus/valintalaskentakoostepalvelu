package fi.vm.sade.valinta.kooste.external.resource.laskenta;

import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakuDTO;

import javax.ws.rs.*;
import java.util.List;

@Path("valintatieto")
public interface ValintatietoResource {

    @POST
    @Path("hakukohde/{hakukohdeOid}")
    @Consumes("application/json")
    @Produces("application/json")
    List<HakemusOsallistuminenDTO> haeValintatiedotHakukohteelle(@PathParam("hakukohdeOid") String hakukohdeOid, List<String> valintakoeOid);

    @GET
    @Path("haku/{hakuOid}")
    @Produces("application/json")
    HakuDTO haeValintatiedot(@PathParam("hakuOid") String hakuOid);
}
