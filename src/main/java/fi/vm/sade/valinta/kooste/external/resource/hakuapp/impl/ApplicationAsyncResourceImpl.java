package fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         ApplicationResource
 */
@Service
public class ApplicationAsyncResourceImpl implements ApplicationAsyncResource {
	private final WebClient webClient;
	private final String address;

	@Autowired
	public ApplicationAsyncResourceImpl(
			//
			@Value("${web.url.cas}") String webCasUrl,
			//
			@Value("${cas.service.haku-service}/j_spring_cas_security_check") String targetService,
			//
			@Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String appClientUsername,
			//
			@Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String appClientPassword,
			@Value("${valintalaskentakoostepalvelu.hakemus.rest.url}") String address
	//
	) {
		this.address = address;
		JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
		bean.setAddress(address);
		bean.setThreadSafe(true);
		List<Object> providers = Lists.newArrayList();
		providers
				.add(new com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider());
		providers.add(new fi.vm.sade.valinta.kooste.ObjectMapperProvider());
		bean.setProviders(providers);
		List<Interceptor<? extends Message>> interceptors = Lists
				.newArrayList();

		CasApplicationAsAUserInterceptor cas = new CasApplicationAsAUserInterceptor();
		cas.setWebCasUrl(webCasUrl);
		cas.setTargetService(targetService);
		cas.setAppClientUsername(appClientUsername);
		cas.setAppClientPassword(appClientPassword);
		interceptors.add(cas);
		bean.setOutInterceptors(interceptors);
		this.webClient = bean.createWebClient();
		ClientConfiguration c = WebClient.getConfig(webClient);
		/**
		 * WARNING! 0 ei ehka tarkoita ikuista.
		 * http://cxf.547215.n5.nabble.com/Turn
		 * -off-all-timeouts-with-WebClient-in-JAX-RS-td3364696.html
		 */
		c.getHttpConduit().getClient()
				.setReceiveTimeout(TimeUnit.HOURS.toMillis(1));
		// org.apache.cxf.transport.http.async.SO_TIMEOUT
	}

	// public static final String CHARSET_UTF_8 = ";charset=UTF-8";
	@Override
	public Future<List<Hakemus>> getApplicationsByOid(String hakuOid,
			String hakukohdeOid) {
		String url = new StringBuilder().append("/applications/listfull")
				.toString();
		// new MediaType("application", "json", Charset.forName("UTF-8"));
		return WebClient.fromClient(webClient).path(url)
		//
				.query("appState", "ACTIVE", "INCOMPLETE")
				//
				.query("rows", 100000)
				//
				.query("asId", hakuOid)
				//
				.query("aoOid", hakukohdeOid)
				//
				// .accept("application/json;charset=UTF-8")
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async()
				//
				.get(new GenericType<List<Hakemus>>() {
				});
	}

	@Override
	public Future<List<Hakemus>> getApplicationsByOids(
			Collection<String> hakemusOids) {
		String url = new StringBuilder().append("/applications/list")
				.toString();
		// new MediaType("application", "json", Charset.forName("UTF-8"));
		return WebClient.fromClient(webClient)
		//
				.path(url)
				// .accept("application/json;charset=UTF-8")
				//
				.query("rows", 100000)
				//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async()
				//
				.post(Entity.entity(Lists.newArrayList(hakemusOids),
						MediaType.APPLICATION_JSON_TYPE),
						new GenericType<List<Hakemus>>() {
						});
	}

	public Peruutettava getApplicationsByOid(String hakuOid,
			String hakukohdeOid, Consumer<List<Hakemus>> callback,
			Consumer<Throwable> failureCallback) {
		String url = new StringBuilder().append("/applications/listfull")
				.toString();
		try {
			return new PeruutettavaImpl(
					WebClient
							.fromClient(webClient)
							.path(url)
							//
							.query("appState", "ACTIVE", "INCOMPLETE")
							//
							.query("rows", 100000)
							//
							.query("asId", hakuOid)
							//
							.query("aoOid", hakukohdeOid)
							//
							.async()
							//
							.get(new Callback<List<Hakemus>>(
									address,
									new StringBuilder()
											.append(url)
											.append("?appStates=ACTIVE&appStates=INCOMPLETE&rows=100000&aoOid=")
											.append(hakukohdeOid)
											.append("&asId=").append(hakuOid)
											.toString(), callback,
									failureCallback,
									new TypeToken<List<Hakemus>>() {
									}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

	public Peruutettava getApplicationAdditionalData(String hakuOid,
			String hakukohdeOid,
			Consumer<List<ApplicationAdditionalDataDTO>> callback,
			Consumer<Throwable> failureCallback) {
		String url = new StringBuilder()
				.append("/applications/additionalData/").append(hakuOid)
				.append("/").append(hakukohdeOid).toString();
		try {
			return new PeruutettavaImpl(
					WebClient
							.fromClient(webClient)
							.path(url)
							.async()
							.get(new Callback<List<ApplicationAdditionalDataDTO>>(
									address,
									url,
									callback,
									failureCallback,
									new TypeToken<List<ApplicationAdditionalDataDTO>>() {
									}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}
}
