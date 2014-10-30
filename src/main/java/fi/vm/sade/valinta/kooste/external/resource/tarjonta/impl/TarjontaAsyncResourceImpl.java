package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class TarjontaAsyncResourceImpl implements TarjontaAsyncResource {
	private final WebClient webClient;
	private final String address;

	@Autowired
	public TarjontaAsyncResourceImpl(
			@Value("${valintalaskentakoostepalvelu.tarjonta.rest.url}") String address
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

		// CasApplicationAsAUserInterceptor cas = new
		// CasApplicationAsAUserInterceptor();
		// cas.setWebCasUrl(webCasUrl);
		// cas.setTargetService(targetService);
		// cas.setAppClientUsername(appClientUsername);
		// cas.setAppClientPassword(appClientPassword);
		// interceptors.add(cas);
		bean.setOutInterceptors(interceptors);
		this.webClient = bean.createWebClient();
		ClientConfiguration c = WebClient.getConfig(webClient);
		/**
		 * WARNING! 0 ei ehka tarkoita ikuista.
		 * http://cxf.547215.n5.nabble.com/Turn
		 * -off-all-timeouts-with-WebClient-in-JAX-RS-td3364696.html
		 */
		c.getHttpConduit().getClient()
				.setReceiveTimeout(TimeUnit.MINUTES.toMillis(5));
		// org.apache.cxf.transport.http.async.SO_TIMEOUT
	}

	@Override
	public Future<HakuV1RDTO> haeHaku(String hakuOid) {
		// TODO Auto-generated method stub
		/*
		 * @Path("/v1/haku") public interface HakuV1Resource {
		 * 
		 * @GET
		 * 
		 * @Path("/{oid}")
		 * 
		 * @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8") //
		 * @ApiOperation(value = "Palauttaa haun annetulla oid:lla", notes = //
		 * "Palauttaa haun annetulla oid:lla", response = HakuV1RDTO.class)
		 * public ResultV1RDTO<HakuV1RDTO> findByOid(@PathParam("oid") String
		 * oid);
		 */
		StringBuilder urlBuilder = new StringBuilder().append("/v1/haku/")
				.append(hakuOid).append("/");
		String url = urlBuilder.toString();
		return WebClient.fromClient(webClient)
		//
				.path(url)
				//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async().get(HakuV1RDTO.class);
	}

	@Override
	public Future<HakukohdeDTO> haeHakukohde(String hakukohdeOid) {
		// TODO Auto-generated method stub
		/*
		 * /hakukohde
		 */
		// @Path("tila")
		StringBuilder urlBuilder = new StringBuilder().append("/hakukohde/")
				.append(hakukohdeOid).append("/");
		String url = urlBuilder.toString();
		return WebClient.fromClient(webClient)
		//
				.path(url)
				//
				// .query("hyvaksytyt", true)
				//
				// .query("hakukohdeOid", hakukohdeOid)
				//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async().get(HakukohdeDTO.class);
	}
}
