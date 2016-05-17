package fi.vm.sade.valinta.kooste.external.resource.seuranta.impl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.valinta.kooste.valintalaskenta.resource.LaskentaParams;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.http.ResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;


@Service
public class LaskentaSeurantaAsyncResourceImpl extends HttpResource implements LaskentaSeurantaAsyncResource {
    private final Gson gson = new Gson();
    private final ResponseCallback responseCallback = new ResponseCallback();

    @Autowired
    public LaskentaSeurantaAsyncResourceImpl(@Value("${host.ilb}") String address) {
        super(address, TimeUnit.HOURS.toMillis(1));
    }

    @Override
    public void otaSeuraavaLaskentaTyonAlle(Consumer<String> uuidCallback, Consumer<Throwable> failureCallback) {
        try {
            String url = "/seuranta-service/resources/seuranta/laskenta/otaSeuraavaLaskentaTyonAlle";
            LOG.info("Haetaan seuraava laskenta tyon alle");
            getWebClient()
                    .path(url)
                    .async()
                    .get(new GsonResponseCallback<>(gson(), address, url, uuidCallback, failureCallback, String.class));
        } catch (Exception e) {
            LOG.error("Uuden tyon hakeminen epaonnistui", e);
            failureCallback.accept(e);
        }
    }

    public void laskenta(String uuid, Consumer<LaskentaDto> callback, Consumer<Throwable> failureCallback) {
        try {
            String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/" + uuid;
            getWebClient()
                    .path(url)
                    .async()
                    .get(new GsonResponseCallback<>(gson(), address, url, callback, failureCallback, LaskentaDto.class));
        } catch (Exception e) {
            failureCallback.accept(e);
        }
    }

    public void resetoiTilat(String uuid, Consumer<LaskentaDto> callback, Consumer<Throwable> failureCallback) {
        try {
            String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/" + uuid + "/resetoi";
            getWebClient()
                    .path(url)
                    .async()
                    .put(Entity.entity(uuid, MediaType.APPLICATION_JSON_TYPE), new GsonResponseCallback<>(gson(), address, url, callback, failureCallback, LaskentaDto.class));
        } catch (Exception e) {
            failureCallback.accept(e);
        }
    }

    public void luoLaskenta(LaskentaParams laskentaParams, List<HakukohdeDto> hakukohdeOids, Consumer<String> callback, Consumer<Throwable> failureCallback) {
        try {
            String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/" + laskentaParams.getHakuOid() + "/tyyppi/" + laskentaParams.getLaskentatyyppi();
            WebClient wc = getWebClient().path(url);
            wc.query("erillishaku", (Boolean) laskentaParams.isErillishaku());
            if (laskentaParams.getValinnanvaihe() != null) {
                wc.query("valinnanvaihe", laskentaParams.getValinnanvaihe());
            }
            if (laskentaParams.getIsValintakoelaskenta() != null) {
                wc.query("valintakoelaskenta", laskentaParams.getIsValintakoelaskenta());
            }
            wc.async().post(Entity.entity(hakukohdeOids, MediaType.APPLICATION_JSON_TYPE), new GsonResponseCallback<>(gson(), address, url, callback, failureCallback, String.class));
        } catch (Exception e) {
            failureCallback.accept(e);
        }
    }

    public void merkkaaLaskennanTila(String uuid, LaskentaTila tila, Optional<IlmoitusDto> ilmoitusDtoOptional) {
        String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/" + uuid + "/tila/" + tila;
        try {
            if(ilmoitusDtoOptional.isPresent()) {
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
            LOG.error("Seurantapalvelun kutsu paatyi virheeseen!" + url, e);
        }
    }

    public void merkkaaLaskennanTila(String uuid, LaskentaTila tila, HakukohdeTila hakukohdetila, Optional<IlmoitusDto> ilmoitusDtoOptional) {
        String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/" + uuid + "/tila/" + tila + "/hakukohde/" + hakukohdetila;
        try {
        if(ilmoitusDtoOptional.isPresent()) {
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
        String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/" + uuid + "/hakukohde/" + hakukohdeOid + "/tila/" + tila;
        try {
            if(ilmoitusDtoOptional.isPresent()) {
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

    public void lisaaIlmoitusHakukohteelle(String uuid, String hakukohdeOid, IlmoitusDto ilmoitus) {
        String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/" + uuid + "/hakukohde/" + hakukohdeOid;
        try {
            getWebClient()
                    .path(url)
                    .async()
                    .post(Entity.entity(gson.toJson(ilmoitus), MediaType.APPLICATION_JSON_TYPE), responseCallback);
        } catch (Exception e) {
            LOG.error("Seurantapalvelun kutsu paatyi virheeseen " + url, e);
        }
    }

}
