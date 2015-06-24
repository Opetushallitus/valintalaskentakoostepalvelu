package fi.vm.sade.valinta.kooste.viestintapalvelu;

import com.google.common.collect.ImmutableList;
import fi.vm.sade.koodisto.service.GenericFault;
import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.SearchKoodisByKoodistoCriteriaType;
import fi.vm.sade.koodisto.service.types.SearchKoodisCriteriaType;
import fi.vm.sade.koodisto.service.types.common.*;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class PostitoimipaikkaMemoizeTest {

    @Test
    public void testPostitoimipaikka() throws Exception {
        PostitoimipaikkaMemoize postitoimipaikkaMemoize = new PostitoimipaikkaMemoize(koodiService);
        String helsinki = postitoimipaikkaMemoize.postitoimipaikka.apply(KieliType.FI).apply("posti_00100");
        assertEquals("Helsinki", helsinki);
    }

    @Test
    public void testPostitoimipaikkaWithMissingLang() throws Exception {
        PostitoimipaikkaMemoize postitoimipaikkaMemoize = new PostitoimipaikkaMemoize(koodiService);
        String helsinki = postitoimipaikkaMemoize.postitoimipaikka.apply(KieliType.EN).apply("posti_00100");
        assertEquals("Helsinki", helsinki);
    }

    private class MockKoodiType extends KoodiType {
        public MockKoodiType(String kuvausFi, String nimiFi, String kuvausSv, String nimiSv) {
            super();
            this.metadata = ImmutableList.of(createMetadata(kuvausFi, nimiFi), createMetadata(kuvausSv, nimiSv));
        }

        private KoodiMetadataType createMetadata(String kuvausFi, String nimiFi) {
            KoodiMetadataType fi = new KoodiMetadataType();
            fi.setKuvaus(kuvausFi);
            fi.setNimi(nimiFi);
            return fi;
        }
    }

    private KoodiService koodiService = new KoodiService() {
        @Override
        public List<KoodiType> listKoodiByRelation(KoodiUriAndVersioType koodiUriAndVersioType, boolean b, SuhteenTyyppiType suhteenTyyppiType) throws GenericFault {
            return null;
        }

        @Override
        public List<KoodiType> searchKoodisByKoodisto(SearchKoodisByKoodistoCriteriaType searchKoodisByKoodistoCriteriaType) throws GenericFault {
            return null;
        }

        @Override
        public List<KoodiType> searchKoodis(SearchKoodisCriteriaType searchKoodisCriteriaType) throws GenericFault {
            return ImmutableList.of(new MockKoodiType("HELSINKI", "HELSINKI", "HELSINGFORS", "HELSINFORS"));
        }
    };
}
