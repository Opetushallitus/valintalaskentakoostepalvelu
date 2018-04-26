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
                Koodi ehdollisenHyvaksynnanKoodi1 = new Koodi();
                ehdollisenHyvaksynnanKoodi1.setMetadata(Arrays.asList(
                        createMetadata("Ehdollinen: lopullinen tutkintotodistus toimitettava määräaikaan mennessä", "FI"),
                        createMetadata("Villkor: lämna in ditt slutliga examensbetyg inom utsatt tid", "SV"),
                        createMetadata("Condition: Submit your final qualification certificate by the deadline", "EN")));
                ehdollisenHyvaksynnanKoodi1.setKoodiArvo("ltt");

                Koodi ehdollisenHyvaksynnanKoodi2 = new Koodi();
                ehdollisenHyvaksynnanKoodi2.setMetadata(Arrays.asList(
                        createMetadata("Muu", "FI"),
                        createMetadata("Annan", "SV"),
                        createMetadata("Other", "EN")));
                ehdollisenHyvaksynnanKoodi2.setKoodiArvo("muu");

                Koodi ehdollisenHyvaksynnanKoodi3 = new Koodi();
                ehdollisenHyvaksynnanKoodi3.setMetadata(Arrays.asList(
                        createMetadata("Ehdollinen: lukuvuosimaksu maksettava määräaikaan mennessä, ennen kuin voit ilmoittautua", "FI"),
                        createMetadata("Villkor: betala läsårsavgiften inom utsatt tid för att du ska kunna anmäla dig", "SV"),
                        createMetadata("Condition: You have to pay the tuition fee by the deadline before you can enroll as a student", "EN")));
                ehdollisenHyvaksynnanKoodi3.setKoodiArvo("lvm");

                Koodi ehdollisenHyvaksynnanKoodi4 = new Koodi();
                ehdollisenHyvaksynnanKoodi4.setMetadata(Arrays.asList(
                        createMetadata("Ehdollinen: tutkintotodistuskopio hakuperusteena olleesta tutkinnosta toimitettava määräaikaan mennessä", "FI"),
                        createMetadata("Villkor: lämna in kopia av examensbetyget för den examen som du använt som ansökningsgrund inom utsatt", "SV"),
                        createMetadata("Condition: Submit a copy of the qualification certificate of the qualification you used to prove your eligibility by the deadline", "EN")));
                ehdollisenHyvaksynnanKoodi4.setKoodiArvo("ttk");

                return ImmutableMap.of( "ltt", ehdollisenHyvaksynnanKoodi1,
                        "muu", ehdollisenHyvaksynnanKoodi2,
                        "lvn", ehdollisenHyvaksynnanKoodi3,
                        "ttk", ehdollisenHyvaksynnanKoodi4);
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
