package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohteenValintakoeDTO;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.kouta.KoutaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.kouta.KoutaHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.kouta.KoutaValintakoe;
import fi.vm.sade.valinta.kooste.external.resource.kouta.PainotettuArvosana;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Organisaatio;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.AbstractHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SuoritaHakukohdeImportKomponenttiTest {
  private SuoritaHakukohdeImportKomponentti suoritaHakukohdeImportKomponentti;
  private KoutaAsyncResource koutaAsyncResource;
  private static final String KOUTA_HAKUKOHDE_OID = "1.2.246.562.20.00000000000000012725";
  private static final String HAKU_OID = "1.2.246.562.29.00000000000000005368";

  private static Koodi fakeKoodi(String koodiArvo, String koodiUri) {
    Koodi koodi = Mockito.mock(Koodi.class);
    when(koodi.getKoodiArvo()).thenReturn(koodiArvo);
    when(koodi.getKoodiUri()).thenReturn(koodiUri);
    return koodi;
  }

  @Before
  public void before() {
    koutaAsyncResource = Mockito.mock(KoutaAsyncResource.class);

    KoodistoCachedAsyncResource koodistoAsyncResource =
        Mockito.mock(KoodistoCachedAsyncResource.class);
    OrganisaatioAsyncResource organisaatioAsyncResource =
        Mockito.mock(OrganisaatioAsyncResource.class);
    TarjontaAsyncResource tarjontaAsyncResource = Mockito.mock(TarjontaAsyncResource.class);

    Map<String, Koodi> valintakokeenTyyppiKoodisto =
        Map.of(
            "1", fakeKoodi("1", "valintakokeentyyppi_1"),
            "2", fakeKoodi("2", "valintakokeentyyppi_2"),
            "6", fakeKoodi("6", "valintakokeentyyppi_6"));
    when(koodistoAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.VALINTAKOKEEN_TYYPPI))
        .thenReturn(valintakokeenTyyppiKoodisto);

    Map<String, Koodi> painotettavatOppiaineetLukiossaKoodisto =
        Map.of(
            "MA", fakeKoodi("MA", "painotettavatoppiaineetlukiossa_ma"),
            "BI", fakeKoodi("BI", "painotettavatoppiaineetlukiossa_bi"),
            "A1_FI", fakeKoodi("A1_FI", "painotettavatoppiaineetlukiossa_a1fi"),
            "A2_SV", fakeKoodi("A2_SV", "painotettavatoppiaineetlukiossa_a2sv"),
            "B1_DE", fakeKoodi("B1_DE", "painotettavatoppiaineetlukiossa_b1de"),
            "B2_FR", fakeKoodi("B2_FR", "painotettavatoppiaineetlukiossa_b2fr"),
            "B3_EN", fakeKoodi("B3_EN", "painotettavatoppiaineetlukiossa_b3en"));
    when(koodistoAsyncResource.haeKoodisto(
            KoodistoCachedAsyncResource.PAINOTETTAVAT_OPPIAINEET_LUKIOSSA))
        .thenReturn(painotettavatOppiaineetLukiossaKoodisto);

    Organisaatio fakeOrganisaatio = new Organisaatio();
    fakeOrganisaatio.setNimi(Map.of("fi", "testikoulu"));

    when(organisaatioAsyncResource.haeOrganisaatio(any()))
        .thenReturn(CompletableFuture.completedFuture(fakeOrganisaatio));

    Haku haku =
        new Haku(
            "H0",
            new HashMap<>(),
            new HashSet<>(),
            "AtaruLomakeAvain",
            null,
            null,
            null,
            null,
            null);
    when(tarjontaAsyncResource.haeHaku(HAKU_OID))
        .thenReturn(CompletableFuture.completedFuture(haku));

    suoritaHakukohdeImportKomponentti =
        new SuoritaHakukohdeImportKomponentti(
            tarjontaAsyncResource,
            koutaAsyncResource,
            organisaatioAsyncResource,
            koodistoAsyncResource);
  }

  @Test
  public void importoiKoutaHakukohteen() {
    KoutaHakukohde hakukohde = fakeHakukohde(new HashSet<>(), new HashSet<>(), new ArrayList<>());
    when(koutaAsyncResource.haeHakukohde(KOUTA_HAKUKOHDE_OID))
        .thenReturn(CompletableFuture.completedFuture(hakukohde));

    HakukohdeImportDTO result =
        suoritaHakukohdeImportKomponentti.suoritaHakukohdeImport(KOUTA_HAKUKOHDE_OID);

    assertNotNull(result);
    assertEquals("tarjoaja-1-oid", result.getTarjoajaOid());
    assertEquals(HAKU_OID, result.getHakuOid());
    assertEquals(KOUTA_HAKUKOHDE_OID, result.getHakukohdeOid());
    assertEquals("JULKAISTU", result.getTila());
    assertEquals(20, result.getValinnanAloituspaikat());
    assertEquals("8.5", getValintaperusteArvo(result, "painotettu_keskiarvo_hylkays_max"));
    assertEquals("hakukohdekoodiuri", result.getHakukohdekoodi().getKoodiUri());
  }

  @Test
  public void importoiKoutaHakukohteenValintakokeet() {
    Set<KoutaValintakoe> valintakokeet = new HashSet<>();
    valintakokeet.add(
        new KoutaValintakoe("paasykoe-1-oid", "valintakokeentyyppi_1#1", BigDecimal.valueOf(15)));
    valintakokeet.add(
        new KoutaValintakoe("lisanaytto-1-oid", "valintakokeentyyppi_2#1", BigDecimal.valueOf(42)));
    KoutaHakukohde hakukohde = fakeHakukohde(valintakokeet, new HashSet<>(), new ArrayList<>());
    when(koutaAsyncResource.haeHakukohde(KOUTA_HAKUKOHDE_OID))
        .thenReturn(CompletableFuture.completedFuture(hakukohde));

    HakukohdeImportDTO result =
        suoritaHakukohdeImportKomponentti.suoritaHakukohdeImport(KOUTA_HAKUKOHDE_OID);

    List<HakukohteenValintakoeDTO> valintakokeetResult = result.getValintakoe();
    assertEquals(2, valintakokeetResult.size());
    HakukohteenValintakoeDTO paasykoe =
        findValintakoe(valintakokeetResult, "valintakokeentyyppi_1#1");
    assertEquals("paasykoe-1-oid", paasykoe.getOid());
    assertEquals("valintakokeentyyppi_1#1", paasykoe.getTyyppiUri());
    HakukohteenValintakoeDTO lisanaytto =
        findValintakoe(valintakokeetResult, "valintakokeentyyppi_2#1");
    assertEquals("lisanaytto-1-oid", lisanaytto.getOid());
    assertEquals("valintakokeentyyppi_2#1", lisanaytto.getTyyppiUri());
    assertEquals("15", getValintaperusteArvo(result, "paasykoe_hylkays_max"));
    assertEquals("42", getValintaperusteArvo(result, "lisanaytto_hylkays_max"));
  }

  @Test
  public void kayttaaValintaperusteenHakukoetta() {
    Set<KoutaValintakoe> valintaperusteenValintakokeet = new HashSet<>();
    valintaperusteenValintakokeet.add(
        new KoutaValintakoe(
            "valintaperuste-paasykoe-1-oid", "valintakokeentyyppi_1#1", BigDecimal.valueOf(16)));
    valintaperusteenValintakokeet.add(
        new KoutaValintakoe(
            "valintaperuste-lisanaytto-1-oid", "valintakokeentyyppi_2#1", BigDecimal.valueOf(43)));
    KoutaHakukohde hakukohde =
        fakeHakukohde(new HashSet<>(), valintaperusteenValintakokeet, new ArrayList<>());
    when(koutaAsyncResource.haeHakukohde(KOUTA_HAKUKOHDE_OID))
        .thenReturn(CompletableFuture.completedFuture(hakukohde));

    HakukohdeImportDTO result =
        suoritaHakukohdeImportKomponentti.suoritaHakukohdeImport(KOUTA_HAKUKOHDE_OID);

    List<HakukohteenValintakoeDTO> valintakokeetResult = result.getValintakoe();
    assertEquals(2, valintakokeetResult.size());
    HakukohteenValintakoeDTO paasykoe =
        findValintakoe(valintakokeetResult, "valintakokeentyyppi_1#1");
    assertEquals("valintaperuste-paasykoe-1-oid", paasykoe.getOid());
    assertEquals("valintakokeentyyppi_1#1", paasykoe.getTyyppiUri());
    HakukohteenValintakoeDTO lisanaytto =
        findValintakoe(valintakokeetResult, "valintakokeentyyppi_2#1");
    assertEquals("valintaperuste-lisanaytto-1-oid", lisanaytto.getOid());
    assertEquals("valintakokeentyyppi_2#1", lisanaytto.getTyyppiUri());
    assertEquals("16", getValintaperusteArvo(result, "paasykoe_hylkays_max"));
    assertEquals("43", getValintaperusteArvo(result, "lisanaytto_hylkays_max"));
  }

  @Test
  public void kayttaaEnsisijaisestiHakukohteenKoetta() {
    Set<KoutaValintakoe> valintakokeet = new HashSet<>();
    valintakokeet.add(
        new KoutaValintakoe("paasykoe-1-oid", "valintakokeentyyppi_1#1", BigDecimal.valueOf(15)));
    valintakokeet.add(
        new KoutaValintakoe("lisanaytto-1-oid", "valintakokeentyyppi_2#1", BigDecimal.valueOf(42)));
    Set<KoutaValintakoe> valintaperusteenValintakokeet = new HashSet<>();
    valintaperusteenValintakokeet.add(
        new KoutaValintakoe(
            "valintaperuste-paasykoe-1-oid", "valintakokeentyyppi_1#1", BigDecimal.valueOf(16)));
    valintaperusteenValintakokeet.add(
        new KoutaValintakoe(
            "valintaperuste-lisanaytto-1-oid", "valintakokeentyyppi_2#1", BigDecimal.valueOf(43)));
    KoutaHakukohde hakukohde =
        fakeHakukohde(valintakokeet, valintaperusteenValintakokeet, new ArrayList<>());
    when(koutaAsyncResource.haeHakukohde(KOUTA_HAKUKOHDE_OID))
        .thenReturn(CompletableFuture.completedFuture(hakukohde));

    HakukohdeImportDTO result =
        suoritaHakukohdeImportKomponentti.suoritaHakukohdeImport(KOUTA_HAKUKOHDE_OID);

    List<HakukohteenValintakoeDTO> valintakokeetResult = result.getValintakoe();
    assertEquals(2, valintakokeetResult.size());
    HakukohteenValintakoeDTO paasykoe =
        findValintakoe(valintakokeetResult, "valintakokeentyyppi_1#1");
    assertEquals("paasykoe-1-oid", paasykoe.getOid());
    assertEquals("valintakokeentyyppi_1#1", paasykoe.getTyyppiUri());
    HakukohteenValintakoeDTO lisanaytto =
        findValintakoe(valintakokeetResult, "valintakokeentyyppi_2#1");
    assertEquals("lisanaytto-1-oid", lisanaytto.getOid());
    assertEquals("valintakokeentyyppi_2#1", lisanaytto.getTyyppiUri());
    assertEquals("15", getValintaperusteArvo(result, "paasykoe_hylkays_max"));
    assertEquals("42", getValintaperusteArvo(result, "lisanaytto_hylkays_max"));
  }

  @Test
  public void painotetutArvosanat() {
    List<PainotettuArvosana> painotetutArvosanat =
        List.of(
            new PainotettuArvosana("painotettavatoppiaineetlukiossa_ma", BigDecimal.valueOf(2)),
            new PainotettuArvosana("painotettavatoppiaineetlukiossa_bi", BigDecimal.valueOf(3)));
    KoutaHakukohde hakukohde = fakeHakukohde(new HashSet<>(), new HashSet<>(), painotetutArvosanat);
    when(koutaAsyncResource.haeHakukohde(KOUTA_HAKUKOHDE_OID))
        .thenReturn(CompletableFuture.completedFuture(hakukohde));

    HakukohdeImportDTO result =
        suoritaHakukohdeImportKomponentti.suoritaHakukohdeImport(KOUTA_HAKUKOHDE_OID);
    assertEquals("2", getValintaperusteArvo(result, "MA_painokerroin"));
    assertEquals("3", getValintaperusteArvo(result, "BI_painokerroin"));
  }

  @Test
  public void painotetutKieltenArvosanat() {
    List<PainotettuArvosana> painotetutArvosanat =
        List.of(
            new PainotettuArvosana("painotettavatoppiaineetlukiossa_a1fi", BigDecimal.valueOf(1)),
            new PainotettuArvosana("painotettavatoppiaineetlukiossa_a2sv", BigDecimal.valueOf(2)),
            new PainotettuArvosana("painotettavatoppiaineetlukiossa_b1de", BigDecimal.valueOf(3)),
            new PainotettuArvosana("painotettavatoppiaineetlukiossa_b2fr", BigDecimal.valueOf(4)),
            new PainotettuArvosana("painotettavatoppiaineetlukiossa_b3en", BigDecimal.valueOf(5)));
    KoutaHakukohde hakukohde = fakeHakukohde(new HashSet<>(), new HashSet<>(), painotetutArvosanat);
    when(koutaAsyncResource.haeHakukohde(KOUTA_HAKUKOHDE_OID))
        .thenReturn(CompletableFuture.completedFuture(hakukohde));

    HakukohdeImportDTO result =
        suoritaHakukohdeImportKomponentti.suoritaHakukohdeImport(KOUTA_HAKUKOHDE_OID);
    assertEquals("1", getValintaperusteArvo(result, "A1_FI_painokerroin"));
    assertEquals("1", getValintaperusteArvo(result, "A12_FI_painokerroin"));
    assertEquals("1", getValintaperusteArvo(result, "A13_FI_painokerroin"));
    assertEquals("2", getValintaperusteArvo(result, "A2_SV_painokerroin"));
    assertEquals("2", getValintaperusteArvo(result, "A22_SV_painokerroin"));
    assertEquals("2", getValintaperusteArvo(result, "A23_SV_painokerroin"));
    assertEquals("3", getValintaperusteArvo(result, "B1_DE_painokerroin"));
    assertEquals("4", getValintaperusteArvo(result, "B2_FR_painokerroin"));
    assertEquals("4", getValintaperusteArvo(result, "B22_FR_painokerroin"));
    assertEquals("4", getValintaperusteArvo(result, "B23_FR_painokerroin"));
    assertEquals("5", getValintaperusteArvo(result, "B3_EN_painokerroin"));
    assertEquals("5", getValintaperusteArvo(result, "B32_EN_painokerroin"));
    assertEquals("5", getValintaperusteArvo(result, "B33_EN_painokerroin"));

    assertThrows(
        NoSuchElementException.class, () -> getValintaperusteArvo(result, "B12_DE_painokerroin"));
    assertThrows(
        NoSuchElementException.class, () -> getValintaperusteArvo(result, "B13_DE_painokerroin"));
  }

  @Test
  public void lukioKoulutustyyppi() {
    KoutaHakukohde hakukohde =
        fakeHakukohde(new HashSet<>(), new HashSet<>(), new ArrayList<>(), "koulutustyyppi_2");
    when(koutaAsyncResource.haeHakukohde(KOUTA_HAKUKOHDE_OID))
        .thenReturn(CompletableFuture.completedFuture(hakukohde));

    HakukohdeImportDTO result =
        suoritaHakukohdeImportKomponentti.suoritaHakukohdeImport(KOUTA_HAKUKOHDE_OID);

    assertEquals("hakukohteet_000", result.getHakukohdekoodi().getKoodiUri());
  }

  private static KoutaHakukohde fakeHakukohde(
      Set<KoutaValintakoe> valintakokeet,
      Set<KoutaValintakoe> valintaperusteValintakokeet,
      List<PainotettuArvosana> painotetutArvosanat) {
    return fakeHakukohde(valintakokeet, valintaperusteValintakokeet, painotetutArvosanat, null);
  }

  private static KoutaHakukohde fakeHakukohde(
      Set<KoutaValintakoe> valintakokeet,
      Set<KoutaValintakoe> valintaperusteValintakokeet,
      List<PainotettuArvosana> painotetutArvosanat,
      String koulutustyyppikoodi) {
    return new KoutaHakukohde(
        KOUTA_HAKUKOHDE_OID,
        AbstractHakukohde.Tila.JULKAISTU,
        new HashMap<>(),
        HAKU_OID,
        Set.of("tarjoaja-1-oid"),
        new HashSet<>(),
        new HashSet<>(),
        20,
        new HashMap<>(),
        valintakokeet,
        valintaperusteValintakokeet,
        BigDecimal.valueOf(8.5),
        painotetutArvosanat,
        "hakukohdekoodiuri",
        koulutustyyppikoodi);
  }

  private static String getValintaperusteArvo(HakukohdeImportDTO importDTO, String avain) {
    return importDTO.getValintaperuste().stream()
        .filter(avainArvo -> avainArvo.getAvain().equals(avain))
        .findFirst()
        .get()
        .getArvo();
  }

  private static HakukohteenValintakoeDTO findValintakoe(
      List<HakukohteenValintakoeDTO> valintakokeet, String tyyppiUri) {
    return valintakokeet.stream()
        .filter(vk -> vk.getTyyppiUri().equals(tyyppiUri))
        .findFirst()
        .get();
  }
}
