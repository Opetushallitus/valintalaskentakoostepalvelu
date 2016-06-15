package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto.LetterBatchCountDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;
import rx.Observable;

import javax.ws.rs.core.Response;

public interface ViestintapalveluAsyncResource {

    int VIESTINTAPALVELUN_MAKSIMI_POLLAUS_SEKUNTIA = (int) TimeUnit.MINUTES.toMillis(60L);

    @Deprecated
        // ks viePdfJaOdotaReferenssiObservable
    Future<LetterResponse> viePdfJaOdotaReferenssi(LetterBatch letterBatch);

    @Deprecated
        // ks haeStatusObservable
    Future<LetterBatchStatusDto> haeStatus(String batchId);

    Observable<LetterResponse> viePdfJaOdotaReferenssiObservable(LetterBatch letterBatch);

    Observable<LetterBatchStatusDto> haeStatusObservable(String batchId);

    Peruutettava haeOsoitetarrat(Osoitteet osoitteet, Consumer<Response> callback, Consumer<Throwable> failureCallback);

    Observable<List<TemplateHistory>> haeKirjepohja(String hakuOid, String tarjoajaOid, String templateName, String languageCode, String hakukohdeOid);

    Observable<LetterBatchCountDto> haeTuloskirjeenMuodostuksenTilanne(String hakuOid, String tyyppi, String kieli);

    Observable<Optional<Long>> haeJulkaistuKirjelahetys(String hakuOid, String kirjeenTyyppi, String kieli);

    Observable<List<String>> haeEPostiOsoitteet(Long batchId);
}
