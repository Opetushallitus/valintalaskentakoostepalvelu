package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import static fi.vm.sade.valintalaskenta.domain.HakukohteenLaskennanTila.UUSI;
import static fi.vm.sade.valintalaskenta.domain.HakukohteenLaskennanTila.VALMIS;
import static fi.vm.sade.valintalaskenta.domain.HakukohteenLaskennanTila.VIRHE;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
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
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ValintalaskentaAsyncResourceImpl extends UrlConfiguredResource implements ValintalaskentaAsyncResource {
    private final static Logger LOG = LoggerFactory.getLogger(ValintalaskentaAsyncResourceImpl.class);
    private final int MAX_POLL_INTERVAL_IN_SECONDS = 30;
    private final HttpClient httpclient;
    private final ExecutorService lahetaLaskeDTOExecutor = Executors.newFixedThreadPool(1);

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
    public CompletableFuture<List<ValintatietoValinnanvaiheDTO>> laskennantulokset(String hakukohdeOid) {
        return this.laskennantulokset(hakukohdeOid, null);
    }

    @Override
    public CompletableFuture<List<ValintatietoValinnanvaiheDTO>> laskennantulokset(String hakukohdeOid, Executor executor) {
        return this.httpclient.getJson(
                getUrl("valintalaskenta-laskenta-service.valintalaskentakoostepalvelu.hakukohde.valinnanvaihe", hakukohdeOid),
                Duration.ofMinutes(1),
                new TypeToken<List<ValintatietoValinnanvaiheDTO>>() {}.getType(),
                executor
        );
    }

    public Observable<String> laske(LaskeDTO laskeDTO, SuoritustiedotDTO suoritustiedot) {
        Laskentakutsu laskentakutsu = new Laskentakutsu(laskeDTO, suoritustiedot);
        if (LOG.isDebugEnabled()) {
            logitaKokotiedot(laskeDTO);
            logitaSuoritustietojenKoko(suoritustiedot);
            LOG.debug(String.format("Suoritustietojen koko base64-gzippinä: %d", laskentakutsu.getSuoritustiedotDtoBase64Gzip().length()));
        }
        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.laske", laskentakutsu);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Observable<String> valintakokeet(LaskeDTO laskeDTO, SuoritustiedotDTO suoritustiedot) {
        Laskentakutsu laskentakutsu = new Laskentakutsu(laskeDTO, suoritustiedot);
        if (LOG.isDebugEnabled()) {
            logitaKokotiedot(laskeDTO);
            logitaSuoritustietojenKoko(suoritustiedot);
            LOG.debug(String.format("Suoritustietojen koko base64-gzippinä: %d", laskentakutsu.getSuoritustiedotDtoBase64Gzip().length()));
        }
        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.valintakokeet", laskentakutsu);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Observable<String> laskeKaikki(LaskeDTO laskeDTO, SuoritustiedotDTO suoritustiedot) {
        Laskentakutsu laskentakutsu = new Laskentakutsu(laskeDTO, suoritustiedot);
        if (LOG.isDebugEnabled()) {
            logitaKokotiedot(laskeDTO);
            logitaSuoritustietojenKoko(suoritustiedot);
            LOG.debug(String.format("Suoritustietojen koko base64-gzippinä: %d", laskentakutsu.getSuoritustiedotDtoBase64Gzip().length()));
        }
        try {
            return kutsuRajapintaaPollaten("valintalaskenta-laskenta-service.valintalaskenta.laskekaikki", laskentakutsu);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Observable<String> laskeJaSijoittele(String uuid, List<LaskeDTO> lista, SuoritustiedotDTO suoritustiedot) {
        Laskentakutsu laskentakutsu = Laskentakutsu.luoTyhjaValintaryhmaLaskentaPalasissaSiirtoaVarten(uuid);
        if (LOG.isDebugEnabled()) {
            lista.forEach(this::logitaKokotiedot);
            logitaSuoritustietojenKoko(suoritustiedot);
        }

        LOG.info(String.format("Laskenta %s : siirretään %d hakukohteen laskennan käynnistys paloissa valintalaskennalle.",
            laskentakutsu.getUuid(), lista.size()));

        try {
            return kutsuRajapintaaPollaten(laskentakutsu, (aloitaLaskentakutsunLahettaminenPaloissa(laskentakutsu)
                .thenComposeAsync(x -> CompletableFutureUtil.sequence(lista.stream()
                    .map(dto -> lahetaYksittainenLaskeDto(dto, laskentakutsu))
                    .collect(Collectors.toList())))
                .thenComposeAsync(x -> lahetaSuoritustiedot(suoritustiedot, laskentakutsu))
                .thenComposeAsync(x -> {
                    LOG.info(String.format("Laskenta %s : kaikkien %d hakukohteen laskentaresurssit on saatu siirrettyä paloissa valintalaskennalle. Lähetetään käynnistyskutsu!",
                        laskentakutsu.getUuid(), lista.size()));
                    return kaynnistaPaloissaSiirrettyLaskenta(laskentakutsu);
                })));
        } catch (Exception e) {
            LOG.error("Valintaryhmälaskennan käynnistyksessä tapahtui virhe", e);
            throw e;
        }
    }

    private CompletableFuture<String> aloitaLaskentakutsunLahettaminenPaloissa(Laskentakutsu laskentakutsu) {
        final String url = getUrl("valintalaskenta-laskenta-service.valintalaskenta.aloita.laskentakutsu.paloissa", laskentakutsu.getPollKey());
        return httpclient.post(
            url,
            Duration.ofMinutes(1),
            httpclient.createJsonBodyPublisher(laskentakutsu, Laskentakutsu.class),
            builder -> builder
                .header("Content-Type", "application/json")
                .header("Accept", "text/plain"),
            httpclient::parseTxt).whenComplete((tulos, poikkeus) -> {
            if (poikkeus != null) {
                LOG.error(String.format("Laskenta %s : Virhe lähetettäessä paloissa siirrettävän laskentakutsun aloitusta valintalaskennalle osoitteeseen '%s'",
                    laskentakutsu.getUuid(), url), poikkeus);
            } else {
                LOG.info(String.format("Laskenta %s : Saatiin valintalaskennalta paloissa siirrettävän laskentakutsun aloitukseen vastaus '%s'", laskentakutsu.getUuid(), tulos));
            }
        });
    }

    private CompletableFuture<String> lahetaYksittainenLaskeDto(LaskeDTO dto, Laskentakutsu laskentakutsu) {
        return CompletableFuture.supplyAsync(() -> {
            final String url = getUrl("valintalaskenta-laskenta-service.valintalaskenta.lisaa.hakukohde.laskentakutsuun", laskentakutsu.getPollKey());
            return httpclient.post(
                    url,
                    Duration.ofMinutes(10),
                    httpclient.createJsonBodyPublisher(dto, LaskeDTO.class),
                    builder -> builder
                            .header("Content-Type", "application/json")
                            .header("Accept", "text/plain"),
                    httpclient::parseTxt).whenComplete((tulos, poikkeus) -> {
                if (poikkeus != null) {
                    LOG.error(String.format("Laskenta %s , hakukohde %s : Virhe lähetettäessä hakukohteen lisäämistä kutsuun valintalaskennalle osoitteeseen '%s'",
                            laskentakutsu.getUuid(), dto.getHakukohdeOid(), url), poikkeus);
                } else {
                    LOG.info(String.format("Laskenta %s , hakukohde %s : Saatiin valintalaskennalta hakukohteen lisäämiseen vastaus '%s'",
                            laskentakutsu.getUuid(), dto.getHakukohdeOid(), tulos));
                }
            }).join();
        }, lahetaLaskeDTOExecutor);
    }

    private CompletableFuture<String> lahetaSuoritustiedot(SuoritustiedotDTO suoritustiedot, Laskentakutsu laskentakutsu) {
        final String url = getUrl("valintalaskenta-laskenta-service.valintalaskenta.lisaa.suoritustiedot.laskentakutsuun", laskentakutsu.getPollKey());
        return httpclient.post(
            url,
            Duration.ofMinutes(10),
            HttpRequest.BodyPublishers.ofString(Laskentakutsu.toBase64Gzip(suoritustiedot), StandardCharsets.UTF_8),
            builder -> builder
                .header("Content-Type", "text/plain")
                .header("Accept", "text/plain"),
            httpclient::parseTxt).whenComplete((tulos, poikkeus) -> {
            if (poikkeus != null) {
                LOG.error(String.format("Laskenta %s : Virhe lähetettäessä suoritustietojen lisäämistä kutsuun valintalaskennalle osoitteeseen '%s'",
                    laskentakutsu.getUuid(), url), poikkeus);
            } else {
                LOG.info(String.format("Laskenta %s : Saatiin valintalaskennalta suoritustietojen lisäämiseen vastaus '%s'",
                    laskentakutsu.getUuid(), tulos));
            }
        });
    }

    private CompletableFuture<String> kaynnistaPaloissaSiirrettyLaskenta(Laskentakutsu laskentakutsu) {
        final String url = getUrl("valintalaskenta-laskenta-service.valintalaskenta.kaynnista.paloissa.aloitettu.laskenta", laskentakutsu.getPollKey());
        return httpclient.post(
            url,
            Duration.ofMinutes(10),
            httpclient.createJsonBodyPublisher(laskentakutsu, Laskentakutsu.class),
            builder -> builder
                .header("Content-Type", "application/json")
                .header("Accept", "text/plain"),
            httpclient::parseTxt).whenComplete((tulos, poikkeus) -> {
            if (poikkeus != null) {
                LOG.error(String.format("Laskenta %s : Virhe lähetettäessä paloissa siirretyn laskentakutsun aloitusta valintalaskennalle osoitteeseen '%s'",
                    laskentakutsu.getUuid(), url), poikkeus);
            } else {
                LOG.info(String.format("Laskenta %s : Saatiin valintalaskennalta paloissa siirretyn laskennan aloitukseen vastaus '%s'", laskentakutsu.getUuid(), tulos));
            }
        });
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

        return kutsuRajapintaaPollaten(laskentakutsu, requestFuture);
    }

    private Observable<String> kutsuRajapintaaPollaten(Laskentakutsu laskentakutsu, CompletableFuture<String> requestFuture) {
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
