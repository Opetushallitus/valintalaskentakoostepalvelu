package fi.vm.sade.tarjonta.service.resources.v1;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeHakutulosV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeValintaperusteetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakutuloksetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;

/**
 *         Wrapper luokalle
 *         fi.vm.sade.tarjonta.service.resources.v1.HakukohdeV1Resource
 * @QueryParam("") ei ole laillinen client annotaatio
 */
@Path("/v1/hakukohde")
public interface HakukohdeV1ResourceWrapper {

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public ResultV1RDTO<HakutuloksetV1RDTO<HakukohdeHakutulosV1RDTO>> search(
            @QueryParam("hakuOid") String hakuOid,
            @QueryParam("tila") List<String> hakukohdeTilas);

    @GET
    @Path("/{oid}/valintaperusteet")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    ResultV1RDTO<HakukohdeValintaperusteetV1RDTO> findValintaperusteetByOid(@PathParam("oid") String oid);
}
