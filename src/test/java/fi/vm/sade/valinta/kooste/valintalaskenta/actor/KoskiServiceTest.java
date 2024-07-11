package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.Funktionimi;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.SuoritustiedotDTO;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

public class KoskiServiceTest {
  private static final Gson GSON = new Gson();
  private static final java.util.Date NYT = new java.util.Date();
  private final String koskifuntionimet =
      "HAEAMMATILLINENYTOARVOSANA,HAEAMMATILLINENYTOARVIOINTIASTEIKKO";

  private CompletableFuture<List<ValintaperusteetDTO>> koskiFunktionSisaltavaValintaperuste =
      luoKoskifunktionSisaltavaValintaperuste();
  private CompletableFuture<List<ValintaperusteetDTO>> kosketonValintaperuste =
      luoKosketonValintaperuste();
  private final KoskiAsyncResource koskiAsyncResource = mock(KoskiAsyncResource.class);
  private HakemusWrapper hakemusWrapper = mock(HakemusWrapper.class);
  private CompletableFuture<List<HakemusWrapper>> hakemukset =
      CompletableFuture.completedFuture(Collections.singletonList(hakemusWrapper));
  private final String oppijanumero = "1.2.246.562.24.50534365452";
  private final KoskiOppija koskiOppija = new KoskiOppija();
  private Set<KoskiOppija> koskioppijat = Collections.singleton(koskiOppija);
  private final SuoritustiedotDTO suoritustiedotDTO = new SuoritustiedotDTO();

  private LocalDate paivaJonkaMukaisiaTietojaKoskiDatastaKaytetaan = LocalDate.now();

  @BeforeEach
  public void setUpTestdata() {
    when(hakemusWrapper.getPersonOid()).thenReturn(oppijanumero);
    koskiOppija.setOppijaOid(oppijanumero);
    koskiOppija.setOpiskeluoikeudet(
        GSON.fromJson(
            classpathResourceAsString(
                "fi/vm/sade/valinta/kooste/valintalaskenta/koski-monitutkinto.json"),
            JsonArray.class));
  }

  @Test
  public void koskestaHaettavienHakukohteidenListallaVoiRajoittaaMilleHakukohteilleHaetaanKoskesta()
      throws ExecutionException, InterruptedException {
    KoskiOpiskeluoikeusHistoryService koskiOpiskeluoikeusHistoryService =
        new KoskiOpiskeluoikeusHistoryService();
    KoskiService service =
        new KoskiService(
            "hakukohdeoid1,hakukohdeoid2",
            koskifuntionimet,
            "ammatillinenkoulutus",
            koskiAsyncResource,
            koskiOpiskeluoikeusHistoryService);
    LocalDate paivaJonkaMukaisiaTietojaKoskiDatastaKaytetaan =
        koskiOpiskeluoikeusHistoryService.etsiKoskiDatanLeikkuriPvm(
            koskiFunktionSisaltavaValintaperuste.join(), "jokumuuhakukohde");

    when(koskiAsyncResource.findKoskiOppijat(
            Collections.singletonList(oppijanumero),
            paivaJonkaMukaisiaTietojaKoskiDatastaKaytetaan))
        .thenReturn(CompletableFuture.completedFuture(koskioppijat));

    Map<String, KoskiOppija> koskiOppijatOppijanumeroittain =
        service
            .haeKoskiOppijat(
                "hakukohdeoid1",
                koskiFunktionSisaltavaValintaperuste,
                hakemukset,
                suoritustiedotDTO,
                NYT)
            .get();
    assertThat(koskiOppijatOppijanumeroittain.entrySet(), hasSize(1));
    assertEquals(koskiOppijatOppijanumeroittain.get(oppijanumero), koskiOppija);
    assertTrue(suoritustiedotDTO.onKoskiopiskeluoikeudet(oppijanumero));
    assertEquals(
        GSON.toJson(koskiOppija.getOpiskeluoikeudet()),
        suoritustiedotDTO.haeKoskiOpiskeluoikeudetJson(oppijanumero));

    assertThat(
        service
            .haeKoskiOppijat(
                "jokumuuhakukohde",
                koskiFunktionSisaltavaValintaperuste,
                hakemukset,
                suoritustiedotDTO,
                NYT)
            .get()
            .entrySet(),
        is(empty()));
  }

  @Test
  public void
      koskidataaKayttavienFunktionimienListallaVoiRajoittaaMilleHakukohteilleHaetaanKoskesta()
          throws ExecutionException, InterruptedException {
    KoskiOpiskeluoikeusHistoryService koskiOpiskeluoikeusHistoryService =
        new KoskiOpiskeluoikeusHistoryService();
    KoskiService service =
        new KoskiService(
            "ALL",
            koskifuntionimet,
            "ammatillinenkoulutus",
            koskiAsyncResource,
            koskiOpiskeluoikeusHistoryService);

    LocalDate paivaJonkaMukaisiaTietojaKoskiDatastaKaytetaan =
        koskiOpiskeluoikeusHistoryService.etsiKoskiDatanLeikkuriPvm(
            koskiFunktionSisaltavaValintaperuste.join(), "hakukohdeoid1");
    when(koskiAsyncResource.findKoskiOppijat(
            Collections.singletonList(oppijanumero),
            paivaJonkaMukaisiaTietojaKoskiDatastaKaytetaan))
        .thenReturn(CompletableFuture.completedFuture(koskioppijat));

    Map<String, KoskiOppija> koskiOppijatOppijanumeroittain =
        service
            .haeKoskiOppijat(
                "hakukohdeoid1",
                koskiFunktionSisaltavaValintaperuste,
                hakemukset,
                suoritustiedotDTO,
                NYT)
            .get();
    assertThat(koskiOppijatOppijanumeroittain.entrySet(), hasSize(1));
    assertEquals(koskiOppijatOppijanumeroittain.get(oppijanumero), koskiOppija);
    assertTrue(suoritustiedotDTO.onKoskiopiskeluoikeudet(oppijanumero));
    assertEquals(
        GSON.toJson(koskiOppija.getOpiskeluoikeudet()),
        suoritustiedotDTO.haeKoskiOpiskeluoikeudetJson(oppijanumero));

    assertThat(
        service
            .haeKoskiOppijat(
                "hakukohdeoid1", kosketonValintaperuste, hakemukset, suoritustiedotDTO, NYT)
            .get()
            .entrySet(),
        is(empty()));
  }

  @Test
  public void koskestaEiHaetaDataaJosJononViimeinenLaskentapvmOnOhitettu()
      throws ExecutionException, InterruptedException {
    final java.util.Date eilinen =
        Date.from(LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
    KoskiOpiskeluoikeusHistoryService koskiOpiskeluoikeusHistoryService =
        new KoskiOpiskeluoikeusHistoryService();
    KoskiService service =
        new KoskiService(
            "ALL",
            koskifuntionimet,
            "ammatillinenkoulutus",
            koskiAsyncResource,
            koskiOpiskeluoikeusHistoryService);
    koskiFunktionSisaltavaValintaperuste
        .get()
        .forEach(
            valintaperusteetDTO ->
                valintaperusteetDTO
                    .getValinnanVaihe()
                    .getValintatapajono()
                    .forEach(jono -> jono.setEiLasketaPaivamaaranJalkeen(eilinen)));

    LocalDate paivaJonkaMukaisiaTietojaKoskiDatastaKaytetaan =
        koskiOpiskeluoikeusHistoryService.etsiKoskiDatanLeikkuriPvm(
            koskiFunktionSisaltavaValintaperuste.join(), "hakukohdeoid1");

    when(koskiAsyncResource.findKoskiOppijat(
            Collections.singletonList(oppijanumero),
            paivaJonkaMukaisiaTietojaKoskiDatastaKaytetaan))
        .thenReturn(
            CompletableFuture.failedFuture(new RuntimeException("Tätähän ei pitänyt kutsua!")));

    assertThat(
        service
            .haeKoskiOppijat(
                "hakukohdeoid1",
                koskiFunktionSisaltavaValintaperuste,
                hakemukset,
                suoritustiedotDTO,
                NYT)
            .get()
            .entrySet(),
        is(empty()));

    verifyNoMoreInteractions(koskiAsyncResource);
  }

  private CompletableFuture<List<ValintaperusteetDTO>> koskiDataaKayttavaValintaperuste(
      String leikkuriPvm) {
    return this.koskiFunktionSisaltavaValintaperuste.thenApplyAsync(
        vps -> {
          KoskiOpiskeluoikeusHistoryService.etsiTutkintojenIterointiFunktioKutsut(vps)
              .forEach(
                  iterointiFunktioKutsu -> {
                    iterointiFunktioKutsu.getSyoteparametrit().stream()
                        .filter(
                            p ->
                                Funktionimi.ITEROIAMMATILLISETTUTKINNOT_LEIKKURIPVM_PARAMETRI
                                    .equals(p.getAvain()))
                        .forEach(leikkuriPvmParametri -> leikkuriPvmParametri.setArvo(leikkuriPvm));
                  });
          return vps;
        });
  }

  private CompletableFuture<List<ValintaperusteetDTO>> luoKoskifunktionSisaltavaValintaperuste() {
    return kaariListaanJaFutureen(
        GSON.fromJson(
            classpathResourceAsString(
                "fi/vm/sade/valinta/kooste/valintalaskenta/actor/valintaperusteita_koskikaavojen_kanssa.json"),
            ValintaperusteetDTO.class));
  }

  private CompletableFuture<List<ValintaperusteetDTO>> luoKosketonValintaperuste() {
    return kaariListaanJaFutureen(
        GSON.fromJson(
            classpathResourceAsString(
                "fi/vm/sade/valinta/kooste/valintalaskenta/actor/valintaperusteita_ilman_koskikaavoja.json"),
            ValintaperusteetDTO.class));
  }

  private <T> CompletableFuture<List<T>> kaariListaanJaFutureen(T x) {
    return CompletableFuture.completedFuture(Collections.singletonList(x));
  }

  private static String classpathResourceAsString(String path) {
    try {
      return IOUtils.toString(new ClassPathResource(path).getInputStream());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
