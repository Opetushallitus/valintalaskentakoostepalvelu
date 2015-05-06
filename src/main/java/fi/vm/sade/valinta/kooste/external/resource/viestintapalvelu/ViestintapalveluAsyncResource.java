package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu;

import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ViestintapalveluAsyncResource {

	int VIESTINTAPALVELUN_MAKSIMI_POLLAUS_SEKUNTIA = (int) TimeUnit.MINUTES
			.toMillis(60L);

	Future<LetterResponse> viePdfJaOdotaReferenssi(LetterBatch letterBatch);

	Future<LetterBatchStatusDto> haeStatus(String batchId);

	Peruutettava haeOsoitetarrat(Osoitteet osoitteet, Consumer<Response> callback, Consumer<Throwable> failureCallback);
}
