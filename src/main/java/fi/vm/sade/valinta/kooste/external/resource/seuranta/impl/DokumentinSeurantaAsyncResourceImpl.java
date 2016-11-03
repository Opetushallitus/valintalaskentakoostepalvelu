package fi.vm.sade.valinta.kooste.external.resource.seuranta.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import fi.vm.sade.valinta.kooste.external.resource.seuranta.DokumentinSeurantaAsyncResource;
import fi.vm.sade.valinta.seuranta.dto.DokumenttiDto;
import fi.vm.sade.valinta.seuranta.dto.VirheilmoitusDto;
import rx.Observable;

@Service
public class DokumentinSeurantaAsyncResourceImpl extends UrlConfiguredResource implements DokumentinSeurantaAsyncResource {

    @Autowired
    public DokumentinSeurantaAsyncResourceImpl(@Qualifier("SeurantaRestClientCasInterceptor") AbstractPhaseInterceptor casInterceptor,
                                               UrlConfiguration urlConfiguration) {
        super(urlConfiguration, TimeUnit.HOURS.toMillis(1), casInterceptor);
    }

    public Observable<DokumenttiDto> paivitaDokumenttiId(String uuid, String dokumenttiId) {
        return postAsObservable(getUrl("seuranta-service.dokumentinseuranta.paivitadokumenttiid", uuid), DokumenttiDto.class, Entity.entity(dokumenttiId, MediaType.TEXT_PLAIN));
    }

    public Observable<String> luoDokumentti(String kuvaus) {
        return postAsObservable(getUrl("seuranta-service.dokumentinseuranta"), String.class, Entity.entity(kuvaus, MediaType.TEXT_PLAIN));
    }

    public Observable<DokumenttiDto> paivitaKuvaus(String uuid, String kuvaus) {
        return postAsObservable(getUrl("seuranta-service.dokumentinseuranta.paivitakuvaus", uuid), DokumenttiDto.class, Entity.entity(kuvaus, MediaType.TEXT_PLAIN));
    }

    public Observable<DokumenttiDto> lisaaVirheilmoituksia(String uuid, List<VirheilmoitusDto> virheilmoitukset) {
        return postAsObservable(getUrl("seuranta-service.dokumentinseuranta.lisaavirheita", uuid), DokumenttiDto.class, Entity.json(virheilmoitukset));
    }
}

