package fi.vm.sade.valinta.kooste.external.resource.seuranta.impl;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.DokumentinSeurantaAsyncResource;
import fi.vm.sade.valinta.seuranta.dto.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class DokumentinSeurantaAsyncResourceImpl extends HttpResource implements DokumentinSeurantaAsyncResource {

    @Autowired
    public DokumentinSeurantaAsyncResourceImpl(@Value("${host.ilb}") String address) {
        super(address, TimeUnit.HOURS.toMillis(1));
    }

    public void paivitaDokumenttiId(String uuid, String dokumenttiId, Consumer<DokumenttiDto> callback, Consumer<Throwable> failureCallback) {
        String url = "/seuranta-service/resources/dokumentinseuranta/" + uuid + "/paivita_dokumenttiId";
        try {
            getWebClient()
                    .path(url)
                    .async()
                    .post(Entity.entity(dokumenttiId, MediaType.TEXT_PLAIN),
                            new GsonResponseCallback<DokumenttiDto>(address, url, callback, failureCallback, new TypeToken<DokumenttiDto>() {
                            }.getType()));
        } catch (Exception e) {
            LOG.error("Seurantapalvelun kutsu {} paatyi virheeseen: {}", url, e.getMessage());
        }
    }

    public void luoDokumentti(String kuvaus, Consumer<String> callback, Consumer<Throwable> failureCallback) {
        String url = "/seuranta-service/resources/dokumentinseuranta/";
        try {
            getWebClient()
                    .path(url)
                    .async()
                    .post(Entity.entity(kuvaus, MediaType.TEXT_PLAIN),
                            new GsonResponseCallback<String>(address, url, callback, failureCallback, new TypeToken<String>() {
                            }.getType()));
        } catch (Exception e) {
            LOG.error("Seurantapalvelun kutsu {} paatyi virheeseen: {}", url, e.getMessage());
        }
    }

    public void paivitaKuvaus(String uuid, String kuvaus, Consumer<DokumenttiDto> callback, Consumer<Throwable> failureCallback) {
        String url = "/seuranta-service/resources/dokumentinseuranta/" + uuid + "/paivita_kuvaus";
        try {
            getWebClient()
                    .path(url)
                    .async()
                    .post(Entity.entity(kuvaus, MediaType.TEXT_PLAIN),
                            new GsonResponseCallback<DokumenttiDto>(address, url, callback, failureCallback, new TypeToken<DokumenttiDto>() {
                            }.getType()));
        } catch (Exception e) {
            LOG.error("Seurantapalvelun kutsu {} paatyi virheeseen: {}", url, e.getMessage());
        }
    }

    public void lisaaVirheilmoituksia(String uuid, List<VirheilmoitusDto> virheilmoitukset, Consumer<DokumenttiDto> callback, Consumer<Throwable> failureCallback) {
        String url = "/seuranta-service/resources/dokumentinseuranta/" + uuid + "/lisaa_virheita";
        try {
            getWebClient()
                    .path(url)
                    .async()
                    .post(Entity.json(virheilmoitukset),
                            new GsonResponseCallback<DokumenttiDto>(address, url, callback, failureCallback, new TypeToken<DokumenttiDto>() {
                            }.getType()));
        } catch (Exception e) {
            LOG.error("Seurantapalvelun kutsu {} paatyi virheeseen: {}", url, e.getMessage());
        }
    }
}

