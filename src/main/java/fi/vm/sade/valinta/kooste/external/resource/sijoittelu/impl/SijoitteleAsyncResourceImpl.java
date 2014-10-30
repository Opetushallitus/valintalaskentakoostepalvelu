package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.camel.Property;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;

/**
 * 
 * @author jussija
 *
 */
@Service
public class SijoitteleAsyncResourceImpl implements SijoitteleAsyncResource {
	private static final Logger LOG = LoggerFactory
			.getLogger(SijoitteluAsyncResourceImpl.class);
	private final WebClient webClient;
	private final String address;

	@Autowired
	public SijoitteleAsyncResourceImpl(
			//
			@Value("${web.url.cas}") String webCasUrl,
			// ${cas.service.suoritusrekisteri}
			@Value("${cas.service.sijoittelu-service}/j_spring_cas_security_check") String targetService,
			// ${valintalaskentakoostepalvelu.app.username.to.suoritusrekisteri}
			@Value("${valintalaskentakoostepalvelu.app.username.to.sijoittelu}") String appClientUsername,
			// ${valintalaskentakoostepalvelu.app.password.to.suoritusrekisteri}
			@Value("${valintalaskentakoostepalvelu.app.password.to.sijoittelu}") String appClientPassword,
			@Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}") String address
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
				.setReceiveTimeout(TimeUnit.MINUTES.toMillis(50));
		// org.apache.cxf.transport.http.async.SO_TIMEOUT
	}

	@Override
	public void sijoittele(String hakuOid, Consumer<String> callback,
			Consumer<Throwable> failureCallback) {
		// fi.vm.sade.valinta.kooste.sijoittelu.resource.SijoitteluResource
		String url = new StringBuilder().append("/sijoittele/").append(hakuOid).append("/")
				.toString();
		try {

			WebClient
					.fromClient(webClient)
					.path(url)
					.accept(MediaType.WILDCARD_TYPE)
					.async()
					.get(new Callback<String>(address, url, callback,
							failureCallback, new TypeToken<String>() {
							}.getType()));
		} catch (Exception e) {
			failureCallback.accept(e);
		}
	}

}
