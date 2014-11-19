package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.external.resource.AsennaCasFilter;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ValintalaskentaValintakoeAsyncResourceImpl implements
		ValintalaskentaValintakoeAsyncResource {

	private final WebClient webClient;

	@Autowired
	public ValintalaskentaValintakoeAsyncResourceImpl(
			//
			@Value("${web.url.cas}") String webCasUrl,
			//
			@Value("${cas.service.valintalaskenta-service}/j_spring_cas_security_check") String targetService,
			//
			@Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
			//
			@Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword,
			@Value("${valintalaskentakoostepalvelu.valintalaskenta.rest.url}") String address,ApplicationContext context
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
		AsennaCasFilter.asennaCasFilter(
				webCasUrl,
				targetService,
				appClientUsername,
				appClientPassword,
				bean,context);
		this.webClient = bean.createWebClient();
	}

	@Override
	public Future<List<ValintakoeOsallistuminenDTO>> haeOsallistumiset(
			Collection<String> hakemusOids) {
		StringBuilder urlBuilder = new StringBuilder()
				.append("/valintakoe/hakemus/");
		String url = urlBuilder.toString();
		return WebClient
				.fromClient(webClient)
				//
				.path(url)
				//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async()
				.post(Entity.entity(hakemusOids,
						MediaType.APPLICATION_JSON_TYPE),
						new GenericType<List<ValintakoeOsallistuminenDTO>>() {
						});
	}

	@Override
	public Future<List<ValintakoeOsallistuminenDTO>> haeHakutoiveelle(
			String hakukohdeOid) {
		// valintalaskentakoostepalvelu.valintalaskenta.rest.url
		// /valintalaskenta-laskenta-service/resources/valintakoe/hakutoive/...
		StringBuilder urlBuilder = new StringBuilder().append(
				"/valintakoe/hakutoive/").append(hakukohdeOid);
		String url = urlBuilder.toString();
		return WebClient.fromClient(webClient)
				//
				.path(url)
				//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async()
				.get(new GenericType<List<ValintakoeOsallistuminenDTO>>() {
				});
	}
}
