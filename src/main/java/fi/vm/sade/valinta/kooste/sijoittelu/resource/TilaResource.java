package fi.vm.sade.valinta.kooste.sijoittelu.resource;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.valinta.kooste.sijoittelu.dto.Valintatulos;

@Path("tila")
public interface TilaResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{hakemusOid}")
    public List<Valintatulos> hakemus(@PathParam("hakemusOid") String hakemusOid);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{hakemusOid}/{hakuOid}/{hakukohdeOid}/{valintatapajonoOid}/")
    public Valintatulos hakemus(@PathParam("hakuOid") String hakuOid, @PathParam("hakukohdeOid") String hakukohdeOid,
            @PathParam("valintatapajonoOid") String valintatapajonoOid, @PathParam("hakemusOid") String hakemusOid);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/hakukohde/{hakukohdeOid}/{valintatapajonoOid}/")
    public List<Valintatulos> haku(@PathParam("hakukohdeOid") String hakukohdeOid,
            @PathParam("valintatapajonoOid") String valintatapajonoOid);

}
