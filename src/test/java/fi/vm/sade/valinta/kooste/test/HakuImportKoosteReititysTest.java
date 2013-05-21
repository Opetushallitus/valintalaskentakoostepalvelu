package fi.vm.sade.valinta.kooste.test;

import fi.vm.sade.koodisto.service.GenericFault;
import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.SearchKoodisByKoodistoCriteriaType;
import fi.vm.sade.koodisto.service.types.SearchKoodisCriteriaType;
import fi.vm.sade.koodisto.service.types.common.*;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdeImportTyyppi;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.tarjonta.service.types.TarjontaTyyppi;
import fi.vm.sade.valinta.kooste.hakuimport.HakuImportAktivointiResource;
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

import javax.jws.WebParam;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * User: wuoti
 * Date: 20.5.2013
 * Time: 13.27
 */
@Configuration
@ContextConfiguration(classes = HakuImportKoosteReititysTest.class)
@PropertySource("classpath:test.properties")
@ImportResource("classpath:META-INF/spring/context/hakuimport-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class HakuImportKoosteReititysTest {

    private static final String HAKU_OID = "hakuoid1";

    private static final String[][] HAKUKOHDE_URIS_AND_OIDS = {{"hakukohdekoodiuri1", "hakukohdeoid1"},
            {"hakukohdekoodiuri2", "hakukohdeoid2"}, {"hakukohdekoodiuri3", "hakukohdeoid3"},
            {"hakukohdekoodiuri4", "hakukohdeoid4"}, {"hakukohdekoodiuri5", "hakukohdeoid5"},
            {"hakukohdekoodiuri6", "hakukohdeoid6s"}};

    @Autowired
    private HakuImportAktivointiResource hakuImportAktivointiResource;

    @Bean
    public ValintaperusteService getValintaperusteServiceMock() {
        return mock(ValintaperusteService.class);
    }

    @Bean
    public KoodiService getKoodiServiceMock() {

        // Pahoittelut. Tämä mocki piti tehdä tällä tavalla vähän nihkeesti, kun Mockitolla mokkaaminen meni turhan
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
                    @WebParam(name = "searchCriteria", targetNamespace = "")
                    SearchKoodisByKoodistoCriteriaType searchCriteria) throws GenericFault {
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
    private ValintaperusteService valintaperusteService;

    @Autowired
    private KoodiService koodiService;

    @Test
    public void testImportHaku() {
        hakuImportAktivointiResource.aktivoiHakuImport(HAKU_OID);
        ArgumentCaptor<HakukohdeImportTyyppi> argCaptor = ArgumentCaptor.forClass(HakukohdeImportTyyppi.class);
        verify(valintaperusteService, times(HAKUKOHDE_URIS_AND_OIDS.length)).tuoHakukohde(argCaptor.capture());

        // Tsekataan, että valintaperusteserviceä kutsuttiin kaikille hakukohde oideille
        outer:
        for (String[] uriAndOid : HAKUKOHDE_URIS_AND_OIDS) {
            for (HakukohdeImportTyyppi t : argCaptor.getAllValues()) {
                if (uriAndOid[1].equals(t.getHakukohdeOid())) {
                    assertEquals(HAKU_OID, t.getHakuOid());
                    assertEquals(uriAndOid[0], t.getHakukohdekoodi().getKoodiUri());
                    assertEquals(uriAndOid[0] + "-arvo", t.getHakukohdekoodi().getArvo());
                    assertEquals(uriAndOid[0] + "-nimi", t.getHakukohdekoodi().getNimiFi());
                    assertEquals(uriAndOid[0] + "-nimi", t.getNimi());

                    continue outer;
                }
            }

            fail();
        }
    }
}
