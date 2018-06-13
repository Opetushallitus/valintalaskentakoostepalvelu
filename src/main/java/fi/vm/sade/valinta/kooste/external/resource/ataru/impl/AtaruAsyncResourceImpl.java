package fi.vm.sade.valinta.kooste.external.resource.ataru.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.util.AtaruHakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AtaruAsyncResourceImpl extends UrlConfiguredResource implements AtaruAsyncResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource;

    @Autowired
    public AtaruAsyncResourceImpl(
            @Qualifier("AtaruRestClientAsAdminCasInterceptor") AbstractPhaseInterceptor casInterceptor,
            OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource1) {
        super(TimeUnit.HOURS.toMillis(1), casInterceptor);
        this.oppijanumerorekisteriAsyncResource = oppijanumerorekisteriAsyncResource1;
    }

    private Observable<List<HakemusWrapper>> getApplications(String hakukohdeOid, List<String> hakemusOids) {
        return this.<String, List<AtaruHakemus>>postAsObservableLazily(
                getUrl("ataru.applications.by-hakukohde"),
                new TypeToken<List<AtaruHakemus>>() {}.getType(),
                Entity.entity(gson().toJson(hakemusOids), MediaType.APPLICATION_JSON),
                client -> {
                    if (hakukohdeOid != null) {
                        client.query("hakukohdeOid", hakukohdeOid);
                    }
                    LOG.info("Calling url {}", client.getCurrentURI());
                    return client;
                }).flatMap(hakemukset -> {
                    List<String> personOids = hakemukset.stream().map(AtaruHakemus::getPersonOid).distinct().collect(Collectors.toList());
                    return oppijanumerorekisteriAsyncResource.haeHenkilot(Lists.newArrayList(personOids))
                            .map(persons -> {
                                Map<String,HenkiloPerustietoDto> henkilotByOid = persons.stream().collect(Collectors.toMap(HenkiloPerustietoDto::getOidHenkilo, p -> p));
                                return hakemukset.stream()
                                        .map(hakemus -> new AtaruHakemusWrapper(hakemus, henkilotByOid.get(hakemus.getPersonOid())))
                                        .collect(Collectors.toList());
                            });
        });
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByHakukohde(String hakukohdeOid) {
        return getApplications(hakukohdeOid, Lists.newArrayList());
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByOids(List<String> oids) {
        return getApplications(null, oids);
    }
}