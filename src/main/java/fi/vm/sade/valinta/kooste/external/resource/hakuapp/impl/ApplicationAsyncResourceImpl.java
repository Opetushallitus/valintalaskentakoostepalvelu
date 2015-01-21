package fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppiBatch;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.AsyncResourceWithCas;

/**
 *
 * @author Jussi Jartamo
 */
@Service
public class ApplicationAsyncResourceImpl extends AsyncResourceWithCas implements ApplicationAsyncResource {
	@Autowired
	public ApplicationAsyncResourceImpl(
			@Value("${web.url.cas}") String webCasUrl,
			@Value("${cas.service.haku-service}/j_spring_cas_security_check") String targetService,
			@Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String appClientUsername,
			@Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String appClientPassword,
			@Value("${valintalaskentakoostepalvelu.hakemus.rest.url}") String address,
			ApplicationContext context
	) {
		super(webCasUrl, targetService, appClientUsername, appClientPassword, address, context, TimeUnit.HOURS.toMillis(1));
	}

	@Override
	public Future<List<Hakemus>> putApplicationPrototypes(String hakuOid, String hakukohdeOid, String tarjoajaOid, Collection<HakemusPrototyyppi> hakemusPrototyypit) {
		String url = "/applications/syntheticApplication";
		return getWebClient().path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async()
				.put(Entity.entity(new HakemusPrototyyppiBatch(hakuOid, hakukohdeOid, tarjoajaOid, hakemusPrototyypit), MediaType.APPLICATION_JSON),new GenericType<List<Hakemus>>() { });
	}

	@Override
	public Future<List<ApplicationAdditionalDataDTO>> getApplicationAdditionalData(String hakuOid, String hakukohdeOid) {
		String url = new StringBuilder()
				.append("/applications/additionalData/").append(hakuOid)
				.append("/").append(hakukohdeOid).toString();
		return getWebClient().path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async()
				.get(new GenericType<List<ApplicationAdditionalDataDTO>>() { });
	}

	@Override
	public Future<List<Hakemus>> getApplicationsByOid(String hakuOid,
			String hakukohdeOid) {
		String url = new StringBuilder().append("/applications/listfull")
				.toString();
		return getWebClient().path(url)
				.query("appState", "ACTIVE", "INCOMPLETE")
				.query("rows", 100000)
				.query("asId", hakuOid)
				.query("aoOid", hakukohdeOid)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async()
				.get(new GenericType<List<Hakemus>>() { });
	}

	@Override
	public Future<List<Hakemus>> getApplicationsByOids(Collection<String> hakemusOids) {
		String url = new StringBuilder().append("/applications/list")
				.toString();
		return getWebClient()
				.path(url)
				.query("rows", 100000)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async()
				.post(Entity.entity(Lists.newArrayList(hakemusOids), MediaType.APPLICATION_JSON_TYPE), new GenericType<List<Hakemus>>() { });
    }

	public Peruutettava getApplicationsByOid(String hakuOid, String hakukohdeOid, Consumer<List<Hakemus>> callback, Consumer<Throwable> failureCallback) {
		String url = "/applications/listfull";
		try {
			return new PeruutettavaImpl(
					getWebClient()
							.path(url)
							.query("appState", "ACTIVE", "INCOMPLETE")
							.query("rows", 100000)
							.query("asId", hakuOid)
							.query("aoOid", hakukohdeOid)
							.async()
							.get(new Callback<List<Hakemus>>(
									address,
									url+"?appStates=ACTIVE&appStates=INCOMPLETE&rows=100000&aoOid="+hakukohdeOid+"&asId="+hakuOid,
                                    callback,
									failureCallback,
									new TypeToken<List<Hakemus>>() { }.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

	public Peruutettava getApplicationAdditionalData(String hakuOid, String hakukohdeOid, Consumer<List<ApplicationAdditionalDataDTO>> callback, Consumer<Throwable> failureCallback) {
		String url = "/applications/additionalData/" + hakuOid + "/" + hakukohdeOid;
		try {
			return new PeruutettavaImpl(
					getWebClient()
							.path(url)
							.async()
							.get(new Callback<List<ApplicationAdditionalDataDTO>>(
									address,
									url,
									callback,
									failureCallback,
									new TypeToken<List<ApplicationAdditionalDataDTO>>() { }.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}
}