package fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl;

import java.util.List;
import java.util.function.Consumer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
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

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ApplicationAsyncResourceImpl implements ApplicationAsyncResource {
	private final WebClient webClient;
	private final String address;

	@Autowired
	public ApplicationAsyncResourceImpl(
			//
			@Value("${web.url.cas}") String webCasUrl,
			//
			@Value("${cas.service.haku-service}/j_spring_cas_security_check") String targetService,
			//
			@Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String appClientUsername,
			//
			@Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String appClientPassword,
			@Value("${valintalaskentakoostepalvelu.hakemus.rest.url}") String address
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

	public void getApplicationsByOid(String hakukohdeOid,
			Consumer<List<Hakemus>> callback,
			Consumer<Throwable> failureCallback) {
		// @GET
		// @Path("listfull")
		// @Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
		// @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
		// public List<Hakemus> getApplicationsByOid(
		// @QueryParam("aoOid") String aoOid,
		// @QueryParam("appState") List<String> appStates,
		// @QueryParam("rows") int rows);
		String url = new StringBuilder()
				.append("/applications/listfull?appStates=ACTIVE&appStates=INCOMPLETE&rows=100000&aoOid=")
				.append(hakukohdeOid).toString();
		try {
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.get(new Callback<List<Hakemus>>(address, url, callback,
							failureCallback));
		} catch (Exception e) {
			failureCallback.accept(e);
		}
	}

	public void getApplicationAdditionalData(String hakuOid,
			String hakukohdeOid,
			Consumer<List<ApplicationAdditionalDataDTO>> callback,
			Consumer<Throwable> failureCallback) {
		String url = new StringBuilder()
				.append("/applications/additionalData/").append(hakuOid)
				.append("/").append(hakukohdeOid).toString();
		try {
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.get(new Callback<List<ApplicationAdditionalDataDTO>>(
							address, url, callback, failureCallback));
		} catch (Exception e) {
			failureCallback.accept(e);
		}
	}
}
