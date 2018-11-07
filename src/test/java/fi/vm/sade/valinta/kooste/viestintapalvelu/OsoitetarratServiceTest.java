package fi.vm.sade.valinta.kooste.viestintapalvelu;

import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.mocks.*;
import fi.vm.sade.valinta.kooste.spec.tarjonta.TarjontaSpec;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;
import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.osallistuminen;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.valintakoe;

/**
 * @author Jussi Jartamo
 */
public class OsoitetarratServiceTest {
    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    final HttpResourceBuilder.WebClientExposingHttpResource osoitetarratResource = new HttpResourceBuilder()
            .address(root + "/viestintapalvelu/osoitetarrat/aktivoi")
            .buildExposingWebClientDangerously();
    final HttpResourceBuilder.WebClientExposingHttpResource osoitetarratSijoittelussaHyvaksytyilleResource = new HttpResourceBuilder()
            .address(root + "/viestintapalvelu/osoitetarrat/sijoittelussahyvaksytyille/aktivoi")
            .buildExposingWebClientDangerously();
    final String HAKU1 = "HAKU1";
    final String HAKUKOHDE1 = "HAKUKOHDE1";
    final String VALINTAKOE1 = "VALINTAKOE1";
    final String HAKEMUS1 = "HAKEMUS1";
    final String ATARUHAKEMUS1 = "1.2.246.562.11.00000000000000000063";
    final String SELVITETTY_TUNNISTE1 = "SELVITETTY_TUNNISTE1";
    private final Observable<Response> byteArrayResponse = Observable.just(Response.ok(new ByteArrayInputStream("lol".getBytes())).build());

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Test
    public void testaaOsoitetarratSijoittelussaHyvaksytyille() {
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
            Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString())).thenReturn(Observable.just(Collections.emptyList()));
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
            Mockito.when(Mocks.getViestintapalveluAsyncResource().haeOsoitetarrat(osoitteetArgumentCaptor.capture())).thenReturn(byteArrayResponse);
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
            Mockito.verify(Mocks.getViestintapalveluAsyncResource(), Mockito.timeout(500).times(1)).haeOsoitetarrat(Mockito.any());
            List<Osoitteet> osoitteet = osoitteetArgumentCaptor.getAllValues();
            Assert.assertEquals(1, osoitteet.size());
            Assert.assertEquals(1, osoitteet.iterator().next().getAddressLabels().size());
        } finally {
            Mocks.reset();
            MockApplicationAsyncResource.clear();
        }
    }

    @Test
    public void ataruHakuTestaaOsoitetarratValintakokeeseenOsallistujilleKunKaikkiKutsutaan() {
        Mocks.reset();
        try {
            Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString())).thenReturn(Observable.just(Collections.emptyList()));
            MockValintaperusteetAsyncResource.setValintakokeetResult(Arrays.asList(
                    valintakoe()
                            .setTunniste(VALINTAKOE1)
                            .setSelvitettyTunniste(SELVITETTY_TUNNISTE1)
                            .setKaikkiKutsutaan()
                            .build())
            );
            MockValintalaskentaValintakoeAsyncResource.setResult(Arrays.asList(
                    osallistuminen()
                            .setHakemusOid(ATARUHAKEMUS1)
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
            MockTarjontaAsyncService.setMockHaku(new TarjontaSpec.HakuBuilder(HAKU1, "AtaruLomakeAvain").build());
            MockAtaruAsyncResource.setByHakukohdeResult(Collections.singletonList(MockAtaruAsyncResource.getAtaruHakemusWrapper("1.2.246.562.11.00000000000000000063")));
            ArgumentCaptor<Osoitteet> osoitteetArgumentCaptor = ArgumentCaptor.forClass(Osoitteet.class);
            Mockito.reset(Mocks.getViestintapalveluAsyncResource());
            Mockito.when(Mocks.getViestintapalveluAsyncResource().haeOsoitetarrat(osoitteetArgumentCaptor.capture())).thenReturn(byteArrayResponse);
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
            Mockito.verify(Mocks.getViestintapalveluAsyncResource(), Mockito.timeout(500).times(1)).haeOsoitetarrat(Mockito.any());
            List<Osoitteet> osoitteet = osoitteetArgumentCaptor.getAllValues();
            Assert.assertEquals(1, osoitteet.size());
            Assert.assertEquals(1, osoitteet.iterator().next().getAddressLabels().size());
        } finally {
            Mocks.reset();
            MockTarjontaAsyncService.clear();
        }
    }

    @Test
    public void testaaOsoitetarratValintakokeeseenOsallistujilleKunYksittainenHakijaKutsutaan() {
        Mocks.reset();
        try {
            Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString())).thenReturn(Observable.just(Collections.emptyList()));
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
            Mockito.when(Mocks.getViestintapalveluAsyncResource().haeOsoitetarrat(osoitteetArgumentCaptor.capture())).thenReturn(byteArrayResponse);

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
            Mockito.verify(Mocks.getViestintapalveluAsyncResource(), Mockito.timeout(500).times(1)).haeOsoitetarrat(Mockito.any());
            List<Osoitteet> osoitteet = osoitteetArgumentCaptor.getAllValues();
            Assert.assertEquals(1, osoitteet.size());
            Assert.assertEquals(1, osoitteet.iterator().next().getAddressLabels().size());
        } finally {
            Mocks.reset();
            MockApplicationAsyncResource.clear();
        }
    }
}
