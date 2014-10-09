package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu;

import java.util.concurrent.Future;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ViestintapalveluAsyncResource {

	Future<String> viePdfJaOdotaReferenssi(LetterBatch letterBatch);

	Future<LetterBatchStatusDto> haeStatus(String batchId);
}
