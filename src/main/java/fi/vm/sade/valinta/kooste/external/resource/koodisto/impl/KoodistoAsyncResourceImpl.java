package fi.vm.sade.valinta.kooste.external.resource.koodisto.impl;

import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;

import javax.ws.rs.core.GenericType;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class KoodistoAsyncResourceImpl extends UrlConfiguredResource implements KoodistoAsyncResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    public KoodistoAsyncResourceImpl() {
        super(TimeUnit.HOURS.toMillis(20));
    }

    @Override
    public Observable<List<Koodi>> haeKoodisto(String koodistoUri) {
        return getAsObservableLazily(
            getUrl("koodisto-service.json.oid.koodi", koodistoUri),
            new GenericType<List<Koodi>>() {}.getType(),
            ACCEPT_JSON.andThen(client -> client.query("onlyValidKoodis", true)));
    }

    private Observable<Koodi> haeKoodi(String koodiUri) {
        return this.getAsObservableLazily(
                getUrl("koodisto-service.json.koodi", koodiUri),
                Koodi.class
        );
    }

    @Override
    public Observable<Koodi> maatjavaltiot2ToMaatjavaltiot1(String koodiUri) {
        return this.<List<Koodi>>getAsObservableLazily(
                getUrl("koodisto-service.json.koodi.rinnasteinen", koodiUri),
                new GenericType<List<Koodi>>() {}.getType(),
                ACCEPT_JSON.andThen(client -> {
                    client.query("koodiVersio", 1);
                    LOG.info("Calling url {}", client.getCurrentURI());
                    return client;
                }))
                .flatMap(koodit -> {
                    Optional<Koodi> koodi = koodit.stream()
                            .filter(k -> k.getKoodistoUri().equals("maatjavaltiot1"))
                            .findFirst();
                    if (koodi.isPresent()) {
                        return Observable.just(koodi.get());
                    } else {
                        LOG.warn(String.format("Could not find related maatjavaltiot1 koodi for %s, returning maatjavaltiot1_xxx instead", koodiUri));
                        return haeKoodi("maatjavaltiot1_xxx");
                    }
                });
    }
}

