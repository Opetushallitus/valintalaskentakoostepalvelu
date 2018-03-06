package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import rx.Observable;

import java.util.Collections;
import java.util.List;

public class MockAtaruAsyncResource implements AtaruAsyncResource {


    @Override
    public Observable<List<AtaruHakemus>> getApplicationsByHakukohde(String hakukohdeOid) {
        return Observable.from(Futures.immediateFuture(Collections.singletonList(getAtaruHakemus())));
    }

    public static AtaruHakemus getAtaruHakemus() {
        try {
            List<AtaruHakemus> hakemukset = new Gson().fromJson(IOUtils
                    .toString(new ClassPathResource("ataruhakemukset.json")
                            .getInputStream()), new TypeToken<List<AtaruHakemus>>() {}.getType());

            return hakemukset.stream()
                    .filter(h -> "1.2.246.562.11.00000000000000000063".equals(h.getHakemusOid()))
                    .distinct().iterator().next();
        } catch (Exception e) {
            System.err.println("Couldn't fetch mock ataru application");
            return new AtaruHakemus();
        }

    }
}
