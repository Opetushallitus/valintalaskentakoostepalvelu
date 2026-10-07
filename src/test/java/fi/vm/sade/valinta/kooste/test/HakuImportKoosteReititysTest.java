package fi.vm.sade.valinta.kooste.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.hakuimport.resource.HakuImportResource;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** User: wuoti Date: 20.5.2013 Time: 13.27 */
@Profile("hakuimport")
@Disabled
@Configuration
@ContextConfiguration(classes = HakuImportKoosteReititysTest.class)
@PropertySource("classpath:test.properties")
@ImportResource({"classpath:META-INF/spring/context/hakuimport-context.xml", "test-context.xml"})
@ActiveProfiles("hakuimport")
@ExtendWith(SpringExtension.class)
public class HakuImportKoosteReititysTest {

  private static final String HAKU_OID = "hakuoid1";

  private static final String[][] HAKUKOHDE_URIS_AND_OIDS = {
    {"hakukohdekoodiuri1", "hakukohdeoid1"},
    {"hakukohdekoodiuri2", "hakukohdeoid2"},
    {"hakukohdekoodiuri3", "hakukohdeoid3"},
    {"hakukohdekoodiuri4", "hakukohdeoid4"},
    {"hakukohdekoodiuri5", "hakukohdeoid5"},
    {"hakukohdekoodiuri6", "hakukohdeoid6s"}
  };

  @Autowired private HakuImportResource hakuImportAktivointiResource;

  @Bean
  public ValintaperusteetAsyncResource getValintaperusteServiceMock() {
    return mock(ValintaperusteetAsyncResource.class);
  }

  @Autowired private ValintaperusteetAsyncResource valintaperusteService;

  @Test
  public void testImportHaku() {
    HttpServletRequest requestMock = mock(HttpServletRequest.class);
    hakuImportAktivointiResource.aktivoiHakuImport(HAKU_OID, requestMock);
    ArgumentCaptor<HakukohdeImportDTO> argCaptor =
        ArgumentCaptor.forClass(HakukohdeImportDTO.class);
    verify(valintaperusteService, times(HAKUKOHDE_URIS_AND_OIDS.length))
        .tuoHakukohde(argCaptor.capture());

    // Tsekataan, että valintaperusteserviceä kutsuttiin kaikille hakukohde oideille
    outer:
    for (String[] uriAndOid : HAKUKOHDE_URIS_AND_OIDS) {
      for (HakukohdeImportDTO t : argCaptor.getAllValues()) {
        if (uriAndOid[1].equals(t.getHakukohdeOid())) {
          assertEquals(HAKU_OID, t.getHakuOid());
          assertEquals(uriAndOid[0], t.getHakukohdekoodi().getKoodiUri());
          assertEquals(uriAndOid[0] + "-arvo", t.getHakukohdekoodi().getArvo());
          assertEquals(uriAndOid[0] + "-nimi", t.getHakukohdekoodi().getNimiFi());
          //   assertEquals(uriAndOid[0] + "-nimi", t.getNimi());

          continue outer;
        }
      }

      fail();
    }
  }
}
