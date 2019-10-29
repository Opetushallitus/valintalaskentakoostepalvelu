package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.impl;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto.LetterBatchCountDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;
import io.reactivex.Observable;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;

@Service
public class ViestintapalveluAsyncResourceImpl extends UrlConfiguredResource implements ViestintapalveluAsyncResource {
    private final HttpClient client;

    @Autowired
    public ViestintapalveluAsyncResourceImpl(
            @Qualifier("viestintapalveluClientCasInterceptor") AbstractPhaseInterceptor casInterceptor,
            @Qualifier("ViestintapalveluHttpClient") HttpClient client
    ) {
        super(TimeUnit.HOURS.toMillis(20), casInterceptor);
        this.client = client;
    }

    @Override
    public CompletableFuture<LetterResponse> vieLetterBatch(LetterBatch letterBatch) {
        return this.client.postJson(
                getUrl("viestintapalvelu.letter.async.letter"),
                Duration.ofHours(20),
                letterBatch,
                new com.google.gson.reflect.TypeToken<LetterBatch>() {}.getType(),
                new com.google.gson.reflect.TypeToken<LetterResponse>() {}.getType()
        );
    }

    @Override
    public CompletableFuture<LetterBatchStatusDto> haeLetterBatchStatus(String letterBatchId) {
        return this.client.getJson(
                getUrl("viestintapalvelu.letter.async.letter.status", letterBatchId),
                Duration.ofMinutes(1),
                new com.google.gson.reflect.TypeToken<LetterBatchStatusDto>() {}.getType()
        );
    }

    public Observable<LetterBatchCountDto> haeTuloskirjeenMuodostuksenTilanne(String hakuOid, String tyyppi, String kieli) {
        return getAsObservableLazily(
                getUrl("viestintapalvelu.luotettu.letter.count.type.language", hakuOid, tyyppi, kieli),
                LetterBatchCountDto.class,
                ACCEPT_JSON);
    }

    @Override
    public CompletableFuture<List<TemplateHistory>> haeKirjepohja(String hakuOid, String tarjoajaOid, String templateName, String languageCode, String hakukohdeOid) {
        HashMap<String, String> query = new HashMap<>();
        query.put("applicationPeriod", hakuOid);
        query.put("oid", tarjoajaOid);
        query.put("templateName", templateName);
        query.put("languageCode", languageCode);
        query.put("tag", hakukohdeOid);
        return this.client.getJson(
                getUrl("viestintapalvelu.template.gethistory", query),
                Duration.ofMinutes(1),
                new com.google.gson.reflect.TypeToken<List<TemplateHistory>>() {}.getType()
        );
    }

    @Override
    public Observable<Response> haeOsoitetarrat(Osoitteet osoitteet) {
        return postAsObservableLazily(
            getUrl("viestintapalvelu.addresslabel.sync.pdf"),
            Entity.json(osoitteet),
            webClient -> webClient.accept(APPLICATION_OCTET_STREAM_TYPE));
    }

    @Override
    public Observable<Optional<Long>> haeKirjelahetysEPostille(String hakuOid, String kirjeenTyyppi, String asiointikieli) {
        return haeKirjelahetys(getUrl("viestintapalvelu.luotettu.letter.getbatchidreadyforeposti"),
                hakuOid, kirjeenTyyppi, asiointikieli);
    }

    @Override
    public Observable<Optional<Long>> haeKirjelahetysJulkaistavaksi(String hakuOid, String kirjeenTyyppi, String asiointikieli) {
        return haeKirjelahetys(getUrl("viestintapalvelu.luotettu.letter.getbatchidreadyforpublish"),
                hakuOid, kirjeenTyyppi, asiointikieli);
    }

    private Observable<Optional<Long>> haeKirjelahetys(String url, String hakuOid, String kirjeenTyyppi, String asiointikieli) {
        return getAsObservableLazily(
                url,
                (batchIdAsString) ->
                        StringUtils.isNumeric(batchIdAsString) ? Optional.of(Long.parseLong(batchIdAsString)) : Optional.empty(),
                client -> {
                    client.accept(MediaType.TEXT_PLAIN_TYPE);
                    client.query("hakuOid", hakuOid);
                    client.query("type", kirjeenTyyppi);
                    client.query("language", asiointikieli);
                    return client;
                });
    }

    @Override
    public Observable<Optional<Long>> julkaiseKirjelahetys(Long batchId) {
        return getAsObservableLazily(
                getUrl("viestintapalvelu.luotettu.letter.publishletterbatch", batchId),
                (batchIdAsString) ->
                        StringUtils.isNumeric(batchIdAsString) ? Optional.of(Long.parseLong(batchIdAsString)) : Optional.empty(),
                client -> client.accept(MediaType.TEXT_PLAIN_TYPE));
    }

    @Override
    public Observable<Map<String, String>> haeEPostiOsoitteet(Long batchId) {
        return getAsObservableLazily(
                getUrl("viestintapalvelu.luotettu.letter.getepostiadressesforletterbatch", batchId),
                new TypeToken<Map<String, String>>() {
                }.getType(),
                ACCEPT_JSON);
    }
}
