package fi.vm.sade.valinta.kooste.mocks;


import com.google.common.collect.ImmutableMap;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MockKoodistoCachedAsyncResource extends KoodistoCachedAsyncResource {

    @Autowired
    public MockKoodistoCachedAsyncResource(KoodistoAsyncResource koodistoAsyncResource) {
        super(koodistoAsyncResource);
    }

    public Map<String, Koodi> haeKoodisto(String koodistoUri) {
        return ImmutableMap.of(
                "FI", new Koodi(),
                "99", new Koodi(),
                "SV", new Koodi(),
                "NO", new Koodi());
    }
}
