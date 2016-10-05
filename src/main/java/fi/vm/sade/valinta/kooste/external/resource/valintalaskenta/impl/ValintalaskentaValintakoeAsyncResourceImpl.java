package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ValintalaskentaValintakoeAsyncResourceImpl extends HttpResource implements ValintalaskentaValintakoeAsyncResource {
    @Autowired
    public ValintalaskentaValintakoeAsyncResourceImpl(
            @Value("${valintalaskentakoostepalvelu.valintalaskenta.rest.url}") String address) {
        super(address, TimeUnit.HOURS.toMillis(1));
    }

    @Override
    public Observable<List<ValintakoeOsallistuminenDTO>> haeHakutoiveelle(String hakukohdeOid) {
        return getAsObservable("/valintalaskentakoostepalvelu/valintakoe/hakutoive/" + hakukohdeOid, new GenericType<List<ValintakoeOsallistuminenDTO>>() {}.getType());
    }

    @Override
    public Observable<List<ValintakoeOsallistuminenDTO>> haeHakutoiveille(Collection<String> hakukohdeOids) {
        return postAsObservable("/valintalaskentakoostepalvelu/valintakoe/hakutoive", new GenericType<List<ValintakoeOsallistuminenDTO>>() {}.getType(),
                Entity.entity(Lists.newArrayList(hakukohdeOids), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                });
    }

    @Override
    public Observable<List<HakemusOsallistuminenDTO>> haeValintatiedotHakukohteelle(String hakukohdeOid, List<String> valintakoeTunnisteet) {
        return postAsObservable("/valintalaskentakoostepalvelu/valintatieto/hakukohde/" + hakukohdeOid, new TypeToken<List<HakemusOsallistuminenDTO>>() {
        }.getType(), Entity.entity(valintakoeTunnisteet, MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public Observable<List<ValintakoeOsallistuminenDTO>> haeAmmatillisenKielikokeenOsallistumiset(Date since) {
        return getAsObservable("/valintalaskentakoostepalvelu/valintakoe/ammatillisenkielikoeosallistumiset/" + new SimpleDateFormat("yyyy-MM-dd").format(since),
            new GenericType<List<ValintakoeOsallistuminenDTO>>() {}.getType());
    }
}
