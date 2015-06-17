package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.external.resource.AsyncResourceWithCas;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import rx.Observable;

@Service
public class TilaAsyncResourceImpl extends AsyncResourceWithCas implements TilaAsyncResource {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, (JsonDeserializer) (json, typeOfT, context) -> new Date(json.getAsJsonPrimitive().getAsLong()))
            .create();

    @Autowired
    public TilaAsyncResourceImpl(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.sijoittelu-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.sijoittelu}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.sijoittelu}") String appClientPassword,
            @Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}") String address,
            ApplicationContext context
    ) {
        super(webCasUrl, targetService, appClientUsername, appClientPassword, address, context, TimeUnit.MINUTES.toMillis(50));
    }

    public void getValintatulokset(String hakuOid, String hakukohdeOid
            , Consumer<List<Valintatulos>> valintatulokset, Consumer<Throwable> poikkeus) {
        ///tila/hakukohde/{hakukohdeOid}
        String url = "/tila/hakukohde/" + hakukohdeOid;
        getWebClient()
                .path(url)
                .accept(MediaType.WILDCARD)
                .async().get(new GsonResponseCallback<>(GSON, address, url, valintatulokset, poikkeus,
                new TypeToken<List<Valintatulos>>() {}.getType()));
    }

    public Observable<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid) {
        String url = "/tila/hakukohde/" + hakukohdeOid;
        return getAsObservable(
                url,
                new TypeToken<List<Valintatulos>>() {}.getType(),
                client -> {
                    client.accept(MediaType.WILDCARD);
                    return client;
                }
        );
    }

    public Future<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid, String valintatapajonoOid) {
        String url = "/tila/hakukohde/" + hakukohdeOid + "/" + valintatapajonoOid;
        return getWebClient()
                .path(url)
                .accept(MediaType.WILDCARD)
                .async().get(new GenericType<List<Valintatulos>>() {});
    }

    @Override
    public Response tuoErillishaunTilat(String hakuOid, String hakukohdeOid, String valintatapajononNimi, Collection<ErillishaunHakijaDTO> erillishaunHakijat) {
        String url = "/tila/erillishaku/" + hakuOid + "/hakukohde/" + hakukohdeOid + "/";
        LOG.info("Asynkroninen kutsu: {}{}?hyvaksytyt=true&hakukohdeOid={}&valintatapajononNimi={}", address, url, hakukohdeOid, valintatapajononNimi);
        return getWebClient()
                .path(url)
                .query("valintatapajononNimi", Optional.ofNullable(valintatapajononNimi).orElse(StringUtils.EMPTY))
                .post(Entity.entity(erillishaunHakijat, MediaType.APPLICATION_JSON_TYPE));
    }
}