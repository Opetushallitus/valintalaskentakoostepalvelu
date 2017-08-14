package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.http.DateDeserializer;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.HakukohteenValintatulosUpdateStatuses;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


@Service
public class SijoitteluAsyncResourceImpl extends UrlConfiguredResource implements SijoitteluAsyncResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private static final Gson GSON = DateDeserializer.gsonBuilder().create();

    @Autowired
    public SijoitteluAsyncResourceImpl(
            @Qualifier("SijoitteluServiceRestClientCasInterceptor") AbstractPhaseInterceptor casInterceptor) {
        super(TimeUnit.MINUTES.toMillis(50), casInterceptor);
    }

    @Override
    public Observable<HakukohdeDTO> getHakukohdeBySijoitteluajoPlainDTO(String hakuOid, String hakukohdeOid) {
        return getAsObservable(
                getUrl("sijoittelu-service.sijoittelu.sijoitteluajo.hakukohde", hakuOid, SijoitteluResource.LATEST, hakukohdeOid),
                new TypeToken<HakukohdeDTO>() {}.getType(),
                client -> {
                    client.accept(MediaType.WILDCARD_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<HakukohdeDTO> asetaJononValintaesitysHyvaksytyksi(String hakuOid, String hakukohdeOid, String valintatapajonoOid, Boolean hyvaksytty) {
        return postAsObservable(
                getUrl("sijoittelu-service.tila.haku.hakukohde.valintatapajono.valintaesitys", hakuOid, hakukohdeOid, valintatapajonoOid),
                HakukohdeDTO.class, Entity.json(""), (webclient) -> webclient.query("hyvaksytty", hyvaksytty));
    }

    @Override
    public Observable<HakukohteenValintatulosUpdateStatuses> muutaHakemuksenTilaa(String hakuOid, String hakukohdeOid, List<Valintatulos> valintatulokset, String selite) {
        try {
            String encodedSelite = URLEncoder.encode(selite, "UTF-8");
            return postAsObservable(
                    getUrl("sijoittelu-service.tila.haku.hakukohde", hakuOid, hakukohdeOid),
                    HakukohteenValintatulosUpdateStatuses.class,
                    Entity.json(valintatulokset),
                    (webclient) -> webclient.query("selite", encodedSelite));
        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        }
    }

    @Override
    public Observable<HakukohteenValintatulosUpdateStatuses> tarkistaEtteivatValintatuloksetMuuttuneetHakemisenJalkeen(List<Valintatulos> valintatulokset) {
        return postAsObservable(getUrl("sijoittelu-service.tila.checkstaleread"),
                HakukohteenValintatulosUpdateStatuses.class, Entity.json(valintatulokset));
    }

    public void getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid, Consumer<HakukohdeDTO> hakukohde, Consumer<Throwable> poikkeus) {
        String url = getUrl("sijoittelu-service.sijoittelu.sijoitteluajo.hakukohde", hakuOid, SijoitteluResource.LATEST, hakukohdeOid);
        getWebClient()
                .path(url)
                .accept(MediaType.WILDCARD)
                .async()
                .get(new GsonResponseCallback<HakukohdeDTO>(GSON, url, hakukohde, poikkeus, new TypeToken<HakukohdeDTO>() {}.getType()));
    }

    public Future<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid, String valintatapajonoOid) {
        return getWebClient()
                .path(getUrl("sijoittelu-service.tila.hakukohde.hakukohdeoid.valintatapajonooid", hakukohdeOid, valintatapajonoOid))
                .accept(MediaType.WILDCARD)
                .async()
                .get(new GenericType<List<Valintatulos>>() {});
    }

    public Future<HakukohdeDTO> getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid) {
        return getWebClient()
                .path(getUrl("sijoittelu-service.sijoittelu.sijoitteluajo.hakukohde", hakuOid, SijoitteluResource.LATEST, hakukohdeOid))
                .accept(MediaType.WILDCARD)
                .async()
                .get(new GenericType<HakukohdeDTO>() {});
    }

    public Observable<HakijaPaginationObject> getKoulutuspaikkalliset(String hakuOid) {
        return getAsObservable(
                getUrl("sijoittelu-service.sijottelu.hyvaksytyt", hakuOid),
                new TypeToken<HakijaPaginationObject>() {}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<HakijaPaginationObject> getKoulutuspaikkalliset(String hakuOid, String hakukohdeOid) {
        return getAsObservable(
                getUrl("sijoittelu-service.sijoittelu.hyvaksytyt.hakukohde", hakuOid, hakukohdeOid),
                new TypeToken<HakijaPaginationObject>() {}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<HakijaDTO> getHakijaByHakemus(String hakuOid, String hakemusOid) {
        return getAsObservable(
                getUrl("sijoittelu-service.sijoittelu.sijoitteluajo.latest.hakemus", hakuOid, hakemusOid),
                new TypeToken<HakijaDTO>() {}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    public Observable<HakukohdeDTO> getLatestHakukohdeBySijoittelu(String hakuOid, String sijoitteluAjoId, String hakukohdeOid) {
        return getAsObservable(
                getUrl("sijoittelu-service.sijoittelu.sijoitteluajo.hakukohde", hakuOid, sijoitteluAjoId, hakukohdeOid),
                new TypeToken<HakukohdeDTO>() {}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

}
