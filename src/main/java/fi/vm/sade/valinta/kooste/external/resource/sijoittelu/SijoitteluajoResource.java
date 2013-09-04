package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.Hakukohde;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.SijoitteluAjo;

@Path("/")
public interface SijoitteluajoResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sijoitteluajo/{sijoitteluajoId}/hakukohde/{hakukohdeOid}")
    Hakukohde getHakukohdeBySijoitteluajo(@PathParam("sijoitteluajoId") Long sijoitteluajoId,
            @PathParam("hakukohdeOid") String hakukohdeOid);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sijoitteluajo/{sijoitteluajoId}/hakemus/{hakemusOid}")
    List<HakemusDTO> getHakemusBySijoitteluajo(@PathParam("sijoitteluajoId") Long sijoitteluajoId,
            @PathParam("hakemusOid") String hakemusOid);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sijoittelu/{hakuOid}/sijoitteluajo")
    List<SijoitteluAjo> getSijoitteluajoByHakuOid(@PathParam("hakuOid") String hakuOid,
            @QueryParam("latest") Boolean latest);

}
