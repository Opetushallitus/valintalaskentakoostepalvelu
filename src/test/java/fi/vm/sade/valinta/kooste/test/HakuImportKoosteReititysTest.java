package fi.vm.sade.valinta.kooste.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fi.vm.sade.koodisto.service.GenericFault;
import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.SearchKoodisByKoodistoCriteriaType;
import fi.vm.sade.koodisto.service.types.SearchKoodisCriteriaType;
import fi.vm.sade.koodisto.service.types.common.KieliType;
import fi.vm.sade.koodisto.service.types.common.KoodiMetadataType;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.koodisto.service.types.common.KoodiUriAndVersioType;
import fi.vm.sade.koodisto.service.types.common.SuhteenTyyppiType;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.tarjonta.service.types.TarjontaTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.hakuimport.resource.HakuImportResource;
import java.util.ArrayList;
import java.util.List;
import javax.jws.WebParam;
import javax.servlet.http.HttpServletRequest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/** User: wuoti Date: 20.5.2013 Time: 13.27 */
@Configuration
@ContextConfiguration(classes = HakuImportKoosteReititysTest.class)
@PropertySource("classpath:test.properties")
@ImportResource({ "classpath:META-INF/spring/context/hakuimport-context.xml", "test-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class HakuImportKoosteReititysTest {

  private static final String HAKU_OID = "hakuoid1";

  private static final String[][] HAKUKOHDE_URIS_AND_OIDS = { { "hakukohdekoodiuri1", "hakukohdeoid1" },
      { "hakukohdekoodiuri2", "hakukohdeoid2" }, { "hakukohdekoodiuri3", "hakukohdeoid3" },
      { "hakukohdekoodiuri4", "hakukohdeoid4" }, { "hakukohdekoodiuri5", "hakukohdeoid5" },
      { "hakukohdekoodiuri6", "hakukohdeoid6s" } };

  @Autowired
  private HakuImportResource hakuImportAktivointiResource;

  @Bean
  public ValintaperusteetAsyncResource getValintaperusteServiceMock() {
    return mock(ValintaperusteetAsyncResource.class);
  }

  @Bean
  public KoodiService getKoodiServiceMock() {

    // Pahoittelut. Tämä mocki piti tehdä tällä tavalla vähän nihkeesti, kun
    // Mockitolla mokkaaminen
    // meni turhan
    // työlääksi. :(
    KoodiService koodiService = new KoodiService() {
      private int kutsukertojenLkm = 0;

      @Override
      public List<KoodiType> listKoodiByRelation(
          @WebParam(name = "koodi", targetNamespace = "") KoodiUriAndVersioType koodi,
          @WebParam(name = "onAlaKoodi", targetNamespace = "") boolean onAlaKoodi,
          @WebParam(name = "suhdeTyyppi", targetNamespace = "") SuhteenTyyppiType suhdeTyyppi)
          throws GenericFault {
        throw new UnsupportedOperationException();
      }

      @Override
      public List<KoodiType> searchKoodisByKoodisto(
          @WebParam(name = "searchCriteria", targetNamespace = "") SearchKoodisByKoodistoCriteriaType searchCriteria)
          throws GenericFault {
        throw new UnsupportedOperationException();
      }

      @Override
      public List<KoodiType> searchKoodis(
          @WebParam(name = "searchCriteria", targetNamespace = "") SearchKoodisCriteriaType searchCriteria)
          throws GenericFault {
        List<KoodiType> lista = new ArrayList<KoodiType>();

        String uri = searchCriteria.getKoodiUris().get(0);

        boolean found = false;
        for (String[] uriAndOid : HAKUKOHDE_URIS_AND_OIDS) {
          if (uriAndOid[0].equals(uri)) {
            found = true;
            break;
          }
        }
        assertTrue(found);

        KoodiType koodi = new KoodiType();
        koodi.setKoodiUri(uri);
        koodi.setKoodiArvo(uri + "-arvo");

        KoodiMetadataType meta = new KoodiMetadataType();
        meta.setKieli(KieliType.FI);
        meta.setNimi(uri + "-nimi");
        koodi.getMetadata().add(meta);

        lista.add(koodi);

        ++kutsukertojenLkm;
        if (kutsukertojenLkm > HAKUKOHDE_URIS_AND_OIDS.length) {
          fail();
        }

        return lista;
      }
    };

    return koodiService;
  }

  @Bean
  public TarjontaPublicService getTarjontaPublicServiceMock() {
    TarjontaPublicService tarjontaService = mock(TarjontaPublicService.class);
    TarjontaTyyppi tarjonta = new TarjontaTyyppi();
    for (String[] uriAndOid : HAKUKOHDE_URIS_AND_OIDS) {
      HakukohdeTyyppi hakukohde = new HakukohdeTyyppi();
      hakukohde.setHakukohdeNimi(uriAndOid[0]);
      hakukohde.setOid(uriAndOid[1]);
      tarjonta.getHakukohde().add(hakukohde);
    }
    when(tarjontaService.haeTarjonta(eq(HAKU_OID))).thenReturn(tarjonta);
    return tarjontaService;
  }

  @Autowired
  private ValintaperusteetAsyncResource valintaperusteService;

  @Autowired
  private KoodiService koodiService;

  @Test
  @Ignore
  public void testImportHaku() {
    HttpServletRequest requestMock = mock(HttpServletRequest.class);
    hakuImportAktivointiResource.aktivoiHakuImport(HAKU_OID, requestMock);
    ArgumentCaptor<HakukohdeImportDTO> argCaptor = ArgumentCaptor.forClass(HakukohdeImportDTO.class);
    verify(valintaperusteService, times(HAKUKOHDE_URIS_AND_OIDS.length)).tuoHakukohde(argCaptor.capture());

    // Tsekataan, että valintaperusteserviceä kutsuttiin kaikille hakukohde oideille
    outer: for (String[] uriAndOid : HAKUKOHDE_URIS_AND_OIDS) {
      for (HakukohdeImportDTO t : argCaptor.getAllValues()) {
        if (uriAndOid[1].equals(t.getHakukohdeOid())) {
          assertEquals(HAKU_OID, t.getHakuOid());
          assertEquals(uriAndOid[0], t.getHakukohdekoodi().getKoodiUri());
          assertEquals(uriAndOid[0] + "-arvo", t.getHakukohdekoodi().getArvo());
          assertEquals(uriAndOid[0] + "-nimi", t.getHakukohdekoodi().getNimiFi());
          // assertEquals(uriAndOid[0] + "-nimi", t.getNimi());

          continue outer;
        }
      }

      fail();
    }
  }
}
