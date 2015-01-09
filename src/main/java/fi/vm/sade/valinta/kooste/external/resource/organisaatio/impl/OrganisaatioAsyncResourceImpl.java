package fi.vm.sade.valinta.kooste.external.resource.organisaatio.impl;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.AsyncResourceWithCas;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         https://${host.virkailija}/organisaatio-service/rest esim
 *         /organisaatio-service/rest/organisaatio/1.2.246.562.10.39218317368
 *         ?noCache=1413976497594
 */
@Service
public class OrganisaatioAsyncResourceImpl extends AsyncResourceWithCas implements OrganisaatioAsyncResource {

	@Autowired
	public OrganisaatioAsyncResourceImpl(
			@Value("${web.url.cas}") String webCasUrl,
			@Value("${cas.service.organisaatio-service}/j_spring_cas_security_check") String targetService,
			@Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
			@Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword,
			@Value("${valintalaskentakoostepalvelu.organisaatioService.rest.url}") String address,
			ApplicationContext context
	) {
		super(webCasUrl, targetService, appClientUsername, appClientPassword, address, context, TimeUnit.HOURS.toMillis(1));
	}

	@Override
	public Future<Response> haeOrganisaatio(String organisaatioOid) {
		String url = "/organisaatio/"+organisaatioOid+"/";

		return WebClient.fromClient(webClient).path(url)
				.accept(MediaType.WILDCARD)
				.async()
				.get();
	}

	@Override
	public Future<String> haeOrganisaationOidKetju(String organisaatioOid) {
		String url = "/organisaatio/"+organisaatioOid+"/parentoids";

		return WebClient.fromClient(webClient).path(url)
				.accept(MediaType.WILDCARD)
				.async()
				.get(String.class);
	}
}
