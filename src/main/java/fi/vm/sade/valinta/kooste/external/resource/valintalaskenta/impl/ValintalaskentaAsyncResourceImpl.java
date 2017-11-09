package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.JonoDto;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.Laskentakutsu;
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

import static fi.vm.sade.valintalaskenta.domain.HakukohteenLaskennanTila.VALMIS;
import static fi.vm.sade.valintalaskenta.domain.HakukohteenLaskennanTila.VIRHE;

@Service
public class ValintalaskentaAsyncResourceImpl extends UrlConfiguredResource implements ValintalaskentaAsyncResource {
    private final static Logger LOG = LoggerFactory.getLogger(ValintalaskentaAsyncResourceImpl.class);
    private final int MAX_POLL_INTERVAL_IN_SECONDS = 30;

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
        Laskentakutsu laskentakutsu = new Laskentakutsu(laskeDTO);

        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.laske", laskentakutsu);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Observable<String> valintakokeet(LaskeDTO laskeDTO) {
        Laskentakutsu laskentakutsu = new Laskentakutsu(laskeDTO);

        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.valintakokeet", laskentakutsu);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Observable<String> laskeKaikki(LaskeDTO laskeDTO) {
        Laskentakutsu laskentakutsu = new Laskentakutsu(laskeDTO);

        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.laskekaikki", laskentakutsu);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Observable<String> laskeJaSijoittele(List<LaskeDTO> lista) {
        Laskentakutsu laskentakutsu = new Laskentakutsu(lista);

        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.laskejasijoittele", laskentakutsu);
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

    public Observable<String> pollaa(int pollInterval, Object result, String uuid, String pollKey) {
        if (VALMIS.equals(result)) {
            return Observable.just(VALMIS);
        } else if (VIRHE.equals(result)) {
            LOG.error("Virhe laskennan suorituksessa, lopetetaan");
            return Observable.error(new RuntimeException(String.format("Laskenta uuid=%s, hakukohde=%s epäonnistui!", uuid, result)));
        } else {
            if(pollInterval == MAX_POLL_INTERVAL_IN_SECONDS) {
                LOG.warn("(Uuid={}) Laskenta hakukohteelle {} kestaa pitkaan! Jatketaan pollausta..", pollKey);
            }
            return Observable.timer(pollInterval, TimeUnit.SECONDS).switchMap(d -> {
                String url = getUrl("valintalaskenta-laskenta-service.valintalaskenta.status", pollKey);
                return getAsObservable(url, String.class, client -> {
                    client.accept(MediaType.TEXT_PLAIN_TYPE);
                    return client;
                }).switchMap(rval -> pollaa(Math.min(pollInterval *2, MAX_POLL_INTERVAL_IN_SECONDS), rval, uuid, pollKey));
            });
        }
    }

    public Observable<String> kutsuRajapintaaPollaten(String api, Laskentakutsu laskentakutsu) {
        LOG.info("(Uuid: {}) Lähetetään laskenta-servicelle laskentakutsu ja pollataan sen tilaa kunnes se on päättynyt. (Pollkey: {})", laskentakutsu.getUuid(), laskentakutsu.getPollKey());
        return postAsObservable(
                getUrl(api),
                String.class,
                Entity.entity(laskentakutsu, MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.TEXT_PLAIN_TYPE);
                    return client;
                }).switchMap(rval -> pollaa(1, rval, laskentakutsu.getUuid(), laskentakutsu.getPollKey()));
    }
}
