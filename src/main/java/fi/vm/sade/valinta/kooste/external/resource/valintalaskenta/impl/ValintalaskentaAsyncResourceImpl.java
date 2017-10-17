package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.*;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.JonoDto;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.HakukohteenLaskennanTila;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.observables.ConnectableObservable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static fi.vm.sade.valintalaskenta.domain.HakukohteenLaskennanTila.*;
import static fi.vm.sade.valintalaskenta.domain.HakukohteenLaskennanTila.VALMIS;

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

        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.laske", laskeDTO.getUuid(), laskeDTO.getHakukohdeOid(), laskeDTO);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Observable<String> valintakokeet(LaskeDTO laskeDTO) {

        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.valintakokeet", laskeDTO.getUuid(), laskeDTO.getHakukohdeOid(), laskeDTO);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Observable<String> laskeKaikki(LaskeDTO laskeDTO) {

        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.laskekaikki", laskeDTO.getUuid(), laskeDTO.getHakukohdeOid(), laskeDTO);
        } catch (Exception e) {
            throw e;
        }
    }

    //FIXME tämä toteutus toisteinen (mutta toimii). Aiheuttajana on valintaryhmälaskennan muista laskennoista poikkeava rakenne.
    @Override
    public Observable<String> laskeJaSijoittele(List<LaskeDTO> lista) {
        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.laskejasijoittele", lista.iterator().next().getUuid(), String.format("(%s hakukohdetta)", lista.size()), lista);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Observable<ValinnanvaiheDTO> lisaaTuloksia(String hakuOid, String hakukohdeOid, String tarjoajaOid, ValinnanvaiheDTO vaihe) {
        final Entity<ValinnanvaiheDTO> entity = Entity.entity(vaihe, MediaType.APPLICATION_JSON_TYPE);
        return postAsObservable(
                getUrl("valintalaskenta-laskenta-service.valintalaskentakoostepalvelu.hakukohde.valinnanvaihe", hakukohdeOid),
                ValinnanvaiheDTO.class, entity, (webclient) -> webclient.query("tarjoajaOid", tarjoajaOid));
    }

    public Observable<String> pollaa(Object result, String uuid, String hakukohdeOid) {
        if (VALMIS.equals(result)) {
            return Observable.just(VALMIS);
        } else if (VIRHE.equals(result)) {
            LOG.error("Virhe laskennan suorituksessa, lopetetaan");
            return Observable.error(new RuntimeException(String.format("Laskenta uuid=%s, hakukohde=%s epäonnistui!", uuid, result)));
        } else {
            return Observable.timer(3, TimeUnit.SECONDS).switchMap(d -> {
                String url = getUrl("valintalaskenta-laskenta-service.valintalaskenta.status", uuid, hakukohdeOid);
                return getAsObservable(url, String.class, client -> {
                    client.accept(MediaType.TEXT_PLAIN_TYPE);
                    return client;
                }).switchMap(rval -> pollaa(rval, uuid, hakukohdeOid));
            });
        }
    }

    public <T> Observable<String> kutsuRajapintaaPollaten(String api, String uuid, String hakukohdeOid, T laskeDTO) {
        return postAsObservable(
                getUrl(api),
                String.class,
                Entity.entity(laskeDTO, MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.TEXT_PLAIN_TYPE);
                    return client;
                }).switchMap(rval -> pollaa(rval, uuid, hakukohdeOid));
    }
}
