package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.http.FailedHttpException;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.HakukohteenValintatulosUpdateStatuses;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.ValintatulosUpdateStatus;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import rx.Observable;

@Service
public class TilaAsyncResourceImpl extends UrlConfiguredResource implements TilaAsyncResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    public TilaAsyncResourceImpl(
            @Qualifier("sijoitteluTilaServiceRestClientCasInterceptor") AbstractPhaseInterceptor casInterceptor) {
        super(TimeUnit.MINUTES.toMillis(50), casInterceptor);

    }

    public Observable<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid) {
        return getAsObservable(getUrl("sijoittelu-service.tila.hakukohde.hakukohdeoid", hakukohdeOid),
                new TypeToken<List<Valintatulos>>() {
            }.getType(), client -> {
                client.accept(MediaType.WILDCARD);
                return client;
            });
    }

    @Override
    public Observable<Valintatulos> getHakemuksenSijoittelunTulos(String hakemusOid, String hakuOid, String hakukohdeOid, String valintatapajonoOid) {
        return getAsObservable(
                getUrl("sijoittelu-service.tila.hakemusoid.hakuoid.hakukohdeoid.valintatapajonooid",
                        hakemusOid, hakuOid, hakukohdeOid, valintatapajonoOid),
                new TypeToken<Valintatulos>() {
            }.getType(), client -> {
                client.accept(MediaType.WILDCARD);
                return client;
            });
    }

    @Override
    public Observable<List<Valintatulos>> getHakemuksenTulokset(String hakemusOid) {
        return getAsObservable(
                getUrl("sijoittelu-service.tila.hakemusoid", hakemusOid),
                new TypeToken<List<Valintatulos>>() {
            }.getType(), client -> {
                client.accept(MediaType.WILDCARD);
                return client;
            });
    }

    @Override
    public Observable<List<Valintatulos>> getValintatuloksetValintatapajonolle(String hakukohdeOid, String valintatapajonoOid) {
        return getAsObservable(
                getUrl("sijoittelu-service.tila.hakukohde.hakukohdeoid.valintatapajonooid", hakukohdeOid, valintatapajonoOid),
                new TypeToken<List<Valintatulos>>() {
            }.getType(), client -> {
                client.accept(MediaType.WILDCARD);
                return client;
            });
    }

    @Override
    public Observable<List<ValintatulosUpdateStatus>> tuoErillishaunTilat(String hakuOid, String hakukohdeOid, Collection<ErillishaunHakijaDTO> erillishaunHakijat) {
        Observable<List<ValintatulosUpdateStatus>> r = postAsObservable(
                getUrl("sijoittelu-service.tila.erillishaku.hakukohde", hakuOid, hakukohdeOid),
                Entity.entity(erillishaunHakijat, MediaType.APPLICATION_JSON_TYPE)
        ).map(s -> Collections.emptyList());
        return r.onErrorResumeNext(t -> {
            HakukohteenValintatulosUpdateStatuses h = ((FailedHttpException) t).response.readEntity(HakukohteenValintatulosUpdateStatuses.class);
            if (h.statuses.isEmpty()) {
                if (h.message == null) {
                    return Observable.error(t);
                } else {
                    return Observable.error(new RuntimeException(h.message));
                }
            } else {
                return Observable.just(h.statuses);
            }
        });
    }
}
