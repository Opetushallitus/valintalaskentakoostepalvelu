package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import static fi.vm.sade.valintalaskenta.domain.HakukohteenLaskennanTila.UUSI;
import static fi.vm.sade.valintalaskenta.domain.HakukohteenLaskennanTila.VALMIS;
import static fi.vm.sade.valintalaskenta.domain.HakukohteenLaskennanTila.VIRHE;

import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.JonoDto;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.Laskentakutsu;
import fi.vm.sade.valintalaskenta.domain.dto.SuoritustiedotDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
public class ValintalaskentaAsyncResourceImpl extends UrlConfiguredResource implements ValintalaskentaAsyncResource {
    private final static Logger LOG = LoggerFactory.getLogger(ValintalaskentaAsyncResourceImpl.class);
    private final int MAX_POLL_INTERVAL_IN_SECONDS = 30;
    private final HttpClient httpclient;

    public ValintalaskentaAsyncResourceImpl(@Qualifier("ValintalaskentaHttpClient") HttpClient httpclient) {
        super(TimeUnit.HOURS.toMillis(8));
        this.httpclient = httpclient;
    }
    @Override
    public Observable<List<JonoDto>> jonotSijoitteluun(String hakuOid) {
        return getAsObservableLazily("/valintalaskentakoostepalvelu/jonotsijoittelussa/" + hakuOid, new GenericType<List<JonoDto>>() {
        }.getType());
    }

    @Override
    public Observable<List<ValintatietoValinnanvaiheDTO>> laskennantulokset(String hakukohdeOid) {
        return getAsObservableLazily(
                getUrl("valintalaskenta-laskenta-service.valintalaskentakoostepalvelu.hakukohde.valinnanvaihe", hakukohdeOid),
                new GenericType<List<ValintatietoValinnanvaiheDTO>>() {
        }.getType());
    }

    public Observable<String> laske(LaskeDTO laskeDTO, SuoritustiedotDTO suoritukset) {
        if (LOG.isDebugEnabled()) {
            logitaKokotiedot(laskeDTO);
        }
        Laskentakutsu laskentakutsu = new Laskentakutsu(laskeDTO, suoritukset);
        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.laske", laskentakutsu);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Observable<String> valintakokeet(LaskeDTO laskeDTO, SuoritustiedotDTO suoritustiedot) {
        if (LOG.isDebugEnabled()) {
            logitaKokotiedot(laskeDTO);
        }
        Laskentakutsu laskentakutsu = new Laskentakutsu(laskeDTO, suoritustiedot);
        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.valintakokeet", laskentakutsu);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Observable<String> laskeKaikki(LaskeDTO laskeDTO, SuoritustiedotDTO suoritustiedot) {
        if (LOG.isDebugEnabled()) {
            logitaKokotiedot(laskeDTO);
        }
        Laskentakutsu laskentakutsu = new Laskentakutsu(laskeDTO, suoritustiedot);
        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.laskekaikki", laskentakutsu);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Observable<String> laskeJaSijoittele(List<LaskeDTO> lista, SuoritustiedotDTO suoritustiedot) {
        if (LOG.isDebugEnabled()) {
            lista.forEach(this::logitaKokotiedot);
            logitaSuoritustietojenKoko(suoritustiedot);
        }
        Laskentakutsu laskentakutsu = new Laskentakutsu(lista, suoritustiedot);
        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.laskejasijoittele", laskentakutsu);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Observable<ValinnanvaiheDTO> lisaaTuloksia(String hakuOid, String hakukohdeOid, String tarjoajaOid, ValinnanvaiheDTO vaihe) {
        final Entity<ValinnanvaiheDTO> entity = Entity.entity(vaihe, MediaType.APPLICATION_JSON_TYPE);
        return postAsObservableLazily(
                getUrl("valintalaskenta-laskenta-service.valintalaskentakoostepalvelu.hakukohde.valinnanvaihe", hakukohdeOid),
                ValinnanvaiheDTO.class, entity, (webclient) -> webclient.query("tarjoajaOid", tarjoajaOid));
    }

    public Observable<String> pollaa(int pollInterval, Object result, String uuid, String pollKey) {
        if (VALMIS.equals(result)) {
            return Observable.just(VALMIS);
        } else if (VIRHE.equals(result)) {
            LOG.error("Virhe laskennan suorituksessa, lopetetaan");
            return Observable.error(new RuntimeException(String.format("(Pollkey: %s) Laskenta epäonnistui!", pollKey)));
        } else {
            if(pollInterval == MAX_POLL_INTERVAL_IN_SECONDS) {
                LOG.warn(String.format("(Pollkey=%s) Laskenta kestää pitkään! Jatketaan pollausta.", pollKey));
            }
            return Observable.timer(pollInterval, TimeUnit.SECONDS).switchMap(d -> {
                String url = getUrl("valintalaskenta-laskenta-service.valintalaskenta.status", pollKey);
                return getAsObservableLazily(url, String.class, client -> {
                    client.accept(MediaType.TEXT_PLAIN_TYPE);
                    return client;
                }).switchMap(rval -> pollaa(Math.min(pollInterval *2, MAX_POLL_INTERVAL_IN_SECONDS), rval, uuid, pollKey));
            });
        }
    }

    public Observable<String> kutsuRajapintaaPollaten(String api, Laskentakutsu laskentakutsu) {
        LOG.info("(Uuid: {}) Lähetetään laskenta-servicelle laskentakutsu. (Pollkey: {})", laskentakutsu.getUuid(), laskentakutsu.getPollKey());

        CompletableFuture<String> requestFuture = httpclient.post(
            getUrl(api),
            Duration.ofMinutes(10),
            httpclient.createJsonBodyPublisher(laskentakutsu, Laskentakutsu.class),
            builder -> builder
                .header("Content-Type", "application/json")
                .header("Accept", "text/plain"),
            httpclient::parseTxt);

        return Observable.fromFuture(requestFuture).switchMap(rval -> {
            if (UUSI.equals(rval)) {
                LOG.info("Saatiin tieto, että uusi laskenta on luotu (Pollkey: {}). Pollataan sen tilaa kunnes se on päättynyt (VALMIS tai VIRHE).", laskentakutsu.getPollKey());
                return pollaa(1, rval, laskentakutsu.getUuid(), laskentakutsu.getPollKey());
            } else {
                LOG.error("Yritettiin käynnistää laskenta, mutta saatiin palautusarvona {} eikä UUSI. Pollauksen pitäisi olla käynnissä muualla. Ei pollata.", rval);
                return Observable.error(new RuntimeException(String.format("Laskenta (pollKey=%s) epäonnistui!", laskentakutsu.getPollKey())));
            }
        });
    }

    private void logitaKokotiedot(LaskeDTO laskeDTO) {
        Function<Object, Integer> koonLaskenta = o -> gson().toJson(o).length();
        try {
            LOG.debug(String.format("laskeDTO %s (hakukohde %s) koot: %s", laskeDTO.getUuid(), laskeDTO.getHakukohdeOid(), laskeDTO.logSerializedSizes(koonLaskenta)));
        } catch (Exception e) {
            LOG.error(String.format("Virhe, kun yritettiin logittaa laskeDTO:n %s (hakukohde %s) kokoa", laskeDTO.getUuid(), laskeDTO.getHakukohdeOid()), e);
        }
    }

    private void logitaSuoritustietojenKoko(SuoritustiedotDTO suoritustiedotDTO) {
        try {
            LOG.debug(String.format("Suoritustietojen koko: %s", gson().toJson(suoritustiedotDTO).length()));
        } catch (Exception e) {
            LOG.error("Virhe, kun yritettiin logittaa suoritustietojen kokoa", e);
        }
    }
}
