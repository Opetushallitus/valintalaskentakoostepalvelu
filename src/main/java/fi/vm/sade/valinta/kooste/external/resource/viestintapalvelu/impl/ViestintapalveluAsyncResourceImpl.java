package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fi.vm.sade.valinta.http.ResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.AsyncResourceWithCas;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto.LetterBatchCountDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class ViestintapalveluAsyncResourceImpl extends AsyncResourceWithCas implements ViestintapalveluAsyncResource {

    private final Gson GSON = new Gson();

    @Autowired
    public ViestintapalveluAsyncResourceImpl(
            @Qualifier("viestintapalveluClientCasInterceptor") AbstractPhaseInterceptor casInterceptor,
            @Value("${valintalaskentakoostepalvelu.viestintapalvelu.url}") String address,
            ApplicationContext context
    ) {
        super(casInterceptor, address, context, TimeUnit.HOURS.toMillis(20));
    }

    @Override
    public Observable<LetterResponse> viePdfJaOdotaReferenssiObservable(LetterBatch letterBatch) {
        return postAsObservable(
                "/api/v1/letter/async/letter",
                new TypeToken<LetterResponse>() {
                }.getType(),
                Entity.entity(GSON.toJson(letterBatch), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<LetterBatchStatusDto> haeStatusObservable(String letterBatchId) {
        return getAsObservable(
                "/api/v1/letter/async/letter/status/" + letterBatchId,
                new TypeToken<LetterBatchStatusDto>() {
                }.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Future<LetterBatchStatusDto> haeStatus(String letterBatchId) {
        String url = "/api/v1/letter/async/letter/status/" + letterBatchId;
        return getWebClient().path(url)
                .accept(MediaType.APPLICATION_JSON_TYPE).async()
                .get(LetterBatchStatusDto.class);
    }

    public Future<LetterResponse> viePdfJaOdotaReferenssi(LetterBatch letterBatch) {
        String url = "/api/v1/letter/async/letter";
        return getWebClient()
                .path(url)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(GSON.toJson(letterBatch), MediaType.APPLICATION_JSON_TYPE), LetterResponse.class);
    }

    public Observable<LetterBatchCountDto> haeTuloskirjeenMuodostuksenTilanne(String hakuOid, String tyyppi, String kieli) {
        String url = String.format("/api/v1/luotettu/letter/count/%s/type/%s/language/%s", hakuOid, tyyppi, kieli);
        return getAsObservable(url, LetterBatchCountDto.class, client -> { client.accept(MediaType.APPLICATION_JSON_TYPE);return client; });
    }

    @Override
    public Observable<List<TemplateHistory>> haeKirjepohja(String hakuOid, String tarjoajaOid, String templateName, String languageCode, String hakukohdeOid) {
        LOG.error("######## TemplateHistory {}/api/v1/template/getHistory?applicationPeriod={}&oid={}&templateName={}&languageCode={}&tag={}", address, hakuOid, tarjoajaOid, templateName, languageCode, hakukohdeOid);
        return getAsObservable(
                "/api/v1/template/getHistory",
                new TypeToken<List<TemplateHistory>>() {
                }.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    client.query("applicationPeriod", hakuOid);
                    client.query("oid", tarjoajaOid);
                    client.query("templateName", templateName);
                    client.query("languageCode", languageCode);
                    client.query("tag", hakukohdeOid);
                    return client;
                }
        );
    }

    @Override
    public Peruutettava haeOsoitetarrat(Osoitteet osoitteet, Consumer<Response> callback, Consumer<Throwable> failureCallback) {
        String url = "/api/v1/addresslabel/sync/pdf";
        try {
            return new PeruutettavaImpl(
                    getWebClient()
                            .path(url)
                            .async()
                            .post(Entity.json(osoitteet), new ResponseCallback(
                                    address + url,
                                    callback,
                                    failureCallback)));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    @Override
    public Observable<Optional<Long>> haeKirjelahetysEPostille(String hakuOid, String kirjeenTyyppi, String asiointikieli) {
        return haeKirjelahetys("/api/v1/luotettu/letter/getBatchIdReadyForEPosti", hakuOid, kirjeenTyyppi, asiointikieli);
    }

    @Override
    public Observable<Optional<Long>> haeKirjelahetysJulkaistavaksi(String hakuOid, String kirjeenTyyppi, String asiointikieli) {
        return haeKirjelahetys("/api/v1/luotettu/letter/getBatchIdReadyForPublish", hakuOid, kirjeenTyyppi, asiointikieli);
    }

    private Observable<Optional<Long>> haeKirjelahetys(String url, String hakuOid, String kirjeenTyyppi, String asiointikieli) {
        return getAsObservable(
                url,
                (batchIdAsString) ->
                        StringUtils.isNumeric(batchIdAsString) ? Optional.of(Long.parseLong(batchIdAsString)) : Optional.empty(),
                client -> {
                    client.accept(MediaType.TEXT_PLAIN_TYPE);
                    client.query("hakuOid", hakuOid);
                    client.query("type", kirjeenTyyppi);
                    client.query("language", asiointikieli);
                    return client;
                }
        );
    }

    @Override
    public Observable<Optional<Long>> julkaiseKirjelahetys(Long batchId) {
        return getAsObservable(
                "/api/v1/luotettu/letter/publishLetterBatch/" + batchId,
                (batchIdAsString) ->
                        StringUtils.isNumeric(batchIdAsString) ? Optional.of(Long.parseLong(batchIdAsString)) : Optional.empty(),
                client -> {
                    client.accept(MediaType.TEXT_PLAIN_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<Map<String, String>> haeEPostiOsoitteet(Long batchId) {
        return getAsObservable(
                "/api/v1/luotettu/letter/getEPostiAddressesForLetterBatch/" + batchId,
                new TypeToken<Map<String, String>>() {
                }.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }
}
