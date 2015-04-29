package fi.vm.sade.valinta.kooste.external.resource.hakuapp;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ApplicationAsyncResource {

	Future<List<ApplicationAdditionalDataDTO>> getApplicationAdditionalData(
			String hakuOid, String hakukohdeOid);

	Future<List<Hakemus>> getApplicationsByOid(String hakuOid,
			String hakukohdeOid);

	Future<List<Hakemus>> putApplicationPrototypes(String hakuOid, String hakukohdeOid, String tarjoajaOid, Collection<HakemusPrototyyppi> hakemusPrototyypit);
	
	Future<List<Hakemus>> getApplicationsByOids(Collection<String> hakemusOids);

	Peruutettava getApplicationsByOid(String hakuOid, String hakukohdeOid,
			Consumer<List<Hakemus>> callback,
			Consumer<Throwable> failureCallback);

	Peruutettava getApplicationAdditionalData(String hakuOid,
			String hakukohdeOid,
			Consumer<List<ApplicationAdditionalDataDTO>> callback,
			Consumer<Throwable> failureCallback);

	Peruutettava getApplicationAdditionalData(Collection<String> hakemusOids, Consumer<List<ApplicationAdditionalDataDTO>> callback, Consumer<Throwable> failureCallback);

	Peruutettava putApplicationAdditionalData(String hakuOid,
											 String hakukohdeOid,
											 List<ApplicationAdditionalDataDTO> additionalData,
											 Consumer<Response> callback, Consumer<Throwable> failureCallback);

}
