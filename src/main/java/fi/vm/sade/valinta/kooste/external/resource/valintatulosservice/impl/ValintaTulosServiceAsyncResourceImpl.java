package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.ResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ValintaTulosServiceAsyncResourceImpl implements ValintaTulosServiceAsyncResource {
	private final WebClient webClient;
	private final String address;
	private final static Logger LOG = LoggerFactory
			.getLogger(ValintaTulosServiceAsyncResourceImpl.class);

	@Autowired
	public ValintaTulosServiceAsyncResourceImpl(
			@Value("${host.ilb}") String address
	//
	) {
		this.address = address;
		JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
		bean.setAddress(this.address);
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
				.setReceiveTimeout(TimeUnit.HOURS.toMillis(1));
	}

	@Override
	public void getValintatulokset(String hakuOid, String hakukohdeOid,
								   Consumer<List<ValintaTulosServiceDto>> vts,
								   Consumer<Throwable> poikkeus) {
		StringBuilder urlBuilder = new StringBuilder().append(
				"/valinta-tulos-service/haku/").append(hakuOid).append("/hakukohde/").append(hakukohdeOid);
		String url = urlBuilder.toString();
		WebClient
				.fromClient(webClient)
				.path(url)
						//
				.accept(MediaType.APPLICATION_JSON_TYPE)
						//
				.async()
				.get(new Callback<List<ValintaTulosServiceDto>>(address, url, vts, poikkeus,
						new TypeToken<List<ValintaTulosServiceDto>>() { }.getType()));
	}

	@Override
	public Future<List<ValintaTulosServiceDto>> getValintatulokset(String hakuOid) {
		StringBuilder urlBuilder = new StringBuilder().append(
				"/valinta-tulos-service/haku/").append(hakuOid);
		String url = urlBuilder.toString();
		
		return WebClient
				.fromClient(webClient)
				.path(url)
				//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async()
				.get(new GenericType<List<ValintaTulosServiceDto>>() {
				});
	}
}
