package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.external.resource.AsennaCasFilter;
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
			@Value("${web.url.cas}") String webCasUrl,
			@Value("${cas.service.sijoittelu-service}/j_spring_cas_security_check") String targetService,
			@Value("${valintalaskentakoostepalvelu.app.username.to.sijoittelu}") String appClientUsername,
			@Value("${valintalaskentakoostepalvelu.app.password.to.sijoittelu}") String appClientPassword,
			@Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}") String address,
			ApplicationContext context
	) {
		this.address = address;
		JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
		bean.setAddress(address);
		bean.setThreadSafe(true);
		List<Object> providers = Lists.newArrayList();
		providers.add(new com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider());
		providers.add(new fi.vm.sade.valinta.kooste.ObjectMapperProvider());
		bean.setProviders(providers);
		AsennaCasFilter.asennaCasFilter(webCasUrl, targetService, appClientUsername, appClientPassword, bean, context);
		this.webClient = bean.createWebClient();
		ClientConfiguration c = WebClient.getConfig(webClient);
		c.getHttpConduit().getClient().setReceiveTimeout(TimeUnit.MINUTES.toMillis(50));
	}

	@Override
	public Future<HakijaPaginationObject> getHakijatIlmanKoulutuspaikkaa(String hakuOid) {
		String url = "/sijoittelu/"+hakuOid+"/sijoitteluajo/"+SijoitteluResource.LATEST+"/hakemukset";
		LOG.info("Asynkroninen kutsu: {}{}?ilmanHyvaksyntaa=true", address, url);
		return WebClient.fromClient(webClient)
				.path(url)
				.query("ilmanHyvaksyntaa", true)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async().get(new GenericType<HakijaPaginationObject>() { });
	}

	public Future<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid, String valintatapajonoOid) {
		String url = "/tila/hakukohde/"+hakukohdeOid+"/"+valintatapajonoOid;
		return WebClient.fromClient(webClient)
				.path(url)
				.accept(MediaType.WILDCARD)
				.async().get(new GenericType<List<Valintatulos>>() { });
	}

	public Future<HakukohdeDTO> getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid) {
		String url = "/sijoittelu/"+hakuOid+"/sijoitteluajo/"+SijoitteluResource.LATEST+"/hakukohde/"+hakukohdeOid;
		return WebClient.fromClient(webClient)
				.path(url)
				.accept(MediaType.WILDCARD)
				.async().get(new GenericType<HakukohdeDTO>() { });
	}

	@Override
	public Future<HakijaPaginationObject> getKaikkiHakijat(String hakuOid, String hakukohdeOid) {
		String url = "/sijoittelu/"+hakuOid+"/sijoitteluajo/"+SijoitteluResource.LATEST+"/hakemukset";
		LOG.info("Asynkroninen kutsu: {}{}?hyvaksytyt=true&hakukohdeOid={}", address, url, hakukohdeOid);
		return WebClient.fromClient(webClient)
				.path(url)
				.query("hakukohdeOid", hakukohdeOid)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async().get(new GenericType<HakijaPaginationObject>() { });
	}

	@Override
	public Future<HakijaPaginationObject> getKoulutuspaikkallisetHakijat(String hakuOid, String hakukohdeOid) {
		String url = "/sijoittelu/"+hakuOid+"/sijoitteluajo/"+SijoitteluResource.LATEST+"/hakemukset";
		LOG.info("Asynkroninen kutsu: {}{}?hyvaksytyt=true&hakukohdeOid={}", address, url, hakukohdeOid);
		return WebClient.fromClient(webClient)
				.path(url)
				.query("hyvaksytyt", true)
				.query("hakukohdeOid", hakukohdeOid)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async().get(new GenericType<HakijaPaginationObject>() { });
	}

}
