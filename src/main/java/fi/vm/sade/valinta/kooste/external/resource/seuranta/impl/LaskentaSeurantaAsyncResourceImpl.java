package fi.vm.sade.valinta.kooste.external.resource.seuranta.impl;

import com.google.gson.Gson;

import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.http.ResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.resource.LaskentaParams;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.TunnisteDto;
import org.apache.cxf.jaxrs.client.WebClient;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


@Service
public class LaskentaSeurantaAsyncResourceImpl extends UrlConfiguredResource implements LaskentaSeurantaAsyncResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final Gson gson = new Gson();

    @Autowired
    public LaskentaSeurantaAsyncResourceImpl(
        @Qualifier("SeurantaRestClientCasInterceptor") AbstractPhaseInterceptor casInterceptor) {
        super(TimeUnit.HOURS.toMillis(1), casInterceptor);
    }

    @Override
    public Observable<String> otaSeuraavaLaskentaTyonAlle() {
        return getAsObservableLazily(getUrl("seuranta-service.seuranta.laskenta.otaseuraavalaskentatyonalle"), String.class);
    }

    public Observable<LaskentaDto> laskenta(String uuid) {
        return getAsObservableLazily(getUrl("seuranta-service.seuranta.kuormantasaus.laskenta", uuid), LaskentaDto.class);
    }

    public Observable<LaskentaDto> resetoiTilat(String uuid) {
        return putAsObservableLazily(
            getUrl("seuranta-service.seuranta.kuormantasaus.laskenta.resetoi", uuid),
            LaskentaDto.class,
            Entity.entity(uuid, MediaType.APPLICATION_JSON_TYPE));
    }

    public void luoLaskenta(LaskentaParams laskentaParams, List<HakukohdeDto> hakukohdeOids, Consumer<TunnisteDto> callback, Consumer<Throwable> failureCallback) {
        try {
            String url = getUrl("seuranta-service.seuranta.kuormantasaus.laskenta.tyyppi", laskentaParams.getHakuOid(), laskentaParams.getLaskentatyyppi());
            WebClient wc = getWebClient().path(url);
            wc.query("userOID", laskentaParams.getUserOID());
            if (laskentaParams.getNimi() != null) {
                wc.query("nimi", laskentaParams.getNimi());
            }
            wc.query("haunnimi", laskentaParams.getHaunNimi());
            wc.query("erillishaku", (Boolean) laskentaParams.isErillishaku());
            if (laskentaParams.getValinnanvaihe() != null) {
                wc.query("valinnanvaihe", laskentaParams.getValinnanvaihe());
            }
            if (laskentaParams.getIsValintakoelaskenta() != null) {
                wc.query("valintakoelaskenta", laskentaParams.getIsValintakoelaskenta());
            }
            wc.async().post(Entity.entity(hakukohdeOids, MediaType.APPLICATION_JSON_TYPE),
                new GsonResponseCallback<>(gson(),
                    url,
                    callback,
                    failureCallback,
                    TunnisteDto.class));
        } catch (Exception e) {
            failureCallback.accept(e);
        }
    }

    public Observable<Response> merkkaaLaskennanTila(String uuid, LaskentaTila tila, Optional<IlmoitusDto> ilmoitusDtoOptional) {
        String url = getUrl("seuranta-service.seuranta.kuormantasaus.laskenta.tila", uuid, tila);
        try {
            if (ilmoitusDtoOptional.isPresent()) {
                return postAsObservableLazily(url, Entity.entity(gson.toJson(ilmoitusDtoOptional.get()), MediaType.APPLICATION_JSON_TYPE));
            } else {
                return putAsObservableLazily(url, Entity.entity(tila, MediaType.APPLICATION_JSON_TYPE));
            }
        } catch (Exception e) {
            LOG.error("Seurantapalvelun kutsu paatyi virheeseen!" + url, e);
            return Observable.error(e);
        }
    }

    public void merkkaaLaskennanTila(String uuid, LaskentaTila tila, HakukohdeTila hakukohdetila, Optional<IlmoitusDto> ilmoitusDtoOptional) {
        String url = getUrl("seuranta-service.seuranta.kuormantasaus.laskenta.tila.hakukohde", uuid, tila, hakukohdetila);
        try {
            ResponseCallback responseCallback = new ResponseCallback(url, ok -> {}, fail -> {
                LOG.error("(UUID = {}) Laskennan tilan (laskenta={}, hakukohde={}) merkkaaminen epaonnistui! {}", uuid, tila, hakukohdetila, fail);
            });
            if (ilmoitusDtoOptional.isPresent()) {
                getWebClient()
                    .path(url)
                    .async()
                    .post(Entity.entity(gson.toJson(ilmoitusDtoOptional.get()), MediaType.APPLICATION_JSON_TYPE), responseCallback);
            } else {
                getWebClient()
                    .path(url)
                    .async()
                    .put(Entity.entity(tila, MediaType.APPLICATION_JSON_TYPE), responseCallback);
            }
        } catch (Exception e) {
            LOG.error("Seurantapalvelun kutsu " + url + " laskennalle " + uuid + " paatyi virheeseen", e);
        }
    }

    @Override
    public void merkkaaHakukohteenTila(String uuid, String hakukohdeOid, HakukohdeTila tila, Optional<IlmoitusDto> ilmoitusDtoOptional) {
        String url = getUrl("seuranta-service.seuranta.kuormantasaus.laskenta.hakukohde.tila", uuid, hakukohdeOid, tila);
        try {
            ResponseCallback responseCallback = new ResponseCallback(url, ok -> {}, fail -> {
                LOG.error("(UUID = {}) Hakukohteen ({}) tilan ({}) merkkaaminen epaonnistui! {}", uuid, hakukohdeOid, tila, fail);
            });
            if (ilmoitusDtoOptional.isPresent()) {
                getWebClient()
                    .path(url)
                    .async()
                    .post(Entity.entity(gson.toJson(ilmoitusDtoOptional.get()), MediaType.APPLICATION_JSON_TYPE), responseCallback);
            } else {
                getWebClient()
                    .path(url)
                    .async()
                    .put(Entity.entity(tila, MediaType.APPLICATION_JSON_TYPE), responseCallback);
            }
        } catch (Exception e) {
            LOG.error("Seurantapalvelun kutsu " + url + " laskennalle " + uuid + " ja hakukohteelle " + hakukohdeOid + " paatyi virheeseen", e);
        }
    }
}
