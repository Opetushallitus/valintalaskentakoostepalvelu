package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.impl;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import fi.vm.sade.valinta.kooste.external.resource.AsyncResourceWithCas;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ViestintapalveluAsyncResourceImpl extends AsyncResourceWithCas implements ViestintapalveluAsyncResource {

	private final Gson GSON = new Gson();

	@Autowired
	public ViestintapalveluAsyncResourceImpl(
			@Value("${web.url.cas}") String webCasUrl,
			@Value("${cas.service.viestintapalvelu}/j_spring_cas_security_check") String targetService,
			@Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
			@Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword,
			@Value("${valintalaskentakoostepalvelu.viestintapalvelu.url}") String address,
			ApplicationContext context
	) {
		super(webCasUrl, targetService, appClientUsername, appClientPassword, address, context, TimeUnit.HOURS.toMillis(20));
	}

	@Override
	public Future<LetterBatchStatusDto> haeStatus(String letterBatchId) {
		String url = "/api/v1/letter/async/letter/status/"+letterBatchId;
		return WebClient.fromClient(webClient).path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE).async()
				.get(LetterBatchStatusDto.class);
	}

	public Future<LetterResponse> viePdfJaOdotaReferenssi(LetterBatch letterBatch) {
		String url = "/api/v1/letter/async/letter";
		return WebClient.fromClient(webClient)
				.path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async()
				.post(Entity.entity(GSON.toJson(letterBatch), MediaType.APPLICATION_JSON_TYPE), LetterResponse.class);
	}
}
