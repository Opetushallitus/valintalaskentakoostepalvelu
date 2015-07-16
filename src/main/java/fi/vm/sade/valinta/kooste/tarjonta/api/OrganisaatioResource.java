package fi.vm.sade.valinta.kooste.tarjonta.api;

import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import javax.ws.rs.DefaultValue;

/**
 * Kopio rajapinnasta fi.vm.sade.organisaatio.resource.OrganisaatioResource
 * ilman metodia searchBasic koska @QueryParam(empty) (eli @QueryParam(""))
 * toimii vaan yhteen suuntaan.
 */
@Path("/organisaatio")
public interface OrganisaatioResource {

    /**
     * NOTE: USED BY SECURITY FRAMEWORK - DON'T CHANGE Find oids of
     * organisaatio's parents, result oids start from root, ends to given oid
     * itself, and are separated by '/'.
     *
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{oid}/parentoids")
    public String parentoids(@PathParam("oid") String oid) throws Exception;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/hello")
    public String hello();

    /**
     * Get list of Organisaatio oids mathching the query.
     * <p/>
     * Search terms:
     * <ul>
     * <li>searchTerms=type=KOULUTUSTOIMIJA / OPPILAITOS / TOIMIPISTE ==
     * OrganisaatioTyyppi.name()</li>
     * </ul>
     *
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public List<String> search(@QueryParam("searchTerms") String searchTerms, @QueryParam("count") int count,
            @QueryParam("startIndex") int startIndex, @QueryParam("lastModifiedBefore") Date lastModifiedBefore,
            @QueryParam("lastModifiedSince") Date lastModifiedSince);

    /**
     * Organisaatio DTO as JSON.
     *
     * @param oid OID or Y-TUNNUS or VIRASTOTUNNUS or OPETUSPISTEKOODI or
     * TOIMIPISTEKOODI
     */
    @GET
    @Path("{oid}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public OrganisaatioRDTO getOrganisaatioByOID(@PathParam("oid") String oid);

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Path("/{oid}/children")
    public List<OrganisaatioRDTO> children(
            @PathParam("oid") String oid,
            @DefaultValue("false") @QueryParam("includeImage") boolean includeImage) throws Exception;

}
