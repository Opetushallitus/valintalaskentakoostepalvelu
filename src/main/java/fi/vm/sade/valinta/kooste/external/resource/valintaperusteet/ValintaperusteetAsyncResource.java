package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet;

import java.util.List;
import java.util.function.Consumer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.Callback;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ValintaperusteetAsyncResource {

	void haeHakijaryhmat(String hakukohdeOid,
			Consumer<List<ValintaperusteetHakijaryhmaDTO>> callback,
			Consumer<Throwable> failureCallback);

	// @GET
	// @Path("haku/{hakuOid}")
	// @Produces(MediaType.APPLICATION_JSON)
	void haunHakukohteet(String hakuOid,
			Consumer<List<HakukohdeViiteDTO>> callback,
			Consumer<Throwable> failureCallback);

	// @GET
	// @Path("{hakukohdeOid}")
	// @Produces(MediaType.APPLICATION_JSON)
	void haeValintaperusteet(
			String hakukohdeOid,
			//
			Integer valinnanVaiheJarjestysluku,
			Consumer<List<ValintaperusteetDTO>> callback,
			Consumer<Throwable> failureCallback);

}
