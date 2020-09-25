package fi.vm.sade.valinta.kooste.test.komponentti;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeValintaperusteetV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakukohdeImportKomponentti;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Component;

/** User: wuoti Date: 20.11.2013 Time: 12.59 */
@Component("suoritaHakukohdeImportKomponenttiTest")
public class SuoritaHakukohdeImportKomponenttiTest {

  private static final String HAKUKOHDE_VALINTAPERUSTEET_JSON =
      "hakukohdeimport/data/hakukohdevalintaperusteet.json";

  private SuoritaHakukohdeImportKomponentti suoritaHakukohdeImportKomponentti;

  private TarjontaAsyncResource tarjontaAsyncResource;
  private OrganisaatioAsyncResource organisaatioAsyncResource;
  private KoodistoCachedAsyncResource koodistoCachedAsyncResource;

  @Before
  public void setUp() {
    tarjontaAsyncResource = mock(TarjontaAsyncResource.class);
    organisaatioAsyncResource = mock(OrganisaatioAsyncResource.class);
    koodistoCachedAsyncResource = mock(KoodistoCachedAsyncResource.class);
    suoritaHakukohdeImportKomponentti =
        new SuoritaHakukohdeImportKomponentti(
            tarjontaAsyncResource, organisaatioAsyncResource, koodistoCachedAsyncResource);
  }

  @Test
  public void test() throws IOException {
    final String hakukohdeOid = "1.2.246.562.5.50425195448";

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    HakukohdeValintaperusteetV1RDTO valintaperusteet =
        mapper.readValue(
            Resources.toString(
                Resources.getResource(HAKUKOHDE_VALINTAPERUSTEET_JSON), Charsets.UTF_8),
            HakukohdeValintaperusteetV1RDTO.class);

    // when(hakukohdeResourceMock.getHakukohdeNimi(hakukohdeOid)).thenReturn(nimi);
    // when(hakukohdeResourceMock.getByOID(hakukohdeOid)).thenReturn(hakukohde);
    when(tarjontaAsyncResource.findValintaperusteetByOid(hakukohdeOid))
        .thenReturn(CompletableFuture.completedFuture(valintaperusteet));

    // ArgumentCaptor<HakukohdeImportTyyppi> captor =
    // ArgumentCaptor.forClass(HakukohdeImportTyyppi.class);
    suoritaHakukohdeImportKomponentti.suoritaHakukohdeImport(hakukohdeOid);
    // verify(valintaperusteServiceMock,
    // times(1)).tuoHakukohde(captor.capture());
  }
}
