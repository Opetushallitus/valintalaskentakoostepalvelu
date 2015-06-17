package fi.vm.sade.valinta.kooste.external.resource.seuranta.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.DokumentinSeurantaAsyncResource;
import fi.vm.sade.valinta.seuranta.dto.DokumenttiDto;
import fi.vm.sade.valinta.seuranta.dto.VirheilmoitusDto;
import rx.Observable;

@Service
public class DokumentinSeurantaAsyncResourceImpl extends HttpResource implements DokumentinSeurantaAsyncResource {

    @Autowired
    public DokumentinSeurantaAsyncResourceImpl(@Value("${host.ilb}") String address) {
        super(address, TimeUnit.HOURS.toMillis(1));
    }

    public Observable<DokumenttiDto> paivitaDokumenttiId(String uuid, String dokumenttiId) {
        return postAsObservable("/seuranta-service/resources/dokumentinseuranta/" + uuid + "/paivita_dokumenttiId", DokumenttiDto.class, Entity.entity(dokumenttiId, MediaType.TEXT_PLAIN));
    }

    public Observable<String> luoDokumentti(String kuvaus) {
        return postAsObservable("/seuranta-service/resources/dokumentinseuranta/", String.class, Entity.entity(kuvaus, MediaType.TEXT_PLAIN));
    }

    public Observable<DokumenttiDto> paivitaKuvaus(String uuid, String kuvaus) {
        return postAsObservable("/seuranta-service/resources/dokumentinseuranta/" + uuid + "/paivita_kuvaus", DokumenttiDto.class, Entity.entity(kuvaus, MediaType.TEXT_PLAIN));
    }

    public Observable<DokumenttiDto> lisaaVirheilmoituksia(String uuid, List<VirheilmoitusDto> virheilmoitukset) {
        return postAsObservable("/seuranta-service/resources/dokumentinseuranta/" + uuid + "/lisaa_virheita", DokumenttiDto.class, Entity.json(virheilmoitukset));
    }
}

