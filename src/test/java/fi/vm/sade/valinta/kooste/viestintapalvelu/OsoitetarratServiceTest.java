package fi.vm.sade.valinta.kooste.viestintapalvelu;

import com.google.common.util.concurrent.Futures;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.mocks.*;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.junit.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;
import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.osallistuminen;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.valintakoe;

/**
 * @author Jussi Jartamo
 */
public class OsoitetarratServiceTest {
    final static Logger LOG = LoggerFactory.getLogger(OsoitetarratServiceTest.class);
    public static final long DEFAULT_POLL_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5L); //5sec
    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    final HttpResource osoitetarratResource = new HttpResourceBuilder()
            .address(root + "/viestintapalvelu/osoitetarrat/aktivoi")
            .build();
    final HttpResource osoitetarratSijoittelussaHyvaksytyilleResource = new HttpResourceBuilder()
            .address(root + "/viestintapalvelu/osoitetarrat/sijoittelussahyvaksytyille/aktivoi")
            .build();
    final String HAKU1 = "HAKU1";
    final String HAKUKOHDE1 = "HAKUKOHDE1";
    final String TARJOAJA1 = "TARJOAJA1";
    final String VALINTAKOE1 = "VALINTAKOE1";
    final String HAKEMUS1 = "HAKEMUS1";
    final String HAKEMUS2 = "HAKEMUS2";
    final String TUNNISTE1 = "TUNNISTE1";
    final String SELVITETTY_TUNNISTE1 = "SELVITETTY_TUNNISTE1";

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Test
    public void testaaOsoitetarratSijoittelussaHyvaksytyille() throws Throwable {
        Mocks.reset();
        try {
            String HAKU2 = "1.2.246.562.5.2013080813081926341927";
            String HAKUKOHDE2 = "1.2.246.562.5.39836447563";
            Response r =
                    osoitetarratSijoittelussaHyvaksytyilleResource.getWebClient()
                            .query("hakuOid", HAKU2)
                            .query("hakukohdeOid", HAKUKOHDE2)
                            .post(Entity.entity(new DokumentinLisatiedot(),
                                    "application/json"));
            Assert.assertEquals(200, r.getStatus());
        } finally {
            Mocks.reset();
        }
    }

    @Test
    public void testaaOsoitetarratValintakokeeseenOsallistujilleKunKaikkiKutsutaan() {
        Mocks.reset();
        try {
            Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString(), Mockito.any(), Mockito.any())).thenAnswer(
                    answer -> {
                        Consumer<List<Koodi>> c = (Consumer<List<Koodi>>)answer.getArguments()[1];
                        if(c != null) {
                            c.accept(Collections.emptyList());
                        }
                        return null;
                    }
            );
            Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString())).thenReturn(Futures.immediateFuture(Collections.emptyList()));
            MockValintaperusteetAsyncResource.setValintakokeetResult(Arrays.asList(
                            valintakoe()
                                    .setTunniste(VALINTAKOE1)
                                    .setSelvitettyTunniste(SELVITETTY_TUNNISTE1)
                                    .setKaikkiKutsutaan()
                                    .build())
            );
            MockValintalaskentaValintakoeAsyncResource.setResult(Arrays.asList(
                    osallistuminen()
                            .setHakemusOid(HAKEMUS1)
                            .hakutoive()
                            .setHakukohdeOid(HAKUKOHDE1)
                            .valinnanvaihe()
                            .valintakoe()
                            .setOsallistuu()
                            .setValintakoeTunniste(SELVITETTY_TUNNISTE1)
                            .setKutsutaankoKaikki(true)
                            .build()
                            .build()
                            .build()
                            .build()
            ));
            MockApplicationAsyncResource.setResult(Arrays.asList(
                    hakemus()
                            .addHakutoive(HAKUKOHDE1)
                            .setOid(HAKEMUS1).build()
            ));
            ArgumentCaptor<Osoitteet> osoitteetArgumentCaptor = ArgumentCaptor.forClass(Osoitteet.class);
            Mockito.reset(Mocks.getViestintapalveluAsyncResource());
            Mockito.when(Mocks.getViestintapalveluAsyncResource().haeOsoitetarrat(
                    osoitteetArgumentCaptor.capture(), Mockito.any(), Mockito.any())).thenReturn(null);
            Response r =
                    osoitetarratResource.getWebClient()
                            .query("hakuOid", HAKU1)
                            .query("hakukohdeOid",HAKUKOHDE1)
                            .query("valintakoeTunnisteet", SELVITETTY_TUNNISTE1)
                            .post(Entity.entity(new DokumentinLisatiedot(),
                                    "application/json"));
            Assert.assertEquals(200, r.getStatus());
            // Ei välttämättä tarpeen koska asyncit testeissä palautuu lähtökohtaisesti heti mutta muutosten varalta
            // annetaan pieni odotus aika ellei kutsut ole jo perillä.
            Mockito.verify(Mocks.getViestintapalveluAsyncResource(), Mockito.timeout(500).times(1)).haeOsoitetarrat(Mockito.any(), Mockito.any(), Mockito.any());
            List<Osoitteet> osoitteet = osoitteetArgumentCaptor.getAllValues();
            Assert.assertEquals(1, osoitteet.size());
            Assert.assertEquals(1, osoitteet.iterator().next().getAddressLabels().size());
        } finally {
            Mocks.reset();
            MockApplicationAsyncResource.clear();
        }
    }

    @Test
    public void testaaOsoitetarratValintakokeeseenOsallistujilleKunYksittainenHakijaKutsutaan() {
        Mocks.reset();
        try {
            Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString(), Mockito.any(), Mockito.any())).thenAnswer(
                    answer -> {
                        Consumer<List<Koodi>> c = (Consumer<List<Koodi>>) answer.getArguments()[1];
                        c.accept(Collections.emptyList());
                        return null;
                    }
            );
            Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString())).thenReturn(Futures.immediateFuture(Collections.emptyList()));
            MockValintaperusteetAsyncResource.setValintakokeetResult(Arrays.asList(
                            valintakoe()
                                    .setTunniste(VALINTAKOE1)
                                    .build())
            );
            List<ValintakoeOsallistuminenDTO> osallistumistiedot = Arrays.asList(
                    osallistuminen()
                            .setHakemusOid(HAKEMUS1)
                            .hakutoive()
                            .setHakukohdeOid(HAKUKOHDE1)
                            .valinnanvaihe()
                            .valintakoe()
                            .setOsallistuu()
                            .setValintakoeTunniste(VALINTAKOE1)
                            .setKutsutaankoKaikki(true)
                            .build()
                            .build()
                            .build()
                            .build()
            );
            MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);
            MockApplicationAsyncResource.setResult(Arrays.asList(
                    hakemus()
                            .addHakutoive(HAKUKOHDE1)
                            .setOid(HAKEMUS1).build()
            ));
            MockApplicationAsyncResource.setResultByOid(Arrays.asList(
                    hakemus()
                            .addHakutoive(HAKUKOHDE1)
                            .setOid(HAKEMUS1).build()
            ));
            ArgumentCaptor<Osoitteet> osoitteetArgumentCaptor = ArgumentCaptor.forClass(Osoitteet.class);
            Mockito.reset(Mocks.getViestintapalveluAsyncResource());
            Mockito.when(Mocks.getViestintapalveluAsyncResource().haeOsoitetarrat(
                    osoitteetArgumentCaptor.capture(), Mockito.any(), Mockito.any())).thenReturn(null);

            Response r =
                    osoitetarratResource.getWebClient()
                            .query("hakuOid", HAKU1)
                            .query("hakukohdeOid",HAKUKOHDE1)
                            .query("valintakoeTunnisteet", VALINTAKOE1)
                            .post(Entity.entity(new DokumentinLisatiedot(),
                                    "application/json"));
            Assert.assertEquals(200, r.getStatus());
            // Ei välttämättä tarpeen koska asyncit testeissä palautuu lähtökohtaisesti heti mutta muutosten varalta
            // annetaan pieni odotus aika ellei kutsut ole jo perillä.
            Mockito.verify(Mocks.getViestintapalveluAsyncResource(), Mockito.timeout(500).times(1)).haeOsoitetarrat(
                    Mockito.any(), Mockito.any(), Mockito.any()
            );
            List<Osoitteet> osoitteet = osoitteetArgumentCaptor.getAllValues();
            Assert.assertEquals(1, osoitteet.size());
            Assert.assertEquals(1, osoitteet.iterator().next().getAddressLabels().size());
        } finally {
            Mocks.reset();
            MockApplicationAsyncResource.clear();
        }
    }
}
