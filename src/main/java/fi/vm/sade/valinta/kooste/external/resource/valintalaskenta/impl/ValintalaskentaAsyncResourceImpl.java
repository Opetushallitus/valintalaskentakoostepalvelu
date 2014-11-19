package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.Entity;
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
import com.google.gson.Gson;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ValintalaskentaAsyncResourceImpl implements
		ValintalaskentaAsyncResource {
	private final WebClient webClient;
	private final Gson gson = new Gson();
	private final String address;

	@Autowired
	public ValintalaskentaAsyncResourceImpl(
			//
			@Value("${web.url.cas}") String webCasUrl,
			//
			@Value("${cas.service.valintalaskenta-service}/j_spring_cas_security_check") String targetService,
			//
			@Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
			//
			@Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword,
			@Value("${valintalaskentakoostepalvelu.valintalaskenta.rest.url}") String address
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
		this.webClient = bean.createWebClient();
		ClientConfiguration c = WebClient.getConfig(webClient);
		/**
		 * WARNING! 0 ei ehka tarkoita ikuista.
		 * http://cxf.547215.n5.nabble.com/Turn
		 * -off-all-timeouts-with-WebClient-in-JAX-RS-td3364696.html
		 */
		c.getHttpConduit().getClient()
				.setReceiveTimeout(TimeUnit.HOURS.toMillis(20));
		// org.apache.cxf.transport.http.async.SO_TIMEOUT

	}

	@Override
	public Peruutettava laske(LaskeDTO laskeDTO, Consumer<String> callback,
			Consumer<Throwable> failureCallback) {
		try {
			String url = new StringBuilder().append("/valintalaskenta/laske")
					.toString();
			return new PeruutettavaImpl(WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.post(Entity.entity(laskeDTO,
							MediaType.APPLICATION_JSON_TYPE),
							new Callback<String>(address, url, callback,
									failureCallback, new TypeToken<String>() {
									}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}

	}

	@Override
	public Peruutettava laskeKaikki(LaskeDTO laskeDTO,
			Consumer<String> callback, Consumer<Throwable> failureCallback) {
		try {
			String url = new StringBuilder().append(
					"/valintalaskenta/laskekaikki").toString();
			return new PeruutettavaImpl(WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.post(Entity.entity(laskeDTO,
							MediaType.APPLICATION_JSON_TYPE),
							new Callback<String>(address, url, callback,
									failureCallback, new TypeToken<String>() {
									}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

	@Override
	public Peruutettava laskeJaSijoittele(List<LaskeDTO> lista,
			Consumer<String> callback, Consumer<Throwable> failureCallback) {
		try {
			String url = new StringBuilder().append(
					"/valintalaskenta/laskejasijoittele").toString();
			return new PeruutettavaImpl(
					WebClient
							.fromClient(webClient)
							.path(url)
							.async()
							.post(Entity.entity(lista,
									MediaType.APPLICATION_JSON_TYPE),
									new Callback<String>(address, url,
											callback, failureCallback,
											new TypeToken<String>() {
											}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

	@Override
	public Peruutettava valintakokeet(LaskeDTO laskeDTO,
			Consumer<String> callback, Consumer<Throwable> failureCallback) {
		try {
			String url = new StringBuilder().append(
					"/valintalaskenta/valintakokeet").toString();
			return new PeruutettavaImpl(WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.post(Entity.entity(laskeDTO,
							MediaType.APPLICATION_JSON_TYPE),
							new Callback<String>(address, url, callback,
									failureCallback, new TypeToken<String>() {
									}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

}
