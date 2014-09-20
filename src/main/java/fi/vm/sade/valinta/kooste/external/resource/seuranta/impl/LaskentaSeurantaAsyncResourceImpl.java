package fi.vm.sade.valinta.kooste.external.resource.seuranta.impl;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

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
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.ResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import fi.vm.sade.valinta.seuranta.dto.YhteenvetoDto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class LaskentaSeurantaAsyncResourceImpl implements
		LaskentaSeurantaAsyncResource {
	private static final Logger LOG = LoggerFactory
			.getLogger(LaskentaSeurantaAsyncResourceImpl.class);
	private final WebClient webClient;
	private final Gson gson = new Gson();
	private final ResponseCallback responseCallback = new ResponseCallback();
	private final String address;

	@Autowired
	public LaskentaSeurantaAsyncResourceImpl(
			//
			@Value("${web.url.cas}") String webCasUrl,
			//
			@Value("${cas.service.seuranta}/j_spring_cas_security_check") String targetService,
			//
			@Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
			//
			@Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword,
			@Value("${valintalaskentakoostepalvelu.seuranta.rest.url}") String address
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

	public void haeAsync(String hakuOid,
			Consumer<Collection<YhteenvetoDto>> callback) {
		String url = "/seuranta/hae/" + hakuOid;
		try {
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.get(new Callback<Collection<YhteenvetoDto>>(address, url,
							callback,
							new TypeToken<Collection<YhteenvetoDto>>() {
							}.getType()));
		} catch (Exception e) {
			LOG.error("Seurantapalvelun kutsu {} paatyi virheeseen: {}", url,
					e.getMessage());
		}
	}

	public void laskenta(String uuid, Consumer<LaskentaDto> callback,
			Consumer<Throwable> failureCallback) {
		try {
			String url = new StringBuilder().append("/seuranta/laskenta/")
					.append(uuid).toString();
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.get(new Callback<LaskentaDto>(address, url, callback,
							failureCallback, new TypeToken<LaskentaDto>() {
							}.getType()));
		} catch (Exception e) {
			failureCallback.accept(e);
		}
	}

	public void resetoiTilat(String uuid, Consumer<LaskentaDto> callback,
			Consumer<Throwable> failureCallback) {
		try {
			String url = new StringBuilder().append("/seuranta/laskenta/")
					.append(uuid).append("/resetoi").toString();
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.put(Entity.entity(uuid, MediaType.APPLICATION_JSON_TYPE),
							new Callback<LaskentaDto>(address, url, callback,
									failureCallback,
									new TypeToken<LaskentaDto>() {
									}.getType()));
		} catch (Exception e) {
			failureCallback.accept(e);
		}
	}

	public void luoLaskenta(String hakuOid, LaskentaTyyppi tyyppi,
			Integer valinnanvaihe, Boolean valintakoelaskenta,
			List<String> hakukohdeOids, Consumer<String> callback,
			Consumer<Throwable> failureCallback) {
		try {
			if (valintakoelaskenta != null && valinnanvaihe != null) {
				String url = new StringBuilder().append("/seuranta/laskenta/")
						.append(hakuOid).append("/tyyppi/").append(tyyppi)
						.append("/valinnanvaihe/").append(valinnanvaihe)
						.append("/valintakoelaskenta/")
						.append(valintakoelaskenta).toString();
				WebClient
						.fromClient(webClient)
						.path(url)
						.async()
						.post(Entity.entity(gson.toJson(hakukohdeOids),
								MediaType.APPLICATION_JSON_TYPE),
								new Callback<String>(address, url, callback,
										failureCallback,
										new TypeToken<String>() {
										}.getType()));
			} else {
				String url = new StringBuilder().append("/seuranta/laskenta/")
						.append(hakuOid).append("/tyyppi/").append(tyyppi)
						.toString();
				WebClient
						.fromClient(webClient)
						.path(url)
						.async()
						.post(Entity.entity(gson.toJson(hakukohdeOids),
								MediaType.APPLICATION_JSON_TYPE),
								new Callback<String>(address, url, callback,
										failureCallback,
										new TypeToken<String>() {
										}.getType()));
			}
		} catch (Exception e) {
			failureCallback.accept(e);
		}
	}

	public void merkkaaLaskennanTila(String uuid, LaskentaTila tila) {
		String url = new StringBuilder().append("/seuranta/laskenta/")
				.append(uuid).append("/tila/").append(tila).toString();
		try {
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.put(Entity.entity(tila, MediaType.APPLICATION_JSON_TYPE),
							responseCallback);
		} catch (Exception e) {
			LOG.error("Seurantapalvelun kutsu {} paatyi virheeseen: {}", url,
					e.getMessage());
		}
	}

	@Override
	public void merkkaaHakukohteenTila(String uuid, String hakukohdeOid,
			HakukohdeTila tila) {
		String url = new StringBuilder().append("/seuranta/laskenta/")
				.append(uuid).append("/hakukohde/").append(hakukohdeOid)
				.append("/tila/").append(tila).toString();
		try {
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.put(Entity.entity(tila, MediaType.APPLICATION_JSON_TYPE),
							responseCallback);
		} catch (Exception e) {
			LOG.error("Seurantapalvelun kutsu {} paatyi virheeseen: {}", url,
					e.getMessage());
		}
	}

	public void lisaaIlmoitusHakukohteelle(String uuid, String hakukohdeOid,
			IlmoitusDto ilmoitus) {
		String url = new StringBuilder().append("/seuranta/laskenta/")
				.append(uuid).append("/hakukohde/").append(hakukohdeOid)
				.toString();
		try {
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.post(Entity.entity(gson.toJson(ilmoitus),
							MediaType.APPLICATION_JSON_TYPE), responseCallback);
		} catch (Exception e) {
			LOG.error("Seurantapalvelun kutsu {} paatyi virheeseen: {}", url,
					e.getMessage());
		}
	}

}
