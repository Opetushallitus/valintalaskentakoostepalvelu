package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import java.util.List;
import java.util.concurrent.Future;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.camel.Property;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
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
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;

/**
 * 
 * @author jussija
 *
 *         fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource <br>
 *         sijoitteluResource.hakemukset( hakuOid, SijoitteluResource.LATEST,
 *         true, null, null, hakukohteet, null, null);
 */
@Service
public class SijoitteluAsyncResourceImpl implements SijoitteluAsyncResource {
	private static final Logger LOG = LoggerFactory
			.getLogger(SijoitteluAsyncResourceImpl.class);
	private final WebClient webClient;
	private final String address;

	@Autowired
	public SijoitteluAsyncResourceImpl(
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
	public Future<HakijaPaginationObject> getHakijatIlmanKoulutuspaikkaa(
			String hakuOid) {
		/*
		 * private SijoitteluResource sijoitteluResource;
		 * 
		 * @Autowired public SijoitteluIlmankoulutuspaikkaaKomponentti(
		 * SijoitteluResource sijoitteluResource) { this.sijoitteluResource =
		 * sijoitteluResource; }
		 * 
		 * public List<HakijaDTO> ilmankoulutuspaikkaa(
		 * 
		 * @Property("hakuOid") String hakuOid,
		 * 
		 * @Property("sijoitteluajoId") String sijoitteluajoId) {
		 * 
		 * final HakijaPaginationObject result = sijoitteluResource.hakemukset(
		 * hakuOid, SijoitteluResource.LATEST, null, true, null, null, null,
		 * null); return result.getResults(); }
		 */
		// https://${host.virkailija}/sijoittelu-service/resources/.../sijoitteluajo/latest/hakemukset?hyvaksytyt=true&hakukohdeOid=
		StringBuilder urlBuilder = new StringBuilder().append("/sijoittelu/")
				.append(hakuOid).append("/sijoitteluajo/")
				.append(SijoitteluResource.LATEST).append("/hakemukset");
		String url = urlBuilder.toString();
		LOG.warn("Asynkroninen kutsu: {}{}?ilmanHyvaksyntaa=true", address, url);
		return WebClient.fromClient(webClient)
		//
				.path(url)
				//
				.query("ilmanHyvaksyntaa", true)
				//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async().get(new GenericType<HakijaPaginationObject>() {
				});
	}

	@Override
	public Future<HakijaPaginationObject> getKoulutuspaikkallisetHakijat(
			String hakuOid, String hakukohdeOid) {
		// https://${host.virkailija}/sijoittelu-service/resources/.../sijoitteluajo/latest/hakemukset?hyvaksytyt=true&hakukohdeOid=
		StringBuilder urlBuilder = new StringBuilder().append("/sijoittelu/")
				.append(hakuOid).append("/sijoitteluajo/")
				.append(SijoitteluResource.LATEST).append("/hakemukset");
		String url = urlBuilder.toString();
		LOG.warn("Asynkroninen kutsu: {}{}?hyvaksytyt=true&hakukohdeOid={}",
				address, url, hakukohdeOid);
		return WebClient.fromClient(webClient)
		//
				.path(url)
				//
				.query("hyvaksytyt", true)
				//
				.query("hakukohdeOid", hakukohdeOid)
				//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async().get(new GenericType<HakijaPaginationObject>() {
				});
	}

}
