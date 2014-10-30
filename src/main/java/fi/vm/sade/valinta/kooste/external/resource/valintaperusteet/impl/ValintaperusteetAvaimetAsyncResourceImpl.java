package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl;

import java.util.List;
import java.util.concurrent.Future;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.collect.Lists;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAvaimetAsyncResource;

public class ValintaperusteetAvaimetAsyncResourceImpl implements ValintaperusteetAvaimetAsyncResource {

	private final WebClient webClient;
	private final String address;
	private final static Logger LOG = LoggerFactory
			.getLogger(ValintaperusteetAsyncResourceImpl.class);

	@Autowired
	public ValintaperusteetAvaimetAsyncResourceImpl(
			//
			@Value("${web.url.cas}") String webCasUrl,
			//
			@Value("${cas.service.valintaperusteet-service}/j_spring_cas_security_check") String targetService,
			//
			@Value("${valintalaskentakoostepalvelu.app.username.to.valintaperusteet}") String appClientUsername,
			//
			@Value("${valintalaskentakoostepalvelu.app.password.to.valintaperusteet}") String appClientPassword,
			@Value("https://${host.virkailija}") String address
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

		CasApplicationAsAUserInterceptor cas = new
		CasApplicationAsAUserInterceptor();
		cas.setWebCasUrl(webCasUrl);
		cas.setTargetService(targetService);
		cas.setAppClientUsername(appClientUsername);
		cas.setAppClientPassword(appClientPassword);
		interceptors.add(cas);
		// bean.setOutInterceptors(interceptors);
		this.webClient = bean.createWebClient();
	}


	// @GET
	// @Path("/avaimet/{oid}")
	// @Produces(MediaType.APPLICATION_JSON)
	// List<ValintaperusteDTO> findAvaimet(@PathParam("oid") String oid);
	@Override
	public Future<List<ValintaperusteDTO>> findAvaimet(String hakukohdeOid) {
		String url = new StringBuilder()
				.append("/valintaperusteet-service/resources/hakukohde/avaimet/")
				.append(hakukohdeOid).append("/").toString();
		return WebClient.fromClient(webClient).path(url)
		//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async().get(new GenericType<List<ValintaperusteDTO>>() {
				});
	}
}
