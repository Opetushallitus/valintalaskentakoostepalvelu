package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ws.rs.client.Entity;
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
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.impl.ViestintapalveluAsyncResourceImpl;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ValintaperusteetValintakoeAsyncResourceImpl implements
		ValintaperusteetValintakoeAsyncResource {
	private final static Logger LOG = LoggerFactory
			.getLogger(ViestintapalveluAsyncResourceImpl.class);
	private final WebClient webClient;

	@Autowired
	public ValintaperusteetValintakoeAsyncResourceImpl(
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
	}

	@Override
	public Future<List<ValintakoeDTO>> haeValintakokeet(Collection<String> oids) {
		StringBuilder urlBuilder = new StringBuilder()
				.append("/valintaperusteet-service/resources/valintakoe/");
		String url = urlBuilder.toString();
		return WebClient
				.fromClient(webClient)
				.path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async()
				.post(Entity.entity(oids, MediaType.APPLICATION_JSON_TYPE),
						new GenericType<List<ValintakoeDTO>>() {
						});
	}

	@Override
	public Future<Map<String, List<ValintakoeDTO>>> haeValintakokeetHakukohteille(
			Collection<String> hakukohdeOids) {
		StringBuilder urlBuilder = new StringBuilder()
				.append("/valintaperusteet-service/resources/hakukohde/valintakoe");
		String url = urlBuilder.toString();
		LOG.error("POST {}\r\n{}", url,
				Arrays.toString(hakukohdeOids.toArray()));
		return WebClient
				.fromClient(webClient)
				.path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async()
				.post(Entity.entity(hakukohdeOids,
						MediaType.APPLICATION_JSON_TYPE),
						new GenericType<Map<String, List<ValintakoeDTO>>>() {
						});
	}
}
