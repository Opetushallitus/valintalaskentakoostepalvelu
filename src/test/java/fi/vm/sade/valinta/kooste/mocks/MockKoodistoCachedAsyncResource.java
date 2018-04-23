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
            case KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1: {
                Koodi maakoodi = new Koodi();
                maakoodi.setMetadata(Arrays.asList(createMetadata("Suomi", "FI"), createMetadata("Finland", "SV"), createMetadata("Finland", "EN")));
                maakoodi.setKoodiArvo("FIN");
                return ImmutableMap.of( "FIN", maakoodi);
            }
            case KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_2: {
                Koodi maakoodi2 = new Koodi();
                maakoodi2.setMetadata(Arrays.asList(createMetadata("Suomi", "FI"), createMetadata("Finland", "SV"), createMetadata("Finland", "EN")));
                maakoodi2.setKoodiArvo("246");
                return ImmutableMap.of( "246", maakoodi2);
            }
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
            case KoodistoCachedAsyncResource.HYVAKSYNNAN_EHDOT: {
                Koodi ehdollisenHyvaksynnanKoodi = new Koodi();
                ehdollisenHyvaksynnanKoodi.setMetadata(Arrays.asList(
                        createMetadata("Ehdollinen: lopullinen tutkintotodistus toimitettava määräaikaan mennessä", "FI"),
                        createMetadata("Villkor: lämna in ditt slutliga examensbetyg inom utsatt tid", "SV"),
                        createMetadata("Finland", "Condition: Submit your final qualification certificate by the deadline")));
                ehdollisenHyvaksynnanKoodi.setKoodiArvo("ltt");
                return ImmutableMap.of( "ltt", ehdollisenHyvaksynnanKoodi);
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
