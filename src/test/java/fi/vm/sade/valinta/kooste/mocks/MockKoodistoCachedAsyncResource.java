package fi.vm.sade.valinta.kooste.mocks;


import com.google.common.collect.ImmutableMap;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

@Service
public class MockKoodistoCachedAsyncResource extends KoodistoCachedAsyncResource {

    @Autowired
    public MockKoodistoCachedAsyncResource(KoodistoAsyncResource koodistoAsyncResource) {
        super(koodistoAsyncResource);
    }

    public Map<String, Koodi> haeKoodisto(String koodistoUri) {
        switch (koodistoUri) {
            case KoodistoCachedAsyncResource.KIELI:
                return ImmutableMap.of(
                    "FI", new Koodi(),
                    "99", new Koodi(),
                    "SV", new Koodi(),
                    "NO", new Koodi());
            case KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1:
                return ImmutableMap.of( "FIN", new Koodi());
            case KoodistoCachedAsyncResource.KUNTA: {
                Koodi kuntaKoodi = new Koodi();
                kuntaKoodi.setMetadata(Arrays.asList(createMetadata("Helsinki", "FI"), createMetadata("Helsingfors", "SV")));
                kuntaKoodi.setKoodiArvo("091");

                return ImmutableMap.of(
                        "091", kuntaKoodi);
            }
            case KoodistoCachedAsyncResource.POSTI: {
                Koodi postinumeroKoodi = new Koodi();
                postinumeroKoodi.setMetadata(Arrays.asList(createMetadata("HELSINKI", "FI"), createMetadata("HELSINGFORS", "SV")));
                postinumeroKoodi.setKoodiArvo("00100");

                return ImmutableMap.of(
                        "00100", postinumeroKoodi);
            }
            default: assert false; return new HashMap<>();
        }
    }

    private Metadata createMetadata(String nimi, String kieli) {
        Metadata metadata = new Metadata();
        metadata.setKieli(kieli);
        metadata.setNimi(nimi);
        return metadata;
    }
}
