package fi.vm.sade.valinta.kooste.test.komponentti;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakukohdeImportKomponentti;

/**
 * User: wuoti Date: 20.11.2013 Time: 12.59
 */
public class SuoritaHakukohdeImportKomponenttiTest {

    private static final String HAKUKOHDE_JSON = "hakukohdeimport/data/hakukohde.json";
    private static final String HAKUKOHDE_NIMI_JSON = "hakukohdeimport/data/hakukohdenimi.json";
    private static final String HAKUKOHDE_VALINTAPERUSTEET_JSON = "hakukohdeimport/data/hakukohdevalintaperusteet.json";

    private SuoritaHakukohdeImportKomponentti suoritaHakukohdeImportKomponentti;

    private ValintaperusteService valintaperusteServiceMock;
    private HakukohdeResource hakukohdeResourceMock;

    @Before
    public void setUp() {
        suoritaHakukohdeImportKomponentti = new SuoritaHakukohdeImportKomponentti();

        hakukohdeResourceMock = mock(HakukohdeResource.class);
        valintaperusteServiceMock = mock(ValintaperusteService.class);

        // ReflectionTestUtils.setField(suoritaHakukohdeImportKomponentti,
        // "valintaperusteService",
        // valintaperusteServiceMock);
        ReflectionTestUtils.setField(suoritaHakukohdeImportKomponentti, "hakukohdeResource", hakukohdeResourceMock);
    }

    @Test
    public void test() throws IOException {
        final String hakukohdeOid = "1.2.246.562.5.50425195448";

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        HakukohdeNimiRDTO nimi = mapper
                .readValue(Resources.toString(Resources.getResource(HAKUKOHDE_NIMI_JSON), Charsets.UTF_8),
                        HakukohdeNimiRDTO.class);
        HakukohdeDTO hakukohde = mapper.readValue(
                Resources.toString(Resources.getResource(HAKUKOHDE_JSON), Charsets.UTF_8), HakukohdeDTO.class);
        HakukohdeValintaperusteetDTO valintaperusteet = mapper.readValue(
                Resources.toString(Resources.getResource(HAKUKOHDE_VALINTAPERUSTEET_JSON), Charsets.UTF_8),
                HakukohdeValintaperusteetDTO.class);

        when(hakukohdeResourceMock.getHakukohdeNimi(hakukohdeOid)).thenReturn(nimi);
        when(hakukohdeResourceMock.getByOID(hakukohdeOid)).thenReturn(hakukohde);
        when(hakukohdeResourceMock.getHakukohdeValintaperusteet(hakukohdeOid)).thenReturn(valintaperusteet);

        // ArgumentCaptor<HakukohdeImportTyyppi> captor =
        // ArgumentCaptor.forClass(HakukohdeImportTyyppi.class);
        suoritaHakukohdeImportKomponentti.suoritaHakukohdeImport(hakukohdeOid);
        // verify(valintaperusteServiceMock,
        // times(1)).tuoHakukohde(captor.capture());
    }

}
