package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.impl;

import java.util.List;
import java.util.function.Consumer;

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
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         ApplicationResource
 */
@Service
public class SuoritusrekisteriAsyncResourceImpl implements
		SuoritusrekisteriAsyncResource {
	private final WebClient webClient;
	private final String address;

	@Autowired
	public SuoritusrekisteriAsyncResourceImpl(
			//
			@Value("${web.url.cas}") String webCasUrl,
			// ${cas.service.suoritusrekisteri}
			@Value("https://${host.virkailija}/suoritusrekisteri/j_spring_cas_security_check") String targetService,
			// ${valintalaskentakoostepalvelu.app.username.to.suoritusrekisteri}
			@Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
			// ${valintalaskentakoostepalvelu.app.password.to.suoritusrekisteri}
			@Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword,
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

	public Peruutettava getOppijatByHaku(String hakuOid,
			Consumer<List<Oppija>> callback, Consumer<Throwable> failureCallback) {
		String url = new StringBuilder().append(
				"/suoritusrekisteri/rest/v1/oppijat").toString();
		// suoritusrekisteri/rest/v1/oppijat?haku=1.2.246.562.29.173465377510
		try {
			return new PeruutettavaImpl(WebClient
					.fromClient(webClient)
					.path(url)
					//
					.query("haku", hakuOid)

					.async()
					//
					.get(new Callback<List<Oppija>>(address,
							new StringBuilder().append(url).append("?haku=")
									.append(hakuOid).toString(), callback,
							failureCallback, new TypeToken<List<Oppija>>() {
							}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

	public Peruutettava getOppijatByHakukohde(String hakukohdeOid,
			Consumer<List<Oppija>> callback, Consumer<Throwable> failureCallback) {
		String url = new StringBuilder().append(
				"/suoritusrekisteri/rest/v1/oppijat").toString();
		try {
			return new PeruutettavaImpl(WebClient
					.fromClient(webClient)
					.path(url)
					//
					.query("hakukohde", hakukohdeOid)

					.async()
					//
					.get(new Callback<List<Oppija>>(address,
							new StringBuilder().append(url)
									.append("?hakukohde=").append(hakukohdeOid)
									.toString(), callback, failureCallback,
							new TypeToken<List<Oppija>>() {
							}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

	public Peruutettava getOppijatByOrganisaatio(String organisaatioOid,
			Consumer<List<Oppija>> callback, Consumer<Throwable> failureCallback) {
		String url = new StringBuilder().append(
				"/suoritusrekisteri/rest/v1/oppijat").toString();
		try {
			return new PeruutettavaImpl(WebClient
					.fromClient(webClient)
					.path(url)
					//
					.query("organisaatio", organisaatioOid)

					.async()
					//
					.get(new Callback<List<Oppija>>(address,
							new StringBuilder().append(url)
									.append("?organisaatio=")
									.append(organisaatioOid).toString(),
							callback, failureCallback,
							new TypeToken<List<Oppija>>() {
							}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}
}
