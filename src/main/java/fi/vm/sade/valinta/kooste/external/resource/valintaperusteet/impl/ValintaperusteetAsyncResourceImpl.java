package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl;

import java.util.List;
import java.util.function.Consumer;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;

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

		CasApplicationAsAUserInterceptor cas = new CasApplicationAsAUserInterceptor();
		cas.setWebCasUrl(webCasUrl);
		cas.setTargetService(targetService);
		cas.setAppClientUsername(appClientUsername);
		cas.setAppClientPassword(appClientPassword);
		interceptors.add(cas);
		bean.setOutInterceptors(interceptors);
		this.webClient = bean.createWebClient();
	}

	public void haeValintaperusteet(String hakukohdeOid,
			Integer valinnanVaiheJarjestysluku,
			Consumer<List<ValintaperusteetDTO>> callback,
			Consumer<Throwable> failureCallback) {
		try {
			StringBuilder urlBuilder = new StringBuilder().append(
					"/valintaperusteet-service/resources/valintaperusteet/")
					.append(hakukohdeOid);
			if (valinnanVaiheJarjestysluku != null) {
				urlBuilder.append("?vaihe=").append(valinnanVaiheJarjestysluku);
			}
			String url = urlBuilder.toString();
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.get(new Callback<List<ValintaperusteetDTO>>(address, url,
							callback, failureCallback,
							new TypeToken<List<ValintaperusteetDTO>>() {
							}.getType()));
		} catch (Exception e) {
			failureCallback.accept(e);
		}
	}

	public void haunHakukohteet(String hakuOid,
			Consumer<List<HakukohdeViiteDTO>> callback,
			Consumer<Throwable> failureCallback) {
		try {
			String url = new StringBuilder()
					.append("/valintaperusteet-service/resources/hakukohde/haku/")
					.append(hakuOid).toString();
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.get(new Callback<List<HakukohdeViiteDTO>>(address, url,
							callback, failureCallback,
							new TypeToken<List<HakukohdeViiteDTO>>() {
							}.getType()));
		} catch (Exception e) {
			failureCallback.accept(e);
		}
	}
}
