package fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.http.Callback;
import fi.vm.sade.valinta.kooste.external.resource.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppiBatch;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import rx.Observable;

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
				.put(Entity.entity(new HakemusPrototyyppiBatch(hakuOid, hakukohdeOid, tarjoajaOid, hakemusPrototyypit), MediaType.APPLICATION_JSON), new GenericType<List<Hakemus>>() {
				});
	}

	/**
	 * /haku-app/applications/listfull?appState=ACTIVE&appState=INCOMPLETE&rows=100000&asId={hakuOid}&aoOid={hakukohdeOid}
	 */
	@Override
	public Observable<List<Hakemus>> getApplicationsByOid(String hakuOid, String hakukohdeOid) {
		return getAsObservable("/applications/listfull", new TypeToken<List<Hakemus>>() {}.getType(), client -> client.query("rows", 100000).query("asId", hakuOid).query("aoOid", hakukohdeOid));
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

	@Override
	public Peruutettava getApplication(String hakemusOid, Consumer<Hakemus> callback, Consumer<Throwable> failureCallback) {
		String url = "/applications/" + hakemusOid;
		return new PeruutettavaImpl(getWebClient()
				.path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async()
				.get(new Callback<>(
						address, url, callback, failureCallback, TypeToken.of(Hakemus.class).getType())));
	}

	@Override
	public Peruutettava getApplicationsByOids(Collection<String> hakemusOids,
									   Consumer<List<Hakemus>> callback,
									   Consumer<Throwable> failureCallback) {
		String url = new StringBuilder().append("/applications/list")
				.toString();
		return new PeruutettavaImpl(getWebClient()
				.path(url)
				.query("rows", 100000)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async()
				.post(Entity.entity(Lists.newArrayList(hakemusOids), MediaType.APPLICATION_JSON_TYPE), new Callback<List<Hakemus>>(
						address,
						url+"?rows=100000",
						callback,
						failureCallback,
						new TypeToken<List<Hakemus>>() { }.getType())));
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
	public Peruutettava getApplicationAdditionalData(Collection<String> hakemusOids, Consumer<List<ApplicationAdditionalDataDTO>> callback, Consumer<Throwable> failureCallback) {
		String url = "/applications/additionalData";
		try {
			return new PeruutettavaImpl(
					getWebClient()
							.path(url)
							.async()
							.post(Entity.json(hakemusOids),new Callback<List<ApplicationAdditionalDataDTO>>(
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

	@Override
	public Observable<Response> putApplicationAdditionalData(String hakuOid, String hakukohdeOid, List<ApplicationAdditionalDataDTO> additionalData) {
		return putAsObservable("/applications/additionalData/" + hakuOid + "/" + hakukohdeOid, Entity.json(additionalData));
	}
}