package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ValintaperusteetAsyncResource {

	// @GET
	///valintaperusteet-service/resources/valintaperusteet/hakijaryhma/{}
	Peruutettava haeHakijaryhmat(String hakukohdeOid,
			Consumer<List<ValintaperusteetHakijaryhmaDTO>> callback,
			Consumer<Throwable> failureCallback);

	// @GET
	///valintaperusteet-service/resources/hakukohde/haku/{}
	Peruutettava haunHakukohteet(String hakuOid,
			Consumer<List<HakukohdeViiteDTO>> callback,
			Consumer<Throwable> failureCallback);

	// @GET
	///valintaperusteet-service/resources/valintaperusteet/{}
	Peruutettava haeValintaperusteet(
			String hakukohdeOid,
			//
			Integer valinnanVaiheJarjestysluku,
			Consumer<List<ValintaperusteetDTO>> callback,
			Consumer<Throwable> failureCallback);

	// @GET
	///valintaperusteet-service/resources/hakukohde/{hakukohdeOid}/ilmanlaskentaa/
	Future<List<ValinnanVaiheJonoillaDTO>> ilmanLaskentaa(String hakukohdeOid);

	// @POST
	///valintaperusteet-service/resources/valintaperusteet/tuoHakukohde/
	Future<Response> tuoHakukohde(HakukohdeImportDTO hakukohde);
	
	// @GET
	///valintaperusteet-service/resources/hakukohde/avaimet/{}
	Future<List<ValintaperusteDTO>> findAvaimet(String hakukohdeOid);
	
	// @POST
	///valintaperusteet-service/resources/valintakoe/
	Future<List<ValintakoeDTO>> haeValintakokeet(Collection<String> oids);
	
	// @POST
	///valintaperusteet-service/resources/hakukohde/valintakoe
	Future<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakukohteille(
			Collection<String> hakukohdeOids);
}
