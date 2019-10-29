package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu;

import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto.LetterBatchCountDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;
import io.reactivex.Observable;

import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ViestintapalveluAsyncResource {

    Duration VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA = Duration.of(60, ChronoUnit.MINUTES);

    CompletableFuture<LetterResponse> vieLetterBatch(LetterBatch letterBatch);

    CompletableFuture<LetterBatchStatusDto> haeLetterBatchStatus(String batchId);

    Observable<Response> haeOsoitetarrat(Osoitteet osoitteet);

    CompletableFuture<List<TemplateHistory>> haeKirjepohja(String hakuOid, String tarjoajaOid, String templateName, String languageCode, String hakukohdeOid);

    Observable<LetterBatchCountDto> haeTuloskirjeenMuodostuksenTilanne(String hakuOid, String tyyppi, String kieli);

    Observable<Optional<Long>> haeKirjelahetysEPostille(String hakuOid, String kirjeenTyyppi, String kieli);

    Observable<Optional<Long>> haeKirjelahetysJulkaistavaksi(String hakuOid, String kirjeenTyyppi, String kieli);

    Observable<Map<String, String>> haeEPostiOsoitteet(Long batchId);

    Observable<Optional<Long>> julkaiseKirjelahetys(Long batchId);
}
