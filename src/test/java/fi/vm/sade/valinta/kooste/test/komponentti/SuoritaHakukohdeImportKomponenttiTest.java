package fi.vm.sade.valinta.kooste.test.komponentti;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.tarjonta.service.resources.v1.HakukohdeV1ResourceWrapper;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeValintaperusteetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.KoodistoUrheilija;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakukohdeImportKomponentti;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Component;

/** User: wuoti Date: 20.11.2013 Time: 12.59 */
@Component("suoritaHakukohdeImportKomponenttiTest")
public class SuoritaHakukohdeImportKomponenttiTest {

  private static final String HAKUKOHDE_JSON = "hakukohdeimport/data/hakukohde.json";
  private static final String HAKUKOHDE_NIMI_JSON = "hakukohdeimport/data/hakukohdenimi.json";
  private static final String HAKUKOHDE_VALINTAPERUSTEET_JSON =
      "hakukohdeimport/data/hakukohdevalintaperusteet.json";
  private static final String KOODISTO_ALAKOODIT_JSON =
      "hakukohdeimport/data/koodistoalakoodit.json";

  private SuoritaHakukohdeImportKomponentti suoritaHakukohdeImportKomponentti;

  private HakukohdeV1ResourceWrapper hakukohdeResourceMock;

  @Before
  public void setUp() {
    hakukohdeResourceMock = mock(HakukohdeV1ResourceWrapper.class);
    suoritaHakukohdeImportKomponentti =
        new SuoritaHakukohdeImportKomponentti(hakukohdeResourceMock);
  }

  @Test
  public void test() throws IOException {
    final String hakukohdeOid = "1.2.246.562.5.50425195448";
    final String hakukohdeNimirUri = "hakukohteet_535";

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    HakukohdeNimiRDTO nimi =
        mapper.readValue(
            Resources.toString(Resources.getResource(HAKUKOHDE_NIMI_JSON), Charsets.UTF_8),
            HakukohdeNimiRDTO.class);
    HakukohdeDTO hakukohde =
        mapper.readValue(
            Resources.toString(Resources.getResource(HAKUKOHDE_JSON), Charsets.UTF_8),
            HakukohdeDTO.class);
    HakukohdeValintaperusteetV1RDTO valintaperusteet =
        mapper.readValue(
            Resources.toString(
                Resources.getResource(HAKUKOHDE_VALINTAPERUSTEET_JSON), Charsets.UTF_8),
            HakukohdeValintaperusteetV1RDTO.class);
    ResultV1RDTO<HakukohdeValintaperusteetV1RDTO> result = new ResultV1RDTO<>();
    result.setResult(valintaperusteet);
    List<KoodistoUrheilija> alakoodit =
        mapper.readValue(
            Resources.toString(Resources.getResource(KOODISTO_ALAKOODIT_JSON), Charsets.UTF_8),
            TypeFactory.defaultInstance()
                .constructCollectionType(List.class, KoodistoUrheilija.class));

    // when(hakukohdeResourceMock.getHakukohdeNimi(hakukohdeOid)).thenReturn(nimi);
    // when(hakukohdeResourceMock.getByOID(hakukohdeOid)).thenReturn(hakukohde);
    when(hakukohdeResourceMock.findValintaperusteetByOid(hakukohdeOid)).thenReturn(result);

    // ArgumentCaptor<HakukohdeImportTyyppi> captor =
    // ArgumentCaptor.forClass(HakukohdeImportTyyppi.class);
    suoritaHakukohdeImportKomponentti.suoritaHakukohdeImport(hakukohdeOid);
    // verify(valintaperusteServiceMock,
    // times(1)).tuoHakukohde(captor.capture());
  }
}