package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ViestintapalveluAsyncResource {

	int VIESTINTAPALVELUN_MAKSIMI_POLLAUS_SEKUNTIA = (int) TimeUnit.MINUTES
			.toMillis(15L);

	Future<String> viePdfJaOdotaReferenssi(LetterBatch letterBatch);

	Future<LetterBatchStatusDto> haeStatus(String batchId);
}
