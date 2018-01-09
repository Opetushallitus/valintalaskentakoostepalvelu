package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fi.vm.sade.valinta.http.ResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.*;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto.LetterBatchCountDto;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class ViestintapalveluAsyncResourceImpl extends UrlConfiguredResource implements ViestintapalveluAsyncResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private final Gson GSON = new Gson();

    @Autowired
    public ViestintapalveluAsyncResourceImpl(
            @Qualifier("viestintapalveluClientCasInterceptor") AbstractPhaseInterceptor casInterceptor)
    {
        super(TimeUnit.HOURS.toMillis(20), casInterceptor);
    }

    @Override
    public Observable<LetterResponse> viePdfJaOdotaReferenssiObservable(LetterBatch letterBatch) {
        return postAsObservableLazily(
                getUrl("viestintapalvelu.letter.async.letter"),
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
        return getAsObservableLazily(
                getUrl("viestintapalvelu.letter.async.letter.status", letterBatchId),
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
        return getWebClient()
                .path(getUrl("viestintapalvelu.letter.async.letter.status", letterBatchId))
                .accept(MediaType.APPLICATION_JSON_TYPE).async()
                .get(LetterBatchStatusDto.class);
    }

    public Future<LetterResponse> viePdfJaOdotaReferenssi(LetterBatch letterBatch) {
        return getWebClient()
                .path(getUrl("viestintapalvelu.letter.async.letter"))
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(GSON.toJson(letterBatch), MediaType.APPLICATION_JSON_TYPE), LetterResponse.class);
    }

    public Observable<LetterBatchCountDto> haeTuloskirjeenMuodostuksenTilanne(String hakuOid, String tyyppi, String kieli) {
        return getAsObservableLazily(
                getUrl("viestintapalvelu.luotettu.letter.count.type.language", hakuOid, tyyppi, kieli),
                LetterBatchCountDto.class, client -> { client.accept(MediaType.APPLICATION_JSON_TYPE);return client; });
    }

    @Override
    public Observable<List<TemplateHistory>> haeKirjepohja(String hakuOid, String tarjoajaOid, String templateName, String languageCode, String hakukohdeOid) {
        String url = getUrl("viestintapalvelu.template.gethistory");
        LOG.info("######## TemplateHistory {}?applicationPeriod={}&oid={}&templateName={}&languageCode={}&tag={}", url, hakuOid, tarjoajaOid, templateName, languageCode, hakukohdeOid);
        return getAsObservableLazily(
                url,
                new TypeToken<List<TemplateHistory>>() {}.getType(),
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
        String url = getUrl("viestintapalvelu.addresslabel.sync.pdf");
        try {
            return new PeruutettavaImpl(
                    getWebClient()
                            .path(url)
                            .async()
                            .post(Entity.json(osoitteet), new ResponseCallback(
                                    url,
                                    callback,
                                    failureCallback)));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
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
                }
        );
    }

    @Override
    public Observable<Optional<Long>> julkaiseKirjelahetys(Long batchId) {
        return getAsObservableLazily(
                getUrl("viestintapalvelu.luotettu.letter.publishletterbatch", batchId),
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
        return getAsObservableLazily(
                getUrl("viestintapalvelu.luotettu.letter.getepostiadressesforletterbatch", batchId),
                new TypeToken<Map<String, String>>() {
                }.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }
}
