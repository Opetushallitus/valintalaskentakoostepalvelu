package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;

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
}
