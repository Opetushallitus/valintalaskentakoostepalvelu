package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.ws.rs.core.GenericType;

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

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ValintaperusteetAsyncResourceImpl implements
		ValintaperusteetAsyncResource {
	private final WebClient webClient;
	private final String address;
	private final static Logger LOG = LoggerFactory
			.getLogger(ValintaperusteetAsyncResourceImpl.class);

	@Autowired
	public ValintaperusteetAsyncResourceImpl(
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

		// CasApplicationAsAUserInterceptor cas = new
		// CasApplicationAsAUserInterceptor();
		// cas.setWebCasUrl(webCasUrl);
		// cas.setTargetService(targetService);
		// cas.setAppClientUsername(appClientUsername);
		// cas.setAppClientPassword(appClientPassword);
		LOG.warn("Valintaperusteiden palvelukutsusta on disabloitu CAS-filtteri!");
		// interceptors.add(cas);
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
				.append(hakukohdeOid).toString();
		return WebClient.fromClient(webClient).path(url).async()
				.get(new GenericType<List<ValintaperusteDTO>>() {
				});
	}

	// /valintaperusteet/hakijaryhmä/{hakukohdeoid}
	public Peruutettava haeHakijaryhmat(String hakukohdeOid,
			Consumer<List<ValintaperusteetHakijaryhmaDTO>> callback,
			Consumer<Throwable> failureCallback) {
		try {

			StringBuilder urlBuilder = new StringBuilder()
					.append("/valintaperusteet-service/resources/valintaperusteet/hakijaryhma/")
					.append(hakukohdeOid);
			String url = urlBuilder.toString();
			return new PeruutettavaImpl(
					WebClient
							.fromClient(webClient)
							.path(url)
							.async()
							.get(new Callback<List<ValintaperusteetHakijaryhmaDTO>>(
									address,
									url,
									callback,
									failureCallback,
									new TypeToken<List<ValintaperusteetHakijaryhmaDTO>>() {
									}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

	public Peruutettava haeValintaperusteet(String hakukohdeOid,
			Integer valinnanVaiheJarjestysluku,
			Consumer<List<ValintaperusteetDTO>> callback,
			Consumer<Throwable> failureCallback) {
		try {
			String url = new StringBuilder()
					.append("/valintaperusteet-service/resources/valintaperusteet/")
					.append(hakukohdeOid).toString();

			WebClient wc = WebClient.fromClient(webClient).path(url);
			if (valinnanVaiheJarjestysluku != null) {
				wc.query("vaihe", valinnanVaiheJarjestysluku);
			}
			return new PeruutettavaImpl(wc.async().get(
					new Callback<List<ValintaperusteetDTO>>(address, url
							+ "?vaihe=" + valinnanVaiheJarjestysluku, callback,
							failureCallback,
							new TypeToken<List<ValintaperusteetDTO>>() {
							}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

	public Peruutettava haunHakukohteet(String hakuOid,
			Consumer<List<HakukohdeViiteDTO>> callback,
			Consumer<Throwable> failureCallback) {
		try {
			String url = new StringBuilder()
					.append("/valintaperusteet-service/resources/hakukohde/haku/")
					.append(hakuOid).toString();
			return new PeruutettavaImpl(WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.get(new Callback<List<HakukohdeViiteDTO>>(address, url,
							callback, failureCallback,
							new TypeToken<List<HakukohdeViiteDTO>>() {
							}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}
}
