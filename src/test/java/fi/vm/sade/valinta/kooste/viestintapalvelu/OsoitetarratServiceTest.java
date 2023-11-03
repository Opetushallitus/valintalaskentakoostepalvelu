package fi.vm.sade.valinta.kooste.viestintapalvelu;

import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;
import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.osallistuminen;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.valintakoe;

import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockAtaruAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockTarjontaAsyncService;
import fi.vm.sade.valinta.kooste.mocks.MockValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.Mocks;
import fi.vm.sade.valinta.kooste.testapp.MockResourcesApp;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import io.reactivex.Observable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** @author Jussi Jartamo */
public class OsoitetarratServiceTest {
  final String root =
      "http://localhost:" + MockResourcesApp.port + "/valintalaskentakoostepalvelu/resources";

  final HttpResourceBuilder.WebClientExposingHttpResource osoitetarratResource =
      new HttpResourceBuilder(getClass().getName())
          .address(root + "/viestintapalvelu/osoitetarrat/aktivoi")
          .buildExposingWebClientDangerously();
  final HttpResourceBuilder.WebClientExposingHttpResource
      osoitetarratSijoittelussaHyvaksytyilleResource =
          new HttpResourceBuilder(getClass().getName())
              .address(root + "/viestintapalvelu/osoitetarrat/sijoittelussahyvaksytyille/aktivoi")
              .buildExposingWebClientDangerously();
  final String HAKU1 = "HAKU1";
  final String HAKUKOHDE1 = "HAKUKOHDE1";
  final String VALINTAKOE1 = "VALINTAKOE1";
  final String HAKEMUS1 = "HAKEMUS1";
  final String ATARUHAKEMUS1 = "1.2.246.562.11.00000000000000000063";
  final String SELVITETTY_TUNNISTE1 = "SELVITETTY_TUNNISTE1";
  private final Observable<InputStream> byteArrayResponse =
      Observable.just(new ByteArrayInputStream("lol".getBytes()));

  @BeforeEach
  public void startServer() {
    MockResourcesApp.start();
  }

  @Test
  public void testaaOsoitetarratSijoittelussaHyvaksytyille() {
    Mocks.reset();
    try {
      String HAKU2 = "1.2.246.562.5.2013080813081926341927";
      String HAKUKOHDE2 = "1.2.246.562.5.39836447563";
      Response r =
          osoitetarratSijoittelussaHyvaksytyilleResource
              .getWebClient()
              .query("hakuOid", HAKU2)
              .query("hakukohdeOid", HAKUKOHDE2)
              .post(Entity.entity(new DokumentinLisatiedot(), "application/json"));
      Assertions.assertEquals(200, r.getStatus());
    } finally {
      Mocks.reset();
    }
  }

  @Test
  public void testaaOsoitetarratValintakokeeseenOsallistujilleKunKaikkiKutsutaan() {
    Mocks.reset();
    try {
      Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString()))
          .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
      MockValintaperusteetAsyncResource.setValintakokeetResult(
          Arrays.asList(
              valintakoe()
                  .setTunniste(VALINTAKOE1)
                  .setSelvitettyTunniste(SELVITETTY_TUNNISTE1)
                  .setKaikkiKutsutaan()
                  .build()));
      MockValintalaskentaValintakoeAsyncResource.setResult(
          Arrays.asList(
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
                  .build()));
      MockApplicationAsyncResource.setResult(
          Arrays.asList(hakemus().addHakutoive(HAKUKOHDE1).setOid(HAKEMUS1).build()));
      ArgumentCaptor<Osoitteet> osoitteetArgumentCaptor = ArgumentCaptor.forClass(Osoitteet.class);
      Mockito.reset(Mocks.getViestintapalveluAsyncResource());
      Mockito.when(
              Mocks.getViestintapalveluAsyncResource()
                  .haeOsoitetarrat(osoitteetArgumentCaptor.capture()))
          .thenReturn(byteArrayResponse);
      Response r =
          osoitetarratResource
              .getWebClient()
              .query("hakuOid", HAKU1)
              .query("hakukohdeOid", HAKUKOHDE1)
              .query("valintakoeTunnisteet", SELVITETTY_TUNNISTE1)
              .post(Entity.entity(new DokumentinLisatiedot(), "application/json"));
      Assertions.assertEquals(200, r.getStatus());
      // Ei välttämättä tarpeen koska asyncit testeissä palautuu lähtökohtaisesti heti mutta
      // muutosten varalta
      // annetaan pieni odotus aika ellei kutsut ole jo perillä.
      Mockito.verify(Mocks.getViestintapalveluAsyncResource(), Mockito.timeout(500).times(1))
          .haeOsoitetarrat(Mockito.any());
      List<Osoitteet> osoitteet = osoitteetArgumentCaptor.getAllValues();
      Assertions.assertEquals(1, osoitteet.size());
      Assertions.assertEquals(1, osoitteet.iterator().next().getAddressLabels().size());
    } finally {
      Mocks.reset();
      MockApplicationAsyncResource.clear();
    }
  }

  @Test
  public void ataruHakuTestaaOsoitetarratValintakokeeseenOsallistujilleKunKaikkiKutsutaan() {
    Mocks.reset();
    try {
      Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString()))
          .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
      MockValintaperusteetAsyncResource.setValintakokeetResult(
          Arrays.asList(
              valintakoe()
                  .setTunniste(VALINTAKOE1)
                  .setSelvitettyTunniste(SELVITETTY_TUNNISTE1)
                  .setKaikkiKutsutaan()
                  .build()));
      MockValintalaskentaValintakoeAsyncResource.setResult(
          Arrays.asList(
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
                  .build()));
      MockTarjontaAsyncService.setMockHaku(
          new Haku(
              HAKU1,
              new HashMap<>(),
              new HashSet<>(),
              "AtaruLomakeAvain",
              null,
              null,
              null,
              null,
              null));
      MockAtaruAsyncResource.setByHakukohdeResult(
          Collections.singletonList(
              MockAtaruAsyncResource.getAtaruHakemusWrapper(
                  "1.2.246.562.11.00000000000000000063")));
      ArgumentCaptor<Osoitteet> osoitteetArgumentCaptor = ArgumentCaptor.forClass(Osoitteet.class);
      Mockito.reset(Mocks.getViestintapalveluAsyncResource());
      Mockito.when(
              Mocks.getViestintapalveluAsyncResource()
                  .haeOsoitetarrat(osoitteetArgumentCaptor.capture()))
          .thenReturn(byteArrayResponse);
      Response r =
          osoitetarratResource
              .getWebClient()
              .query("hakuOid", HAKU1)
              .query("hakukohdeOid", HAKUKOHDE1)
              .query("valintakoeTunnisteet", SELVITETTY_TUNNISTE1)
              .post(Entity.entity(new DokumentinLisatiedot(), "application/json"));
      Assertions.assertEquals(200, r.getStatus());
      // Ei välttämättä tarpeen koska asyncit testeissä palautuu lähtökohtaisesti heti mutta
      // muutosten varalta
      // annetaan pieni odotus aika ellei kutsut ole jo perillä.
      Mockito.verify(Mocks.getViestintapalveluAsyncResource(), Mockito.timeout(500).times(1))
          .haeOsoitetarrat(Mockito.any());
      List<Osoitteet> osoitteet = osoitteetArgumentCaptor.getAllValues();
      Assertions.assertEquals(1, osoitteet.size());
      Assertions.assertEquals(1, osoitteet.iterator().next().getAddressLabels().size());
    } finally {
      Mocks.reset();
      MockTarjontaAsyncService.clear();
    }
  }

  @Test
  public void testaaOsoitetarratValintakokeeseenOsallistujilleKunYksittainenHakijaKutsutaan() {
    Mocks.reset();
    try {
      Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString()))
          .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
      MockValintaperusteetAsyncResource.setValintakokeetResult(
          Arrays.asList(valintakoe().setTunniste(VALINTAKOE1).build()));
      List<ValintakoeOsallistuminenDTO> osallistumistiedot =
          Arrays.asList(
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
                  .build());
      MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);
      MockApplicationAsyncResource.setResult(
          Arrays.asList(hakemus().addHakutoive(HAKUKOHDE1).setOid(HAKEMUS1).build()));
      MockApplicationAsyncResource.setResultByOid(
          Arrays.asList(hakemus().addHakutoive(HAKUKOHDE1).setOid(HAKEMUS1).build()));
      ArgumentCaptor<Osoitteet> osoitteetArgumentCaptor = ArgumentCaptor.forClass(Osoitteet.class);
      Mockito.reset(Mocks.getViestintapalveluAsyncResource());
      Mockito.when(
              Mocks.getViestintapalveluAsyncResource()
                  .haeOsoitetarrat(osoitteetArgumentCaptor.capture()))
          .thenReturn(byteArrayResponse);

      Response r =
          osoitetarratResource
              .getWebClient()
              .query("hakuOid", HAKU1)
              .query("hakukohdeOid", HAKUKOHDE1)
              .query("valintakoeTunnisteet", VALINTAKOE1)
              .post(Entity.entity(new DokumentinLisatiedot(), "application/json"));
      Assertions.assertEquals(200, r.getStatus());
      // Ei välttämättä tarpeen koska asyncit testeissä palautuu lähtökohtaisesti heti mutta
      // muutosten varalta
      // annetaan pieni odotus aika ellei kutsut ole jo perillä.
      Mockito.verify(Mocks.getViestintapalveluAsyncResource(), Mockito.timeout(500).times(1))
          .haeOsoitetarrat(Mockito.any());
      List<Osoitteet> osoitteet = osoitteetArgumentCaptor.getAllValues();
      Assertions.assertEquals(1, osoitteet.size());
      Assertions.assertEquals(1, osoitteet.iterator().next().getAddressLabels().size());
    } finally {
      Mocks.reset();
      MockApplicationAsyncResource.clear();
    }
  }
}
