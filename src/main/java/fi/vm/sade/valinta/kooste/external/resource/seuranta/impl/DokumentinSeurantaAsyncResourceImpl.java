package fi.vm.sade.valinta.kooste.external.resource.seuranta.impl;

import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.DokumentinSeurantaAsyncResource;
import fi.vm.sade.valinta.seuranta.dto.DokumenttiDto;
import fi.vm.sade.valinta.seuranta.dto.VirheilmoitusDto;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DokumentinSeurantaAsyncResourceImpl extends UrlConfiguredResource implements DokumentinSeurantaAsyncResource {

    @Autowired
    public DokumentinSeurantaAsyncResourceImpl(@Qualifier("SeurantaRestClientCasInterceptor") AbstractPhaseInterceptor casInterceptor) {
        super(TimeUnit.HOURS.toMillis(1), casInterceptor);
    }

    public Observable<DokumenttiDto> paivitaDokumenttiId(String uuid, String dokumenttiId) {
        return postAsObservableLazily(getUrl("seuranta-service.dokumentinseuranta.paivitadokumenttiid", uuid), DokumenttiDto.class, Entity.entity(dokumenttiId, MediaType.TEXT_PLAIN));
    }

    public Observable<String> luoDokumentti(String kuvaus) {
        return postAsObservableLazily(getUrl("seuranta-service.dokumentinseuranta"), String.class, Entity.entity(kuvaus, MediaType.TEXT_PLAIN));
    }

    public Observable<DokumenttiDto> paivitaKuvaus(String uuid, String kuvaus) {
        return postAsObservableLazily(getUrl("seuranta-service.dokumentinseuranta.paivitakuvaus", uuid), DokumenttiDto.class, Entity.entity(kuvaus, MediaType.TEXT_PLAIN));
    }

    public Observable<DokumenttiDto> lisaaVirheilmoituksia(String uuid, List<VirheilmoitusDto> virheilmoitukset) {
        return postAsObservableLazily(getUrl("seuranta-service.dokumentinseuranta.lisaavirheita", uuid), DokumenttiDto.class, Entity.json(virheilmoitukset));
    }
}

