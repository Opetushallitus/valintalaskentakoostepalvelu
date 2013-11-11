package fi.vm.sade.valinta.kooste.external.resource.haku;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusList;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * User: wuoti Date: 3.9.2013 Time: 9.40
 */
@Path("/applications")
public interface ApplicationResource {
    public static final String CHARSET_UTF_8 = ";charset=UTF-8";
    public static final String OID = "oid";
    public static final String HENKILOTUNNUS = "Henkilotunnus";

    @GET
    @Path("{oid}")
    @Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD')")
    public Hakemus getApplicationByOid(@PathParam(OID) String oid);

    @GET
    @Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD')")
    public HakemusList findApplications(@DefaultValue(value = "") @QueryParam("q") String query,
            @QueryParam("appState") List<String> state, @QueryParam("aoid") String aoid,
            @QueryParam("lopoid") String lopoid, @QueryParam("asId") String asId, @QueryParam("aoOid") String aoOid,
            @DefaultValue(value = "0") @QueryParam("start") int start,
            @DefaultValue(value = "100") @QueryParam("rows") int rows);
}
