package fi.vm.sade.valinta.kooste.external.resource.koodisto.impl;

import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.GenericType;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class KoodistoAsyncResourceImpl extends UrlConfiguredResource implements KoodistoAsyncResource {

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

    @Override
    public Observable<Koodi> haeRinnasteinenKoodi(String koodiUri) {
        return this.<List<Koodi>>getAsObservableLazily(
                getUrl("koodisto-service.json.koodi.rinnasteinen", koodiUri),
                new GenericType<List<Koodi>>() {}.getType(),
                ACCEPT_JSON.andThen(client -> client.query("koodiVersio", 1)))
                .map(koodit -> koodit
                        .stream()
                        .filter(k -> k.getKoodistoUri().equals("maatjavaltiot1"))
                        .findFirst()
                        .orElseThrow(IllegalArgumentException::new));
    }
}

