package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.interceptor.Interceptor;
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

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.external.resource.AsennaCasFilter;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
/**
 * 
 * @author jussija
 *
 *         fi.vm.sade.sijoittelu.laskenta.resource.TilaResource <br>
 */
@Service
public class TilaAsyncResourceImpl implements TilaAsyncResource{
	private static final Logger LOG = LoggerFactory
			.getLogger(TilaAsyncResourceImpl.class);
	private final WebClient webClient;
	private final String address;
	@Autowired
	public TilaAsyncResourceImpl(
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
				.setReceiveTimeout(TimeUnit.MINUTES.toMillis(50));
		// org.apache.cxf.transport.http.async.SO_TIMEOUT
	}
	//@Path("erillishaku/{hakuOid}/hakukohde/{hakukohdeOid}")
	@Override
	public Future<Response> tuoErillishaunTilat(
			String hakuOid, String hakukohdeOid,
			Collection<ErillishaunHakijaDTO> erillishaunHakijat) {
		//@Path("tila")
		StringBuilder urlBuilder = new StringBuilder().append("/tila/erillishaku/")
				.append(hakuOid).append("/hakukohde/")
				.append(hakukohdeOid).append("/");
		String url = urlBuilder.toString();
		LOG.warn("Asynkroninen kutsu: {}{}?hyvaksytyt=true&hakukohdeOid={}",
				address, url, hakukohdeOid);
		return WebClient.fromClient(webClient)
		//
				.path(url)
				//
				//.query("hyvaksytyt", true)
				//
				//.query("hakukohdeOid", hakukohdeOid)
				//
				//.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async().post(Entity.entity(erillishaunHakijat,
						MediaType.APPLICATION_JSON_TYPE));
	}
}
