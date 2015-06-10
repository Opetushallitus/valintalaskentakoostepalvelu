package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.AsyncResourceWithCas;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import rx.Observable;

@Service
public class ValintalaskentaValintakoeAsyncResourceImpl extends AsyncResourceWithCas implements ValintalaskentaValintakoeAsyncResource {
    @Autowired
    public ValintalaskentaValintakoeAsyncResourceImpl(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.valintalaskenta-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword,
            @Value("${valintalaskentakoostepalvelu.valintalaskenta.rest.url}") String address, ApplicationContext context) {
        super(webCasUrl, targetService, appClientUsername, appClientPassword, address, context, TimeUnit.HOURS.toMillis(1));
    }

    @Override
    public Observable<List<ValintakoeOsallistuminenDTO>> haeHakutoiveelle(String hakukohdeOid) {
        return getAsObservable("/valintakoe/hakutoive/" + hakukohdeOid, new GenericType<List<ValintakoeOsallistuminenDTO>>() {}.getType());
    }

    @Override
    public Observable<List<HakemusOsallistuminenDTO>> haeValintatiedotHakukohteelle(String hakukohdeOid, List<String> valintakoeTunnisteet) {
        return postAsObservable("/valintatieto/hakukohde/" + hakukohdeOid, new TypeToken<List<HakemusOsallistuminenDTO>>() {
        }.getType(), Entity.entity(valintakoeTunnisteet, MediaType.APPLICATION_JSON_TYPE));
    }
}
