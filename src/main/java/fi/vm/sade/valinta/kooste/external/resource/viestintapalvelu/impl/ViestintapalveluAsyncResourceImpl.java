package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.impl;

import java.util.List;
import java.util.concurrent.Future;

import javax.ws.rs.client.Entity;
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
import com.google.gson.Gson;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ViestintapalveluAsyncResourceImpl implements
		ViestintapalveluAsyncResource {

	private final static Logger LOG = LoggerFactory
			.getLogger(ViestintapalveluAsyncResourceImpl.class);
	private final WebClient webClient;
	private final Gson GSON = new Gson();

	@Autowired
	public ViestintapalveluAsyncResourceImpl(
			//
			@Value("${web.url.cas}") String webCasUrl,
			//
			@Value("${cas.service.viestintapalvelu}/j_spring_cas_security_check") String targetService,
			//
			@Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
			//
			@Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword,
			@Value("${valintalaskentakoostepalvelu.viestintapalvelu.url}") String address
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
	public Future<LetterBatchStatusDto> haeStatus(String letterBatchId) {
		StringBuilder urlBuilder = new StringBuilder().append(
				"/api/v1/letter/async/letter/status/").append(letterBatchId);
		String url = urlBuilder.toString();
		return WebClient.fromClient(webClient).path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE).async()
				.get(LetterBatchStatusDto.class);
	}

	public Future<String> viePdfJaOdotaReferenssi(LetterBatch letterBatch) {
		StringBuilder urlBuilder = new StringBuilder()
				.append("/api/v1/letter/async/letter");
		String url = urlBuilder.toString();
		return WebClient.fromClient(webClient)
				.path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				// MediaType.TEXT_PLAIN_TYPE)
				.async()
				.post(Entity.entity(GSON.toJson(letterBatch),
						MediaType.APPLICATION_JSON_TYPE), String.class);
	}
}
