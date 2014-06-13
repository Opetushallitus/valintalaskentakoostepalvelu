package fi.vm.sade.valinta.kooste.external.resource.haku;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.security.access.prepost.PreAuthorize;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusList;

/**
 * User: wuoti Date: 3.9.2013 Time: 9.40
 */
@Path("/applications")
public interface ApplicationResource {
	public static final String CHARSET_UTF_8 = ";charset=UTF-8";
	public static final String OID = "oid";
	public static final String HENKILOTUNNUS = "Henkilotunnus";
	public static final String SYNTYMAAIKA = "syntymaaika";
	public static final String ACTIVE = "ACTIVE";
	public static final String INCOMPLETE = "INCOMPLETE";
	public static final List<String> ACTIVE_AND_INCOMPLETE = Collections
			.unmodifiableList(Arrays.asList(ACTIVE, INCOMPLETE));

	@GET
	@Path("{oid}")
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD')")
	public Hakemus getApplicationByOid(@PathParam(OID) String oid);

	@GET
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD')")
	public HakemusList findApplications(
			@DefaultValue(value = "") @QueryParam("q") String query,
			@QueryParam("appState") List<String> state,
			@QueryParam("aoid") String aoid,
			@QueryParam("lopoid") String lopoid,
			@QueryParam("asId") String asId, @QueryParam("aoOid") String aoOid,
			@DefaultValue(value = "0") @QueryParam("start") int start,
			@DefaultValue(value = "100") @QueryParam("rows") int rows);

	@GET
	@Path("listfull")
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	// @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	public List<Hakemus> getApplicationsByOid(
			@QueryParam("aoOid") String aoOid,
			@QueryParam("appState") List<String> appStates,
			@QueryParam("rows") int rows);

	@POST
	@Path("list")
	@Consumes(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	// @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	public List<Hakemus> getApplicationsByOids(List<String> oids);

	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	// @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	public List<Hakemus> getApplicationsByOidsGet(
			@QueryParam("oid") List<String> oids);

	@GET
	@Path("additionalData/{asId}/{aoId}")
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	//
	public List<ApplicationAdditionalDataDTO> getApplicationAdditionalData(
			@PathParam("asId") String hakuOid,
			@PathParam("aoId") String hakukohdeOid);

	@PUT
	@Path("additionalData/{asId}/{aoId}")
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	@Consumes(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	//
	public void putApplicationAdditionalData(@PathParam("asId") String hakuOid,
			@PathParam("aoId") String hakukohdeOid,
			List<ApplicationAdditionalDataDTO> additionalData);
}
