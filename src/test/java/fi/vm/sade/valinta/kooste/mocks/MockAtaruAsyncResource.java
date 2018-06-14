package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.util.AtaruHakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.Collections;
import java.util.List;

@Service
public class MockAtaruAsyncResource implements AtaruAsyncResource {

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByHakukohde(String hakukohdeOid) {
        return Observable.just(Collections.singletonList(getAtaruHakemus("1.2.246.562.11.00000000000000000063")));
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByOids(List<String> oids) {
        return Observable.just(Collections.singletonList(getAtaruHakemus("1.2.246.562.11.00000000000000000063")));
    }

    public static HakemusWrapper getAtaruHakemus(String s) {
        HenkiloPerustietoDto henkilo = new HenkiloPerustietoDto();
        henkilo.setOidHenkilo("Henkilo1");
        henkilo.setHetu("Hetu1");
        try {
            List<AtaruHakemus> hakemukset = new Gson().fromJson(IOUtils
                    .toString(new ClassPathResource("ataruhakemukset.json")
                            .getInputStream()), new TypeToken<List<AtaruHakemus>>() {}.getType());

            return hakemukset.stream()
                    .map(h -> new AtaruHakemusWrapper(h, henkilo))
                    .filter(h -> s.equals(h.getOid()))
                    .distinct().iterator().next();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't fetch mock ataru application", e);
        }
    }
}
