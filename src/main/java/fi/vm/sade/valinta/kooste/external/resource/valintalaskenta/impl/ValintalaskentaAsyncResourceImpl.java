package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.*;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.JonoDto;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class ValintalaskentaAsyncResourceImpl extends UrlConfiguredResource implements ValintalaskentaAsyncResource {
    private final static Logger LOG = LoggerFactory.getLogger(ValintalaskentaAsyncResourceImpl.class);

    public ValintalaskentaAsyncResourceImpl() {
        super(TimeUnit.HOURS.toMillis(8));
    }
    @Override
    public Observable<List<JonoDto>> jonotSijoitteluun(String hakuOid) {
        return getAsObservable("/valintalaskentakoostepalvelu/jonotsijoittelussa/" + hakuOid, new GenericType<List<JonoDto>>() {
        }.getType());
    }
    @Override
    public Observable<List<ValintatietoValinnanvaiheDTO>> laskennantulokset(String hakukohdeOid) {
        return getAsObservable(
                getUrl("valintalaskenta-laskenta-service.valintalaskentakoostepalvelu.hakukohde.valinnanvaihe", hakukohdeOid),
                new GenericType<List<ValintatietoValinnanvaiheDTO>>() {
        }.getType());
    }

    public Observable<String> laske(LaskeDTO laskeDTO) {
        return postAsObservable(
                getUrl("valintalaskenta-laskenta-service.valintalaskenta.laske"),
                String.class,
                Entity.entity(laskeDTO, MediaType.APPLICATION_JSON_TYPE),
                client -> client.accept(MediaType.TEXT_PLAIN_TYPE));
    }

    @Override
    public Observable<String> valintakokeet(LaskeDTO laskeDTO) {
        return postAsObservable(
                getUrl("valintalaskenta-laskenta-service.valintalaskenta.valintakokeet"),
                String.class,
                Entity.entity(laskeDTO, MediaType.APPLICATION_JSON_TYPE),
                client -> client.accept(MediaType.TEXT_PLAIN_TYPE));
    }

    @Override
    public Observable<ValinnanvaiheDTO> lisaaTuloksia(String hakuOid, String hakukohdeOid, String tarjoajaOid, ValinnanvaiheDTO vaihe) {
        final Entity<ValinnanvaiheDTO> entity = Entity.entity(vaihe, MediaType.APPLICATION_JSON_TYPE);
        return postAsObservable(
                getUrl("valintalaskenta-laskenta-service.valintalaskentakoostepalvelu.hakukohde.valinnanvaihe", hakukohdeOid),
                ValinnanvaiheDTO.class, entity, (webclient) -> webclient.query("tarjoajaOid", tarjoajaOid));
    }

    @Override
    public Observable<String> laskeKaikki(LaskeDTO laskeDTO) {
        return postAsObservable(
                getUrl("valintalaskenta-laskenta-service.valintalaskenta.laskekaikki"),
                String.class,
                Entity.entity(laskeDTO, MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.TEXT_PLAIN_TYPE);
                    return client;
                });
    }


    @Override
    public Peruutettava laskeJaSijoittele(List<LaskeDTO> lista, Consumer<String> callback, Consumer<Throwable> failureCallback) {
        try {
            String url = getUrl("valintalaskenta-laskenta-service.valintalaskenta.laskejasijoittele");
            return new PeruutettavaImpl(
                    getWebClient()
                            .path(url)
                            .async()
                            .post(Entity.entity(lista, MediaType.APPLICATION_JSON_TYPE), new GsonResponseCallback<String>(gson(), url, callback, failureCallback, new TypeToken<String>() {
                            }.getType())));
        } catch (Exception e) {
            LOG.error("Virhe laske ja sijoittele kutsussa", e);
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

}
