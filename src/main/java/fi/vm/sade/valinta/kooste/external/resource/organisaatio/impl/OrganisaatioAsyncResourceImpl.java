package fi.vm.sade.valinta.kooste.external.resource.organisaatio.impl;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.valinta.kooste.external.resource.AsennaCasFilter;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         https://${host.virkailija}/organisaatio-service/rest esim
 *         /organisaatio-service/rest/organisaatio/1.2.246.562.10.39218317368
 *         ?noCache=1413976497594
 */
@Service
public class OrganisaatioAsyncResourceImpl implements OrganisaatioAsyncResource {

	private final WebClient webClient;
	private final String address;

	@Autowired
	public OrganisaatioAsyncResourceImpl(
			//
			@Value("${web.url.cas}") String webCasUrl,
			//
			@Value("${cas.service.organisaatio-service}/j_spring_cas_security_check") String targetService,
			//
			@Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
			//
			@Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword,
			@Value("${valintalaskentakoostepalvelu.organisaatioService.rest.url}") String address
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
		AsennaCasFilter.asennaCasFilter(
				webCasUrl,
				targetService,
				appClientUsername,
				appClientPassword,
				bean);
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

	@Override
	public Future<Response> haeOrganisaatio(String organisaatioOid) {
		// /organisaatio/1.2.246.562.10.39218317368
		String url = new StringBuilder().append("/organisaatio/")
				.append(organisaatioOid).append("/").toString();
		// new MediaType("application", "json", Charset.forName("UTF-8"));
		return WebClient.fromClient(webClient).path(url)
		//
				.accept(MediaType.WILDCARD)
				//
				.async()
				//
				.get();//new GenericType<OrganisaatioRDTO>() {}
	}
}
