package fi.vm.sade.valinta.kooste.valintalaskenta;

import static fi.vm.sade.service.valintaperusteet.dto.model.Osallistuminen.EI_OSALLISTUNUT;
import static fi.vm.sade.service.valintaperusteet.dto.model.Osallistuminen.MERKITSEMATTA;
import static fi.vm.sade.service.valintaperusteet.dto.model.Osallistuminen.OSALLISTUI;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.ei_osallistunut;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hylatty;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hyvaksytty;
import static fi.vm.sade.valinta.kooste.valintalaskenta.spec.SuoritusrekisteriSpec.laskennanalkamisparametri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuudenSyy;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakemuksenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakutoiveenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.valintalaskenta.spec.SuoritusrekisteriSpec;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.AvainMetatiedotDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakukohdeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.Lisapistekoulutus;
import fi.vm.sade.valintalaskenta.domain.dto.PohjakoulutusToinenAste;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.collection.IsMapContaining;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HakemuksetConverterUtilTest {

  private final HarkinnanvaraisuusAsyncResource harkinnanvaraisuusAsyncResource =
      mock(HarkinnanvaraisuusAsyncResource.class);

  private static final Haku haku =
      new Haku(
          "hakuoid",
          new HashMap<>(),
          new HashSet<>(),
          null,
          "haunkohdejoukko_11#1",
          "kausi_k#1",
          2015,
          null,
          null);

  private static final Haku ataruKoutaToisenAsteenHaku =
      new Haku(
          "1.2.246.562.29.00000000000000009999",
          new HashMap<>(),
          new HashSet<>(),
          "abc123",
          "haunkohdejoukko_11#1",
          "kausi_k#1",
          2015,
          null,
          null);

  private static final Haku haku_syksy =
      new Haku(
          "hakuoid", new HashMap<>(), new HashSet<>(), null, null, "kausi_s#1", 2015, null, null);

  private static final String HAKEMUS1_OID = "1.2.246.562.11.1";
  private static final String HAKEMUS2_OID = "1.2.246.562.11.2";
  private static final String HAKUKAUDELLA = "1.1.2015";
  private static final String HAKUKAUDEN_ULKOPUOLELLA = "1.1.2014";
  private static final String PERUSKOULU_OID = "1.2.246.562.10.82871417679";
  private static final SuoritusJaArvosanat vahvistettuPerusopetusValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setPerusopetus()
          .setVahvistettu(true)
          .setYksilollistaminen("Ei")
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistettuPerusopetusValmisAiemmin =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setPerusopetus()
          .setVahvistettu(true)
          .setYksilollistaminen("Ei")
          .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistettuLukioValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setLukio()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistamatonPerusopetusValmisEiHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setPerusopetus()
          .setVahvistettu(false)
          .setYksilollistaminen("Ei")
          .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat
      vahvistamatonYksilollistettuPerusopetusValmisHakukaudella =
          new SuoritusrekisteriSpec.SuoritusBuilder()
              .setPerusopetus()
              .setVahvistettu(false)
              .setYksilollistaminen("Kokonaan")
              .setValmistuminen(HAKUKAUDELLA)
              .setValmis()
              .done();
  private static final SuoritusJaArvosanat vahvistettuYksilollistettyPerusopetusValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setPerusopetus()
          .setVahvistettu(true)
          .setYksilollistaminen("Kokonaan")
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat
      vahvistettuOsittainYksilollistettyPerusopetusValmisHakukaudella =
          new SuoritusrekisteriSpec.SuoritusBuilder()
              .setPerusopetus()
              .setVahvistettu(true)
              .setYksilollistaminen("Osittain")
              .setValmistuminen(HAKUKAUDELLA)
              .setValmis()
              .done();
  private static final SuoritusJaArvosanat
      vahvistettuAlueittainYksilollistettyPerusopetusValmisHakukaudella =
          new SuoritusrekisteriSpec.SuoritusBuilder()
              .setPerusopetus()
              .setVahvistettu(true)
              .setYksilollistaminen("Alueittain")
              .setValmistuminen(HAKUKAUDELLA)
              .setValmis()
              .done();
  private static final SuoritusJaArvosanat vahvistettuPerusopetusKeskenHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setPerusopetus()
          .setVahvistettu(true)
          .setYksilollistaminen("Ei")
          .setValmistuminen(HAKUKAUDELLA)
          .setKesken()
          .done();
  private static final SuoritusJaArvosanat vahvistettuPerusopetusKeskenEiHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setPerusopetus()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA)
          .setKesken()
          .done();
  private static final SuoritusJaArvosanat vahvistettuPerusopetusKeskeytynytHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setPerusopetus()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setKeskeytynyt()
          .done();
  private static final SuoritusJaArvosanat vahvistettuPerusopetusKeskeytynytEiHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setPerusopetus()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA)
          .setKeskeytynyt()
          .done();
  private static final SuoritusJaArvosanat vahvistettuKymppiValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setKymppiluokka()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistettuKymppiKeskeytynytHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setKymppiluokka()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setKeskeytynyt()
          .done();
  private static final SuoritusJaArvosanat vahvistettuOpistovuosiKeskeytynytEiHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setOpistovuosi()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA)
          .setKeskeytynyt()
          .done();
  private static final SuoritusJaArvosanat vahvistettuAmmattistarttiValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setAmmattistartti()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistettuAmmatilliseenValmistavaValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setAmmatilliseenValmistava()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistettuLukioonValmistavaValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setLukioonValmistava()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistettuLisaopetusTalousValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setLisaopetusTalous()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistettuValmentavaValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setValmentava()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistamatonLukioValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setLukio()
          .setVahvistettu(false)
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistettuValmaValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setValma()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistettuTelmaValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setTelma()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistettuTuvaValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setTuva()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistamatonLukioKeskeytynytHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setLukio()
          .setVahvistettu(false)
          .setValmistuminen(HAKUKAUDELLA)
          .setKeskeytynyt()
          .done();
  private static final SuoritusJaArvosanat vahvistamatonLukioKeskeytynytEiHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setLukio()
          .setVahvistettu(false)
          .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA)
          .setKeskeytynyt()
          .done();
  private static final SuoritusJaArvosanat vahvistettuYOValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setYo()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat vahvistettuYOKeskenHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setYo()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setKesken()
          .done();
  private static final SuoritusJaArvosanat vahvistettuYOKeskeytynytHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setYo()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setKeskeytynyt()
          .done();
  private static final SuoritusJaArvosanat vahvistettuUlkomainenValmisHakukaudella =
      new SuoritusrekisteriSpec.SuoritusBuilder()
          .setUlkomainenKorvaava()
          .setVahvistettu(true)
          .setValmistuminen(HAKUKAUDELLA)
          .setValmis()
          .done();
  private static final SuoritusJaArvosanat
      vahvistamatonYksilollistettyPerusopetusValmisHakukaudella =
          new SuoritusrekisteriSpec.SuoritusBuilder()
              .setPerusopetus()
              .setVahvistettu(false)
              .setYksilollistaminen("Kokonaan")
              .setMyontaja(PERUSKOULU_OID)
              .setValmistuminen(HAKUKAUDELLA)
              .setValmis()
              .done();
  private static final SuoritusJaArvosanat
      vahvistamatonOsittainYksilollistettyPerusopetusValmisHakukaudella =
          new SuoritusrekisteriSpec.SuoritusBuilder()
              .setPerusopetus()
              .setVahvistettu(false)
              .setYksilollistaminen("Osittain")
              .setMyontaja(PERUSKOULU_OID)
              .setValmistuminen(HAKUKAUDELLA)
              .setValmis()
              .done();
  private static final SuoritusJaArvosanat
      vahvistamatonAlueittainYksilollistettyPerusopetusValmisHakukaudella =
          new SuoritusrekisteriSpec.SuoritusBuilder()
              .setPerusopetus()
              .setVahvistettu(false)
              .setYksilollistaminen("Alueittain")
              .setMyontaja(PERUSKOULU_OID)
              .setValmistuminen(HAKUKAUDELLA)
              .setValmis()
              .done();

  private static final SuoritusJaArvosanat peruskouluSuoritusValmisDeadlinenJalkeen =
      new SuoritusrekisteriSpec.SuoritusBuilder().setValmis().done();
  private static final SuoritusJaArvosanat peruskouluSuoritusKeskenDeadlinenJalkeen =
      new SuoritusrekisteriSpec.SuoritusBuilder().setKesken().done();

  private static final java.time.LocalDate currentDateFixedForTests =
      java.time.LocalDate.of(2024, 2, 21);
  private HakemuksetConverterUtil hakemuksetConverterUtil;

  @BeforeEach
  public void setup() {
    hakemuksetConverterUtil =
        new HakemuksetConverterUtil("9999-12-31", "2024-02-21", harkinnanvaraisuusAsyncResource);
  }

  @Test
  public void kasitteleDeadlinenJalkeenKeskenTilaisetKuinKeskeytynyt() {
    HakemusDTO h = new HakemusDTO();

    Oppija o = new Oppija();
    o.getSuoritukset().add(peruskouluSuoritusValmisDeadlinenJalkeen);
    o.getSuoritukset().add(peruskouluSuoritusValmisDeadlinenJalkeen);
    o.getSuoritukset().add(peruskouluSuoritusKeskenDeadlinenJalkeen);

    List<SuoritusJaArvosanat> suoritukset =
        hakemuksetConverterUtil.filterKeskenDeadlinenJalkeenSuoritukset(
            o.getSuoritukset(), java.time.LocalDate.of(2024, 2, 21));
    assertEquals(3, suoritukset.size());

    suoritukset =
        hakemuksetConverterUtil.filterKeskenDeadlinenJalkeenSuoritukset(
            o.getSuoritukset(), java.time.LocalDate.of(2024, 2, 22));
    assertEquals(2, suoritukset.size());

    o = new Oppija();
    o.getSuoritukset().add(peruskouluSuoritusKeskenDeadlinenJalkeen);
    suoritukset =
        hakemuksetConverterUtil.filterKeskenDeadlinenJalkeenSuoritukset(
            o.getSuoritukset(), java.time.LocalDate.of(2024, 2, 22));
    assertEquals(0, suoritukset.size());
  }

  @Test
  public void suodattaaPoisHakukaudenUlkopuolellaKeskenOlleetPeruskoulunSuoritukset() {
    HakemusDTO h = new HakemusDTO();
    Oppija o = new Oppija();
    o.getSuoritukset().add(vahvistettuPerusopetusKeskenHakukaudella);
    o.getSuoritukset().add(vahvistettuPerusopetusKeskenEiHakukaudella);
    List<SuoritusJaArvosanat> suoritukset =
        hakemuksetConverterUtil.filterUnrelevantSuoritukset(haku, h, o.getSuoritukset());
    assertEquals(1, suoritukset.size());
    assertEquals(vahvistettuPerusopetusKeskenHakukaudella, suoritukset.get(0));
  }

  @Test
  public void suodattaaPoisHakukaudenUlkopuolellaKeskeytyneetPeruskoulunSuoritukset() {
    HakemusDTO h = new HakemusDTO();
    Oppija o = new Oppija();
    o.getSuoritukset().add(vahvistettuPerusopetusKeskeytynytEiHakukaudella);
    o.getSuoritukset().add(vahvistettuPerusopetusKeskeytynytHakukaudella);
    List<SuoritusJaArvosanat> suoritukset =
        hakemuksetConverterUtil.filterUnrelevantSuoritukset(haku, h, o.getSuoritukset());
    assertEquals(1, suoritukset.size());
    assertEquals(vahvistettuPerusopetusKeskeytynytHakukaudella, suoritukset.get(0));
  }

  @Test
  public void suodattaaPoisKeskeytyneetLukionSuoritukset() {
    HakemusDTO h = new HakemusDTO();
    Oppija o = new Oppija();
    o.getSuoritukset().add(vahvistamatonLukioKeskeytynytEiHakukaudella);
    o.getSuoritukset().add(vahvistamatonLukioKeskeytynytHakukaudella);
    List<SuoritusJaArvosanat> suoritukset =
        hakemuksetConverterUtil.filterUnrelevantSuoritukset(haku, h, o.getSuoritukset());
    assertEquals(0, suoritukset.size());
  }

  @Test
  public void suodattaaPoisVahvistamattomatLisäpistekoulutusSuoritukset() {
    HakemusDTO h = new HakemusDTO();
    Oppija o =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setKymppiluokka()
            .setVahvistettu(false)
            .setValmistuminen(HAKUKAUDELLA)
            .setValmis()
            .build()
            .suoritus()
            .setAmmattistartti()
            .setVahvistettu(false)
            .setValmistuminen(HAKUKAUDELLA)
            .setValmis()
            .build()
            .suoritus()
            .setAmmatilliseenValmistava()
            .setVahvistettu(false)
            .setValmistuminen(HAKUKAUDELLA)
            .setValmis()
            .build()
            .suoritus()
            .setLukioonValmistava()
            .setVahvistettu(false)
            .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA)
            .setValmis()
            .build()
            .suoritus()
            .setLisaopetusTalous()
            .setVahvistettu(false)
            .setValmistuminen(HAKUKAUDELLA)
            .setValmis()
            .build()
            .suoritus()
            .setValmentava()
            .setVahvistettu(false)
            .setValmistuminen(HAKUKAUDELLA)
            .setValmis()
            .build()
            .build();
    List<SuoritusJaArvosanat> suoritukset =
        hakemuksetConverterUtil.filterUnrelevantSuoritukset(haku, h, o.getSuoritukset());
    assertTrue(suoritukset.isEmpty());
  }

  @Test
  public void suodattaaPoisKeskenjaKeskeytynytTilaisetYOSuoritukset() {
    HakemusDTO h = new HakemusDTO();
    Oppija o = new Oppija();
    o.getSuoritukset().add(vahvistettuYOKeskenHakukaudella);
    o.getSuoritukset().add(vahvistettuYOKeskeytynytHakukaudella);
    List<SuoritusJaArvosanat> suoritukset =
        hakemuksetConverterUtil.filterUnrelevantSuoritukset(haku, h, o.getSuoritukset());
    assertTrue(suoritukset.isEmpty());
  }

  @Test
  public void keskeytynytPohjakoulutusJosEiSuorituksia() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset = new ArrayList<>();
    Assertions.assertEquals(
        PohjakoulutusToinenAste.KESKEYTYNYT,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void
      pohjakoulutusLukioJosHakemuksellaLukioJaLukioSuoritusKeskeytynytHakukaudellaJaLeikkuriPvmOnTulevaisuudessa() {
    hakemuksetConverterUtil =
        new HakemuksetConverterUtil("9999-12-31", "9999-12-31", harkinnanvaraisuusAsyncResource);
    HakemusDTO h = new HakemusDTO();
    h.setHakijaOid("1.2.3.4.5.6");
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YLIOPPILAS));
            this.add(new AvainArvoDTO("lukioPaattotodistusVuosi", "2015"));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistamatonLukioKeskeytynytHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.YLIOPPILAS,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void
      pohjakoulutusPeruskouluJosHakemuksellaLukioJaSuressaEiValmistaSuoritustaJaLeikkuriPvmOnMenneisyydessaJaAbiturientti() {
    hakemuksetConverterUtil =
        new HakemuksetConverterUtil("1970-01-01", "1970-01-01", harkinnanvaraisuusAsyncResource);
    HakemusDTO h = new HakemusDTO();
    h.setHakijaOid("1.2.3.4.5.6");
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YLIOPPILAS));
            this.add(new AvainArvoDTO("lukioPaattotodistusVuosi", "2015"));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.PERUSKOULU,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void
      pohjakoulutusLukioJosHakemuksellaLukioJaSuressaEiValmistaSuoritustaJaLeikkuriPvmOnMenneisyydessaJaEIAbiturientti() {
    hakemuksetConverterUtil =
        new HakemuksetConverterUtil("1970-01-01", "1970-01-01", harkinnanvaraisuusAsyncResource);
    HakemusDTO h = new HakemusDTO();
    h.setHakijaOid("1.2.3.4.5.6");
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YLIOPPILAS));
            this.add(new AvainArvoDTO("lukioPaattotodistusVuosi", "2014"));
          }
        });
    Assertions.assertEquals(
        PohjakoulutusToinenAste.YLIOPPILAS,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, new ArrayList<>()).get());
  }

  @Test
  public void
      pohjakoulutusHeittaaPoikkeuksenJosHakemuksellaLukioJaSuressaEiValmistaSuoritustaJaLeikkuriPvmOnMenneisyydessa() {
    hakemuksetConverterUtil =
        new HakemuksetConverterUtil("1970-01-01", "1970-01-01", harkinnanvaraisuusAsyncResource);
    HakemusDTO h = new HakemusDTO();
    h.setHakijaOid("1.2.3.4.5.6");
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YLIOPPILAS));
          }
        });
    Assertions.assertThrows(
        RuntimeException.class,
        () -> hakemuksetConverterUtil.pohjakoulutus(haku, h, new ArrayList<>()));
  }

  @Test
  public void
      pohjakoulutusLukioJosHakemuksellaLukioJaEIAbiturienttiJaSuressaEiValmistaSuoritusta() {
    HakemusDTO h = new HakemusDTO();
    h.setHakijaOid("1.2.3.4.5.6");
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YLIOPPILAS));
            this.add(new AvainArvoDTO("lukioPaattotodistusVuosi", "2014"));
          }
        });
    Assertions.assertEquals(
        PohjakoulutusToinenAste.YLIOPPILAS,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, new ArrayList<>()).get());
  }

  @Test
  public void pohjakoulutusLukioJosLukionValmisSuoritus() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistamatonLukioValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.YLIOPPILAS,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void pohjakoulutusLukioJosVahvistettuYOSuoritus() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuYOValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.YLIOPPILAS,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void
      pohjakoulutusPeruskouluKeskeytynytJosVahvistettuPeruskoulunSuoritusKeskeytynytHakukaudella() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusKeskeytynytHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.KESKEYTYNYT,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void
      pohjakoulutusPeruskouluKeskeytynytJosVahvistettuPeruskoulunSuoritusKeskeytynytHakukaudellaAtaruhakemus() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("base-education-2nd", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusKeskeytynytHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.KESKEYTYNYT,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void
      pohjakoulutusPeruskouluJosSekaVahvistettuPeruskoulunSuoritusKeskeytynytEttaKeskenHakukaudella() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusKeskeytynytHakukaudella);
            add(vahvistettuPerusopetusKeskenHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.PERUSKOULU,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void
      automaticPreferenceDiscretionarySetJosVahvistettuPeruskoulunSuoritusKeskeytynytHakukaudella() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    h.setHakukohteet(Arrays.asList(new HakukohdeDTO(), new HakukohdeDTO()));
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusKeskeytynytHakukaudella);
          }
        };
    Map<String, String> answers =
        hakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset, currentDateFixedForTests);
    Assertions.assertEquals("true", answers.get("preference1-discretionary"));
    Assertions.assertEquals(
        "todistustenpuuttuminen", answers.get("preference1-discretionary-follow-up"));
    Assertions.assertEquals("true", answers.get("preference2-discretionary"));
    Assertions.assertEquals(
        "todistustenpuuttuminen", answers.get("preference2-discretionary-follow-up"));
    Assertions.assertFalse(answers.containsKey("preference3-discretionary"));
  }

  @Test
  public void pohjakoulutusLukioJosHakemuksellaLukioJaLoytyyValmisLukiosuoritus() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YLIOPPILAS));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusValmisHakukaudella);
            add(vahvistettuLukioValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.YLIOPPILAS,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void pohjakoulutusPeruskouluJosVahvistettuPeruskoulunSuoritusValmis() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.PERUSKOULU,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void
      pohjakoulutusYksilollistettyPeruskouluJosYksilollistettyVahvistettuPeruskoulunSuoritusValmis() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YKSILOLLISTETTY));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuYksilollistettyPerusopetusValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.YKSILOLLISTETTY,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void
      pohjakoulutusOsittainYksilollistettyPeruskouluJosYksilollistettyVahvistettuPeruskoulunSuoritusValmis() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(
                new AvainArvoDTO(
                    "POHJAKOULUTUS", PohjakoulutusToinenAste.OSITTAIN_YKSILOLLISTETTY));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuOsittainYksilollistettyPerusopetusValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.OSITTAIN_YKSILOLLISTETTY,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void
      pohjakoulutusAlueittainYksilollistettyPeruskouluJosYksilollistettyVahvistettuPeruskoulunSuoritusValmis() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(
                new AvainArvoDTO(
                    "POHJAKOULUTUS", PohjakoulutusToinenAste.ALUEITTAIN_YKSILOLLISTETTY));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuAlueittainYksilollistettyPerusopetusValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.ALUEITTAIN_YKSILOLLISTETTY,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void pohjakoulutusPeruskouluSurestaVaikkaHakemuksenPohjakoulutusEiVastaaSurenSuoritusta() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuAlueittainYksilollistettyPerusopetusValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.ALUEITTAIN_YKSILOLLISTETTY,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void pohjakoulutusHakemukseltaJosHakemuksellaPeruskouluJaVahvistettuUlkomainenSuoritus() {
    HakemusDTO h = new HakemusDTO();
    h.setHakijaOid("1.2.3.4.5.6");
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuUlkomainenValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.PERUSKOULU,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void pohjakoulutusUlkomainenJosVahvistettuUlkomainenSuoritusTaiHakemuksellaUlkomainen() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(
                new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO));
          }
        });
    List<SuoritusJaArvosanat> suoritukset = new ArrayList<>();
    Assertions.assertEquals(
        PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    suoritukset.add(vahvistettuUlkomainenValmisHakukaudella);
    Assertions.assertEquals(
        PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void
      pohjakoulutusVahvistamatonYksilollistettuPeruskouluJosVahvistettuaPeruskoulunSuoritusEiOleSuressa() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistamatonYksilollistettyPerusopetusValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.YKSILOLLISTETTY,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void
      pohjakoulutusVahvistamatonOsittainYksilollistettyPeruskouluJosVahvistettuaPeruskoulunSuoritusEiOleSuressa() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistamatonOsittainYksilollistettyPerusopetusValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.OSITTAIN_YKSILOLLISTETTY,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void
      pohjakoulutusVahvistamatonAlueittainYksilollistettuPeruskouluJosVahvistettuaPeruskoulunSuoritusEiOleSuressa() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistamatonAlueittainYksilollistettyPerusopetusValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.ALUEITTAIN_YKSILOLLISTETTY,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void pohjakoulutusPeruskouluJosVahvistettuaPeruskoulunSuoritusOnSuressa() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistamatonYksilollistettyPerusopetusValmisHakukaudella);
            add(vahvistettuPerusopetusValmisHakukaudella);
          }
        };
    Assertions.assertEquals(
        PohjakoulutusToinenAste.PERUSKOULU,
        hakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
  }

  @Test
  public void valmaTelmaLisapistekoulutuksetJosValmisPerusopetus() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusValmisHakukaudella);
            add(vahvistettuTelmaValmisHakukaudella);
            add(vahvistettuValmaValmisHakukaudella);
          }
        };
    Map<String, String> oletettu = new HashMap<>();

    Arrays.stream(Lisapistekoulutus.values()).forEach(lpk -> oletettu.put(lpk.name(), "false"));

    Lisapistekoulutus[] oletetutLisapisteet = {
      Lisapistekoulutus.LISAKOULUTUS_TELMA, Lisapistekoulutus.LISAKOULUTUS_VALMA
    };
    Arrays.stream(oletetutLisapisteet).forEach(lpk -> oletettu.put(lpk.name(), "true"));

    oletettu.put("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU);
    oletettu.put("PK_TILA", "true");
    oletettu.put("AM_TILA", "false");
    oletettu.put("LK_TILA", "false");
    oletettu.put("YO_TILA", "false");
    oletettu.put("PK_PAATTOTODISTUSVUOSI", "2015");
    oletettu.put("PK_SUORITUSVUOSI", "2015");
    oletettu.put("PK_SUORITUSLUKUKAUSI", "2");
    oletettu.put(HakemuksetConverterUtil.VALMA_SUORITUSVUOSI_KEY, "2015");
    Assertions.assertEquals(
        oletettu,
        hakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset, currentDateFixedForTests));
  }

  @Test
  public void tuvaLisapistekoulutuksetJosValmisPerusopetus() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusValmisHakukaudella);
            add(vahvistettuTuvaValmisHakukaudella);
          }
        };
    Map<String, String> oletettu = new HashMap<>();

    Arrays.stream(Lisapistekoulutus.values()).forEach(lpk -> oletettu.put(lpk.name(), "false"));

    Lisapistekoulutus[] oletetutLisapisteet = {Lisapistekoulutus.LISAKOULUTUS_TUVA};
    Arrays.stream(oletetutLisapisteet).forEach(lpk -> oletettu.put(lpk.name(), "true"));

    oletettu.put("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU);
    oletettu.put("PK_TILA", "true");
    oletettu.put("AM_TILA", "false");
    oletettu.put("LK_TILA", "false");
    oletettu.put("YO_TILA", "false");
    oletettu.put("PK_PAATTOTODISTUSVUOSI", "2015");
    oletettu.put("PK_SUORITUSVUOSI", "2015");
    oletettu.put("PK_SUORITUSLUKUKAUSI", "2");
    oletettu.put(HakemuksetConverterUtil.TUVA_SUORITUSVUOSI_KEY, "2015");
    Assertions.assertEquals(
        oletettu,
        hakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset, currentDateFixedForTests));
  }

  @Test
  public void negatiivinenValmaTulosSuoritusrekisteristaAjaaYliHakemuksenTiedot() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            this.add(new AvainArvoDTO("LISAKOULUTUS_VALMA", "true"));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusValmisAiemmin);
            add(
                new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setValma()
                    .setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA)
                    .setKeskeytynyt()
                    .done());
          }
        };
    assertThat(
        hakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset, currentDateFixedForTests),
        new IsMapContaining<>(equalTo("LISAKOULUTUS_VALMA"), equalTo("false")));
  }

  @Test
  public void
      negatiivinenValmaTulosSuoritusrekisteristaEdelliseltaVuodeltaEiAjaYliHakemuksenTietoja() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            this.add(new AvainArvoDTO("LISAKOULUTUS_VALMA", "true"));
          }
        });
    String edellisenaVuonna = "1.1.2014";
    Assertions.assertEquals(
        "1.1.2015", HAKUKAUDELLA); // edellisenaVuonna needs to be fixed if HAKUKAUDELLA changes

    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusValmisAiemmin);
            add(
                new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setValma()
                    .setVahvistettu(true)
                    .setValmistuminen(edellisenaVuonna)
                    .setKeskeytynyt()
                    .done());
          }
        };
    assertThat(
        hakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset, currentDateFixedForTests),
        new IsMapContaining<>(equalTo("LISAKOULUTUS_VALMA"), equalTo("true")));
  }

  @Test
  public void positiivinenValmaTulosSuoritusrekisteristaHuomioidaanHakukaudelta() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(
                new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setValma()
                    .setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA)
                    .setKesken()
                    .done());
            add(vahvistettuPerusopetusValmisAiemmin);
          }
        };
    assertThat(
        hakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset, currentDateFixedForTests),
        new IsMapContaining<>(equalTo("LISAKOULUTUS_VALMA"), equalTo("true")));
  }

  @Test
  public void positiivistaValmaTulostaSuoritusrekisteristaEiHuomioidaEdelliseltaVuodelta() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    String edellisenaVuonna = "1.1.2014";
    Assertions.assertEquals(
        "1.1.2015", HAKUKAUDELLA); // edellisenaVuonna needs to be fixed if HAKUKAUDELLA changes
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusValmisAiemmin);
            add(
                new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setValma()
                    .setVahvistettu(true)
                    .setValmistuminen(edellisenaVuonna)
                    .setKesken()
                    .done());
          }
        };
    assertThat(
        hakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset, currentDateFixedForTests),
        new IsMapContaining<>(equalTo("LISAKOULUTUS_VALMA"), equalTo("false")));
  }

  @Test
  public void eiLisapistekoulutuksiaJosKeskeytynytPerusopetusTaiLukioTaiUlkomainenSuoritus() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusKeskeytynytHakukaudella);
            add(vahvistettuKymppiValmisHakukaudella);
          }
        };
    Map<String, String> oletettu = new HashMap<>();
    Arrays.stream(Lisapistekoulutus.values()).forEach(lpk -> oletettu.put(lpk.name(), "false"));
    oletettu.put("POHJAKOULUTUS", PohjakoulutusToinenAste.KESKEYTYNYT);
    oletettu.put("PK_TILA", "false");
    oletettu.put("AM_TILA", "false");
    oletettu.put("LK_TILA", "false");
    oletettu.put("YO_TILA", "false");
    Assertions.assertEquals(
        oletettu,
        hakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset, currentDateFixedForTests));
  }

  @Test
  public void eiLisapistekoulutustaHakemukseltaJosKeskeytynytSuoritus() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            add(new AvainArvoDTO(Lisapistekoulutus.LISAKOULUTUS_KYMPPI.name(), "true"));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusValmisAiemmin);
            add(vahvistettuKymppiKeskeytynytHakukaudella);
          }
        };
    Map<String, String> oletettu = new HashMap<>();
    oletettu.put("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU);
    oletettu.put("PK_TILA", "false");
    oletettu.put("AM_TILA", "false");
    oletettu.put("LK_TILA", "false");
    oletettu.put("YO_TILA", "false");
    Arrays.stream(Lisapistekoulutus.values()).forEach(lpk -> oletettu.put(lpk.name(), "false"));
    Assertions.assertEquals(
        oletettu,
        hakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset, java.time.LocalDate.now()));
  }

  @Test
  public void lisapistekoulutusHakemukseltaJosEiKeskeytynyttaSuoritus() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            add(new AvainArvoDTO(Lisapistekoulutus.LISAKOULUTUS_OPISTOVUOSI.name(), "true"));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusValmisAiemmin);
          }
        };
    Map<String, String> oletettu = new HashMap<>();
    oletettu.put("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU);
    oletettu.put("PK_TILA", "false");
    oletettu.put("AM_TILA", "false");
    oletettu.put("LK_TILA", "false");
    oletettu.put("YO_TILA", "false");
    Arrays.stream(Lisapistekoulutus.values()).forEach(lpk -> oletettu.put(lpk.name(), "false"));
    oletettu.put(Lisapistekoulutus.LISAKOULUTUS_OPISTOVUOSI.name(), "true");
    Assertions.assertEquals(
        oletettu,
        hakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset, currentDateFixedForTests));
  }

  @Test
  public void lisapistekoulutusHakemukseltaJosKeskeytynytSuoritusEiHakukaudella() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            add(new AvainArvoDTO(Lisapistekoulutus.LISAKOULUTUS_OPISTOVUOSI.name(), "true"));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuOpistovuosiKeskeytynytEiHakukaudella);
            add(vahvistettuPerusopetusValmisAiemmin);
          }
        };
    Map<String, String> oletettu = new HashMap<>();
    oletettu.put("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU);
    oletettu.put("PK_TILA", "false");
    oletettu.put("AM_TILA", "false");
    oletettu.put("LK_TILA", "false");
    oletettu.put("YO_TILA", "false");
    Arrays.stream(Lisapistekoulutus.values()).forEach(lpk -> oletettu.put(lpk.name(), "false"));
    oletettu.put(Lisapistekoulutus.LISAKOULUTUS_OPISTOVUOSI.name(), "true");
    Assertions.assertEquals(
        oletettu,
        hakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset, currentDateFixedForTests));
  }

  @Test
  public void suoritusVuosiJaKausiJosPeruskoulunValmistunutSuoritus() {
    HakemusDTO h = new HakemusDTO();
    h.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
          }
        });
    List<SuoritusJaArvosanat> suoritukset =
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusValmisHakukaudella);
          }
        };
    Map<String, String> oletettu = new HashMap<>();
    oletettu.put("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU);
    oletettu.put("PK_TILA", "true");
    oletettu.put("AM_TILA", "false");
    oletettu.put("LK_TILA", "false");
    oletettu.put("YO_TILA", "false");
    Arrays.stream(Lisapistekoulutus.values()).forEach(lpk -> oletettu.put(lpk.name(), "false"));
    oletettu.put("PK_PAATTOTODISTUSVUOSI", "2015");
    oletettu.put("PK_SUORITUSVUOSI", "2015");
    oletettu.put("PK_SUORITUSLUKUKAUSI", "2");
    Assertions.assertEquals(
        oletettu,
        hakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset, currentDateFixedForTests));
  }

  @Test
  public void preferoiArvosanaaYliSuoritusmerkinnan() {
    ParametritDTO parametrit = SuoritusrekisteriSpec.laskennanalkamisparametri(new DateTime());
    HakemusDTO hakemus = new HakemusDTO();
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setVahvistettu(false)
            .setValmistuminen("1.1.2014")
            .setValmis()
            .arvosana()
            .setAine("AI")
            .setArvosana("S")
            .setAsteikko_4_10()
            .build()
            .build()
            .suoritus()
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .arvosana()
            .setAine("AI")
            .setArvosana("7")
            .setAsteikko_4_10()
            .build()
            .setValmis()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", parametrit, new HashMap<>(), oppija, hakemus, true);
    final String pk_arvosana = firstHakemusArvo(hakemus, "PK_AI").get();
    assertEquals("7", pk_arvosana);
  }

  @Test
  public void pohjakoulutusSurenPerusteellaJosYOSuoritusKeskeytynytHakukaudella() {
    HakemusDTO h = new HakemusDTO();
    h.setHakijaOid("1.2.3.4.5.6");
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            this.add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2015"));
          }
        });
    Oppija o = new Oppija();
    o.setSuoritukset(
        new ArrayList<>() {
          {
            add(vahvistettuPerusopetusValmisAiemmin);
            add(vahvistettuYOKeskeytynytHakukaudella);
          }
        });
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), o, h, true);
    Assertions.assertEquals(
        PohjakoulutusToinenAste.PERUSKOULU, getFirstHakemusArvo(h, "POHJAKOULUTUS"));
    Assertions.assertEquals("2015", getFirstHakemusArvo(h, "PK_PAATTOTODISTUSVUOSI"));
  }

  @Test
  public void pohjakoulutusHakemukseltaJosYOSuoritusKeskenHakukaudella() {
    HakemusDTO h = new HakemusDTO();
    h.setHakijaOid("1.2.3.4.5.6");
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YKSILOLLISTETTY));
            this.add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2015"));
          }
        });
    Oppija o = new Oppija();
    o.setSuoritukset(
        new ArrayList<>() {
          {
            add(vahvistamatonYksilollistettuPerusopetusValmisHakukaudella);
            add(vahvistettuYOKeskenHakukaudella);
          }
        });
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), o, h, true);
    Assertions.assertEquals(
        PohjakoulutusToinenAste.YKSILOLLISTETTY, getFirstHakemusArvo(h, "POHJAKOULUTUS"));
    Assertions.assertEquals("2015", getFirstHakemusArvo(h, "PK_PAATTOTODISTUSVUOSI"));
  }

  @Test
  public void pohjakoulutusHakemukseltaJosVainLisapistekoulutusVahvistettuValmis() {
    HakemusDTO h = new HakemusDTO();
    h.setHakijaOid("1.2.3.4.5.6");
    h.setAvaimet(
        new ArrayList<>() {
          {
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            this.add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2012"));
            this.add(
                new AvainArvoDTO(Lisapistekoulutus.LISAKOULUTUS_AMMATTISTARTTI.name(), "true"));
          }
        });
    Oppija o = new Oppija();
    o.setSuoritukset(
        new ArrayList<>() {
          {
            add(vahvistettuAmmattistarttiValmisHakukaudella);
            add(vahvistamatonPerusopetusValmisEiHakukaudella);
          }
        });
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), o, h, true);
    Assertions.assertEquals(
        PohjakoulutusToinenAste.PERUSKOULU, getFirstHakemusArvo(h, "POHJAKOULUTUS"));
    Assertions.assertEquals("2012", getFirstHakemusArvo(h, "PK_PAATTOTODISTUSVUOSI"));
    Assertions.assertEquals(
        "true", getFirstHakemusArvo(h, Lisapistekoulutus.LISAKOULUTUS_AMMATTISTARTTI.name()));
  }

  @Test
  public void suurinArvosanaValitaanYliUseanSuorituksen() {
    { // Uudempi
      HakemusDTO hakemus = new HakemusDTO();
      hakemus.setAvaimet(
          new ArrayList<>() {
            {
              add(new AvainArvoDTO("PK_BI_VAL1", "7"));
            }
          });
      Oppija oppija =
          new SuoritusrekisteriSpec.OppijaBuilder()
              .suoritus()
              .setPerusopetus()
              .setVahvistettu(false)
              .setValmistuminen("1.1.2014")
              .setValmis()
              .arvosana()
              .setAine("AI")
              .setArvosana("6")
              .setAsteikko_4_10()
              .build()
              .build()
              .suoritus()
              .setPerusopetus()
              .setVahvistettu(true)
              .setValmistuminen("1.1.2015")
              .setValmis()
              .arvosana()
              .setAine("AI")
              .setArvosana("7")
              .setAsteikko_4_10()
              .build()
              .arvosana()
              .setAine("BI")
              .setValinnainen()
              .setJarjestys(1)
              .setArvosana("8")
              .setAsteikko_4_10()
              .build()
              .build()
              .suoritus()
              .setKymppiluokka()
              .setVahvistettu(true)
              .setValmistuminen("1.1.2015")
              .setValmis()
              .arvosana()
              .setAine("BI")
              .setValinnainen()
              .setJarjestys(1)
              .setArvosana("9")
              .setAsteikko_4_10()
              .build()
              .build()
              .build();

      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
      final String pk_arvosana = firstHakemusArvo(hakemus, "PK_AI").get();
      final String pk_val_arvosana = firstHakemusArvo(hakemus, "PK_BI_VAL1").get();
      assertEquals("7", pk_arvosana);
      assertEquals("9", pk_val_arvosana);
      assertFalse(hakemus.getAvaimet().stream().anyMatch(a -> a.getAvain().equals("PK_BI_VAL2")));
      assertFalse(hakemus.getAvaimet().stream().anyMatch(a -> a.getAvain().equals("PK_BI")));
    }
    {
      HakemusDTO hakemus = new HakemusDTO();
      Oppija oppija =
          new SuoritusrekisteriSpec.OppijaBuilder()
              .suoritus()
              .setPerusopetus()
              .setVahvistettu(true)
              .setValmistuminen("1.1.2014")
              .setValmis()
              .arvosana()
              .setAine("AI")
              .setArvosana("7")
              .setAsteikko_4_10()
              .build()
              .build()
              .suoritus()
              .setPerusopetus()
              .setVahvistettu(true)
              .setValmistuminen("1.1.2015")
              .arvosana()
              .setAine("AI")
              .setArvosana("6")
              .setAsteikko_4_10()
              .build()
              .setValmis()
              .build()
              .build();

      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
      final String pk_arvosana = firstHakemusArvo(hakemus, "PK_AI").get();
      assertEquals("7", pk_arvosana);
    }
    { // Valmistumaton kymppiluokka otetaan huomioon
      HakemusDTO hakemus = new HakemusDTO();
      Oppija oppija =
          new SuoritusrekisteriSpec.OppijaBuilder()
              .suoritus()
              .setPerusopetus()
              .setVahvistettu(false)
              .setValmistuminen("1.1.2014")
              .setValmis()
              .arvosana()
              .setAine("AI")
              .setArvosana("7")
              .setAsteikko_4_10()
              .build()
              .build()
              .suoritus()
              .setKymppiluokka()
              .setVahvistettu(true)
              .setValmistuminen("1.1.2015")
              .arvosana()
              .setAine("AI")
              .setArvosana("8")
              .setAsteikko_4_10()
              .build()
              .setKesken()
              .build()
              .build();

      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
      final String pk_arvosana =
          hakemus.getAvaimet().stream()
              .filter(a -> a.getAvain().equals("PK_AI"))
              .map(a -> a.getArvo())
              .findFirst()
              .get();
      assertEquals("8", pk_arvosana);
    }
    { // Vanhempi kymppiluokka otetaan huomioon
      HakemusDTO hakemus = new HakemusDTO();
      Oppija oppija =
          new SuoritusrekisteriSpec.OppijaBuilder()
              .suoritus()
              .setPerusopetus()
              .setVahvistettu(false)
              .setValmistuminen("1.1.2015")
              .setValmis()
              .arvosana()
              .setAine("AI")
              .setArvosana("7")
              .setAsteikko_4_10()
              .build()
              .build()
              .suoritus()
              .setKymppiluokka()
              .setVahvistettu(true)
              .setValmistuminen("1.1.2013")
              .arvosana()
              .setAine("AI")
              .setArvosana("8")
              .setAsteikko_4_10()
              .build()
              .setKesken()
              .build()
              .build();

      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
      final String pk_arvosana =
          hakemus.getAvaimet().stream()
              .filter(a -> a.getAvain().equals("PK_AI"))
              .map(a -> a.getArvo())
              .findFirst()
              .get();
      Assertions.assertEquals("8", pk_arvosana);
    }
  }

  @Test
  public void vainItseIlmoitettu() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2015"));
          }
        });
    Oppija oppija = new Oppija();
    oppija.setEnsikertalainen(true);

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
  }

  @Test
  public void itseIlmoitettuJaValmis() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2014"));
            add(new AvainArvoDTO(hakemuksetConverterUtil.PERUSOPETUS_KIELI, "FI"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("SV")
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_SUORITUSVUOSI"));
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
    assertEquals("SV", getFirstHakemusArvo(hakemus, hakemuksetConverterUtil.PERUSOPETUS_KIELI));
  }

  @Test
  public void hakemukseltaKopioituJaValmisPk() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2014"));
            add(new AvainArvoDTO(hakemuksetConverterUtil.PERUSOPETUS_KIELI, "FI"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setPerusopetus()
            .setVahvistettu(false)
            .setMyontaja(HAKEMUS1_OID)
            .setValmistuminen("1.1.2014")
            .setValmis()
            .build()
            .suoritus()
            .setSuoritusKieli("SV")
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_SUORITUSVUOSI"));
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
    assertEquals("SV", getFirstHakemusArvo(hakemus, hakemuksetConverterUtil.PERUSOPETUS_KIELI));
  }

  @Test
  public void arvosanatAtaruhakemukselta() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO(hakemuksetConverterUtil.ATARU_POHJAKOULUTUS_VUOSI, "2009"));
            add(new AvainArvoDTO("arvosana-KO_group0", "arvosana-KO-8"));
            add(new AvainArvoDTO("arvosana-FY_group0", "arvosana-FY-7"));
            add(new AvainArvoDTO("arvosana-TT_group0", "arvosana-TT-8"));
            add(new AvainArvoDTO("arvosana-MA_group0", "arvosana-MA-6"));
            add(new AvainArvoDTO("arvosana-B1_group0", "arvosana-B1-6"));
            add(new AvainArvoDTO("arvosana-BI_group0", "arvosana-BI-9"));
            add(new AvainArvoDTO("arvosana-A_group0", "arvosana-A-10"));
            add(new AvainArvoDTO("arvosana-A_group1", "arvosana-A-9"));
            add(new AvainArvoDTO("arvosana-KO_group0", "arvosana-KO-8"));
            add(new AvainArvoDTO("arvosana-KO_group0", "arvosana-KO-8"));
            add(new AvainArvoDTO("arvosana-KO_group0", "arvosana-KO-8"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setLukio()
            .setVahvistettu(false)
            .setMyontaja(HAKEMUS1_OID)
            .setValmistuminen("1.1.2014")
            .setKesken()
            .build()
            .build();
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
    assertEquals("10", firstHakemusArvo(hakemus, "PK_AI").orElse("notFound"));
    assertEquals("9", firstHakemusArvo(hakemus, "PK_AI_VAL1").orElse("notFound"));
    assertEquals("8", firstHakemusArvo(hakemus, "PK_TT").orElse("notFound"));
  }

  @Test
  public void oppiaineetAtaruhakemukselta() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    Map<String, Exception> errors = new HashMap<>();
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO(hakemuksetConverterUtil.ATARU_POHJAKOULUTUS_VUOSI, "2009"));
            add(new AvainArvoDTO("oppimaara-kieli-B1_group0", "SV"));
            add(new AvainArvoDTO("oppimaara-kieli-B1_group1", "SV"));
            add(new AvainArvoDTO("oppimaara-kieli-A1_group0", "JA"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setLukio()
            .setVahvistettu(false)
            .setMyontaja(HAKEMUS1_OID)
            .setValmistuminen("1.1.2014")
            .setKesken()
            .build()
            .build();
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), errors, oppija, hakemus, true);
    assertEquals("SV", firstHakemusArvo(hakemus, "PK_B1_OPPIAINE").orElse("notFound"));
    assertEquals("JA", firstHakemusArvo(hakemus, "PK_A1_OPPIAINE").orElse("notFound"));
    assertTrue(errors.isEmpty());
  }

  @Test
  public void b2KieliAtaruHakemukselta() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO(hakemuksetConverterUtil.ATARU_POHJAKOULUTUS_VUOSI, "2009"));
            add(
                new AvainArvoDTO(
                    "oppiaine-valinnainen-kieli_group0", "oppiaine-valinnainen-kieli-b2"));
            add(
                new AvainArvoDTO(
                    "arvosana-valinnainen-kieli_group0", "arvosana-valinnainen-kieli-7"));
            add(new AvainArvoDTO("oppimaara-kieli-valinnainen-kieli_group0", "FR"));
            add(
                new AvainArvoDTO(
                    "oppiaine-valinnainen-kieli_group1", "oppiaine-valinnainen-kieli-a2"));
            add(
                new AvainArvoDTO(
                    "arvosana-valinnainen-kieli_group1", "arvosana-valinnainen-kieli-8"));
            add(new AvainArvoDTO("oppimaara-kieli-valinnainen-kieli_group1", "EN"));
            add(
                new AvainArvoDTO(
                    "oppiaine-valinnainen-kieli_group2", "oppiaine-valinnainen-kieli-a2"));
            add(
                new AvainArvoDTO(
                    "arvosana-valinnainen-kieli_group2", "arvosana-valinnainen-kieli-9"));
            add(new AvainArvoDTO("oppimaara-kieli-valinnainen-kieli_group2", "DE"));
            add(
                new AvainArvoDTO(
                    "oppiaine-valinnainen-kieli_group3", "oppiaine-valinnainen-kieli-b2"));
            add(
                new AvainArvoDTO(
                    "arvosana-valinnainen-kieli_group3", "arvosana-valinnainen-kieli-HYVAKSYTTY"));
            add(new AvainArvoDTO("oppimaara-kieli-valinnainen-kieli_group3", "TU"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setLukio()
            .setVahvistettu(false)
            .setMyontaja(HAKEMUS1_OID)
            .setValmistuminen("1.1.2014")
            .setKesken()
            .build()
            .build();
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
    assertEquals("FR", firstHakemusArvo(hakemus, "PK_B2_OPPIAINE").orElse("notFound"));
    assertEquals("7", firstHakemusArvo(hakemus, "PK_B2").orElse("notFound"));
    assertEquals("EN", firstHakemusArvo(hakemus, "PK_A2_OPPIAINE").orElse("notFound"));
    assertEquals("DE", firstHakemusArvo(hakemus, "PK_A22_OPPIAINE").orElse("notFound"));
    assertEquals("8", firstHakemusArvo(hakemus, "PK_A2").orElse("notFound"));
    assertEquals("9", firstHakemusArvo(hakemus, "PK_A22").orElse("notFound"));
    assertEquals("notFound", firstHakemusArvo(hakemus, "PK_B22").orElse("notFound"));
  }

  @Test
  public void aidinkieliAtaruHakemukselta() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO(hakemuksetConverterUtil.ATARU_POHJAKOULUTUS_VUOSI, "2009"));
            add(new AvainArvoDTO("oppimaara-a_group0", "ruotsi-viittomakielisille"));
            add(new AvainArvoDTO("arvosana-A_group0", "arvosana-A-10"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setLukio()
            .setVahvistettu(false)
            .setMyontaja(HAKEMUS1_OID)
            .setValmistuminen("1.1.2014")
            .setKesken()
            .build()
            .build();
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
    assertEquals("SV_VK", firstHakemusArvo(hakemus, "PK_AI_OPPIAINE").orElse("notFound"));
    assertEquals("10", firstHakemusArvo(hakemus, "PK_AI").orElse("notFound"));
  }

  @Test
  public void suoritusvuosiAtaruhakemukselta() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO(hakemuksetConverterUtil.ATARU_POHJAKOULUTUS_VUOSI, "2019"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setLukio()
            .setVahvistettu(false)
            .setMyontaja(HAKEMUS1_OID)
            .setValmistuminen("1.1.2014")
            .setKesken()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
    assertEquals(
        "2019", getFirstHakemusArvo(hakemus, hakemuksetConverterUtil.PK_PAATTOTODISTUSVUOSI));
  }

  @Test
  public void useitaA1kieliaAtaruHakemuksella() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO(hakemuksetConverterUtil.ATARU_POHJAKOULUTUS_VUOSI, "2008"));
            add(
                new AvainArvoDTO(
                    "oppiaine-valinnainen-kieli_group0", "oppiaine-valinnainen-kieli-a1"));
            add(
                new AvainArvoDTO(
                    "arvosana-valinnainen-kieli_group0", "arvosana-valinnainen-kieli-7"));
            add(new AvainArvoDTO("oppimaara-kieli-valinnainen-kieli_group0", "EN"));
            add(
                new AvainArvoDTO(
                    "oppiaine-valinnainen-kieli_group1", "oppiaine-valinnainen-kieli-a1"));
            add(
                new AvainArvoDTO(
                    "arvosana-valinnainen-kieli_group1", "arvosana-valinnainen-kieli-10"));
            add(new AvainArvoDTO("oppimaara-kieli-valinnainen-kieli_group1", "FR"));
            add(new AvainArvoDTO("arvosana-A1_group0", "arvosana-A1-8"));
            add(new AvainArvoDTO("oppimaara-kieli-A1_group0", "FI"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setLukio()
            .setVahvistettu(false)
            .setMyontaja(HAKEMUS1_OID)
            .setValmistuminen("1.1.2014")
            .setKesken()
            .build()
            .build();
    Map<String, Exception> errors = new HashMap<>();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), errors, oppija, hakemus, true);
    assertTrue(errors.isEmpty());
    assertEquals(
        "2008", getFirstHakemusArvo(hakemus, hakemuksetConverterUtil.PK_PAATTOTODISTUSVUOSI));
    assertEquals("7", firstHakemusArvo(hakemus, "PK_A12").orElse("notFound"));
    assertEquals("8", firstHakemusArvo(hakemus, "PK_A1").orElse("notFound"));
    assertEquals("10", firstHakemusArvo(hakemus, "PK_A13").orElse("notFound"));
    assertEquals("FR", firstHakemusArvo(hakemus, "PK_A13_OPPIAINE").orElse("notFound"));
  }

  @Test
  public void useitaA2kieliaAtaruHakemuksella() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO(hakemuksetConverterUtil.ATARU_POHJAKOULUTUS_VUOSI, "2008"));
            add(
                new AvainArvoDTO(
                    "oppiaine-valinnainen-kieli_group0", "oppiaine-valinnainen-kieli-a2"));
            add(
                new AvainArvoDTO(
                    "arvosana-valinnainen-kieli_group0", "arvosana-valinnainen-kieli-9"));
            add(new AvainArvoDTO("oppimaara-kieli-valinnainen-kieli_group0", "FR"));
            add(
                new AvainArvoDTO(
                    "oppiaine-valinnainen-kieli_group1", "oppiaine-valinnainen-kieli-a2"));
            add(
                new AvainArvoDTO(
                    "arvosana-valinnainen-kieli_group1", "arvosana-valinnainen-kieli-8"));
            add(new AvainArvoDTO("oppimaara-kieli-valinnainen-kieli_group1", "DE"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setLukio()
            .setVahvistettu(false)
            .setMyontaja(HAKEMUS1_OID)
            .setValmistuminen("1.1.2014")
            .setKesken()
            .build()
            .build();
    Map<String, Exception> errors = new HashMap<>();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), errors, oppija, hakemus, true);
    assertTrue(errors.isEmpty());
    assertEquals(
        "2008", getFirstHakemusArvo(hakemus, hakemuksetConverterUtil.PK_PAATTOTODISTUSVUOSI));
    assertEquals("9", firstHakemusArvo(hakemus, "PK_A2").orElse("notFound"));
    assertEquals("8", firstHakemusArvo(hakemus, "PK_A22").orElse("notFound"));
    assertEquals("FR", firstHakemusArvo(hakemus, "PK_A2_OPPIAINE").orElse("notFound"));
    assertEquals("DE", firstHakemusArvo(hakemus, "PK_A22_OPPIAINE").orElse("notFound"));
  }

  @Test
  public void useitaB2kieliaAtaruHakemuksella() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO(hakemuksetConverterUtil.ATARU_POHJAKOULUTUS_VUOSI, "2008"));
            add(
                new AvainArvoDTO(
                    "oppiaine-valinnainen-kieli_group0", "oppiaine-valinnainen-kieli-b2"));
            add(
                new AvainArvoDTO(
                    "arvosana-valinnainen-kieli_group0", "arvosana-valinnainen-kieli-7"));
            add(new AvainArvoDTO("oppimaara-kieli-valinnainen-kieli_group0", "DE"));
            add(
                new AvainArvoDTO(
                    "oppiaine-valinnainen-kieli_group1", "oppiaine-valinnainen-kieli-b2"));
            add(
                new AvainArvoDTO(
                    "arvosana-valinnainen-kieli_group1", "arvosana-valinnainen-kieli-10"));
            add(new AvainArvoDTO("oppimaara-kieli-valinnainen-kieli_group1", "FR"));
            add(
                new AvainArvoDTO(
                    "oppiaine-valinnainen-kieli_group2", "oppiaine-valinnainen-kieli-b2"));
            add(
                new AvainArvoDTO(
                    "arvosana-valinnainen-kieli_group2", "arvosana-valinnainen-kieli-9"));
            add(new AvainArvoDTO("oppimaara-kieli-valinnainen-kieli_group2", "IT"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setLukio()
            .setVahvistettu(false)
            .setMyontaja(HAKEMUS1_OID)
            .setValmistuminen("1.1.2014")
            .setKesken()
            .build()
            .build();
    Map<String, Exception> errors = new HashMap<>();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), errors, oppija, hakemus, true);
    assertTrue(errors.isEmpty());
    assertEquals(
        "2008", getFirstHakemusArvo(hakemus, hakemuksetConverterUtil.PK_PAATTOTODISTUSVUOSI));
    assertEquals("7", firstHakemusArvo(hakemus, "PK_B2").orElse("notFound"));
    assertEquals("DE", firstHakemusArvo(hakemus, "PK_B2_OPPIAINE").orElse("notFound"));
    assertEquals("10", firstHakemusArvo(hakemus, "PK_B22").orElse("notFound"));
    assertEquals("FR", firstHakemusArvo(hakemus, "PK_B22_OPPIAINE").orElse("notFound"));
    assertEquals("9", firstHakemusArvo(hakemus, "PK_B23").orElse("notFound"));
    assertEquals("IT", firstHakemusArvo(hakemus, "PK_B23_OPPIAINE").orElse("notFound"));
  }

  @Test
  public void hakemukseltaKopioituMuokattuLukioKieli() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO(hakemuksetConverterUtil.LUKIO_KIELI, "FI"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("SV")
            .setLukio()
            .setVahvistettu(false)
            .setMyontaja(HAKEMUS1_OID)
            .setValmistuminen("1.1.2014")
            .setValmis()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
    assertEquals("SV", getFirstHakemusArvo(hakemus, hakemuksetConverterUtil.LUKIO_KIELI));
  }

  @Test
  public void keskenJaValmis() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2014"));
            add(new AvainArvoDTO(hakemuksetConverterUtil.PERUSOPETUS_KIELI, "FI"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setKesken()
            .build()
            .suoritus()
            .setSuoritusKieli("SV")
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_SUORITUSVUOSI"));
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
    assertEquals("SV", getFirstHakemusArvo(hakemus, hakemuksetConverterUtil.PERUSOPETUS_KIELI));
  }

  @Test
  public void kaytetaanUudempaaSuoritustaJosMuutenSamatTilat() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2014"));
            add(new AvainArvoDTO(hakemuksetConverterUtil.PERUSOPETUS_KIELI, "FI"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .build()
            .suoritus()
            .setSuoritusKieli("SV")
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.2.2015")
            .setValmis()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_SUORITUSVUOSI"));
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
    assertEquals("SV", getFirstHakemusArvo(hakemus, hakemuksetConverterUtil.PERUSOPETUS_KIELI));
  }

  @Test
  public void hakemukseltaTulevaHarkinnanvaraisuustietoLoytyyLahdearvoista() {

    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid("1.2.246.562.11.00001138888");
    String hakukohdeOid = "1.2.246.562.20.88759381234";
    HakemuksenHarkinnanvaraisuus hark =
        new HakemuksenHarkinnanvaraisuus(
            hakemus.getHakemusoid(),
            List.of(
                new HakutoiveenHarkinnanvaraisuus(
                    hakukohdeOid, HarkinnanvaraisuudenSyy.ATARU_YKS_MAT_AI)));

    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2014"));
            add(new AvainArvoDTO(hakemuksetConverterUtil.PERUSOPETUS_KIELI, "FI"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .build()
            .suoritus()
            .setSuoritusKieli("SV")
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.2.2015")
            .setValmis()
            .build()
            .build();

    when(harkinnanvaraisuusAsyncResource.hasYksilollistettyMatAi(hark, oppija)).thenReturn(true);

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        ataruKoutaToisenAsteenHaku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        oppija,
        hakemus,
        true,
        hark);
    System.out.println("hakemuksen avaimet" + hakemus.getAvaimet());
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_SUORITUSVUOSI"));
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
    assertEquals("true", getFirstHakemusArvo(hakemus, "yks_mat_ai"));

    assertEquals("SV", getFirstHakemusArvo(hakemus, hakemuksetConverterUtil.PERUSOPETUS_KIELI));
  }

  @Test
  public void yksilollistettyMatAiOnFalseJosHakemuksellaTaiSuorituksessaEiTietoa() {

    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid("1.2.246.562.11.00001138888");
    String hakukohdeOid = "1.2.246.562.20.88759381234";
    HakemuksenHarkinnanvaraisuus hark =
        new HakemuksenHarkinnanvaraisuus(
            hakemus.getHakemusoid(),
            List.of(
                new HakutoiveenHarkinnanvaraisuus(
                    hakukohdeOid, HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN)));

    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2014"));
            add(new AvainArvoDTO(hakemuksetConverterUtil.PERUSOPETUS_KIELI, "FI"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .build()
            .suoritus()
            .setSuoritusKieli("SV")
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.2.2015")
            .setValmis()
            .build()
            .build();

    when(harkinnanvaraisuusAsyncResource.hasYksilollistettyMatAi(hark, oppija)).thenReturn(false);

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        ataruKoutaToisenAsteenHaku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        oppija,
        hakemus,
        true,
        hark);
    System.out.println("hakemuksen avaimet" + hakemus.getAvaimet());
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_SUORITUSVUOSI"));
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
    assertEquals("false", getFirstHakemusArvo(hakemus, "yks_mat_ai"));
    assertEquals("SV", getFirstHakemusArvo(hakemus, hakemuksetConverterUtil.PERUSOPETUS_KIELI));
  }

  @Test
  public void yksMatAiAvaintaEiAsetetaKaikilleHauille() {

    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid("1.2.246.562.11.00001138888");
    String hakukohdeOid = "1.2.246.562.20.88759381234";

    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2014"));
            add(new AvainArvoDTO(hakemuksetConverterUtil.PERUSOPETUS_KIELI, "FI"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setSuoritusKieli("FI")
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .build()
            .suoritus()
            .setSuoritusKieli("SV")
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.2.2015")
            .setValmis()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
    System.out.println("hakemuksen avaimet" + hakemus.getAvaimet());
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_SUORITUSVUOSI"));
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
    assertFalse(hakemus.getAvaimet().stream().anyMatch(a -> "yks_mat_ai".equals(a.getAvain())));
    assertEquals("SV", getFirstHakemusArvo(hakemus, hakemuksetConverterUtil.PERUSOPETUS_KIELI));
  }

  @Test
  public void itseIlmoitettuJaKaksiValmista() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2013"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_SUORITUSVUOSI"));
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
  }

  @Test
  public void useampiSuoritusJoistaEiOllaKiinnostuneita() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija virheellinenKoskaUseampiSuoritus =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setKomo("koulutus_732101")
            .setValmistuminen("1.1.2015")
            .setKesken()
            .build()
            .suoritus()
            .setKomo("koulutus_732101")
            .setValmistuminen("1.1.2015")
            .setValmis()
            .build()
            .suoritus()
            .setKomo("koulutus_671101")
            .setValmistuminen("1.1.2015")
            .setKesken()
            .build()
            .suoritus()
            .setKomo("koulutus_671101")
            .setValmistuminen("1.1.2015")
            .setValmis()
            .build()
            .build();
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        virheellinenKoskaUseampiSuoritus,
        hakemus,
        true);
  }

  @Test
  public void perusopetusOikeinKoskaVainValmisSuoritusJaKeskeytyneitaSuorituksia() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija oikeinKoskaVainValmisSuoritusJaKeskeytyneitaSuorituksia =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setValmistuminen("1.1.2015")
            .setKeskeytynyt()
            .build()
            .suoritus()
            .setPerusopetus()
            .setValmistuminen("1.1.2015")
            .setValmis()
            .build()
            .suoritus()
            .setPerusopetus()
            .setValmistuminen("1.1.2015")
            .setKeskeytynyt()
            .build()
            .build();
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        oikeinKoskaVainValmisSuoritusJaKeskeytyneitaSuorituksia,
        hakemus,
        true);
  }

  @Test
  public void vainHaunHakukaudenPerusopetusSuoritusvuosiJaSuorituskausi() {
    /*
    PK_SUORITUSLUKUKAUSI = 1/2
    1.1. - 31.7. ->  2
    1.8. -> 31.12. -> 1
            */
    {
      HakemusDTO hakemus = new HakemusDTO();
      Oppija suoritus =
          new SuoritusrekisteriSpec.OppijaBuilder()
              .suoritus()
              .setPerusopetus()
              .setVahvistettu(true)
              .setValmistuminen("31.5.2015")
              .setValmis()
              .build()
              .build();

      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false, haku, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
      Assertions.assertTrue(
          hakemus.getAvaimet().stream()
                  .filter(
                      a -> "PK_SUORITUSVUOSI".equals(a.getAvain()) && "2015".equals(a.getArvo()))
                  .count()
              == 1L,
          "PK_SUORITUSVUOSI löytyy ja sen arvo on 2015");
      Assertions.assertEquals(
          new AvainArvoDTO("PK_SUORITUSLUKUKAUSI", "2"),
          hakemus.getAvaimet().stream()
              .filter(a -> "PK_SUORITUSLUKUKAUSI".equals(a.getAvain()))
              .findFirst()
              .get(),
          "PK_SUORITUSLUKUKAUSI löytyy ja sen arvo on 2");
      Assertions.assertTrue(
          hakemus.getAvaimet().stream()
                  .filter(
                      a ->
                          "PK_PAATTOTODISTUSVUOSI".equals(a.getAvain())
                              && "2015".equals(a.getArvo()))
                  .count()
              == 1L,
          "PK_PAATTOTODISTUSVUOSI löytyy ja sen arvo on 2015");
    }
    {
      HakemusDTO hakemus = new HakemusDTO();
      Oppija suoritus =
          new SuoritusrekisteriSpec.OppijaBuilder()
              .suoritus()
              .setPerusopetus()
              .setVahvistettu(true)
              .setValmistuminen(HAKUKAUDELLA)
              .setValmis()
              .build()
              .build();

      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false, haku, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
      Assertions.assertTrue(
          hakemus.getAvaimet().stream()
                  .filter(
                      a -> "PK_SUORITUSVUOSI".equals(a.getAvain()) && "2015".equals(a.getArvo()))
                  .count()
              == 1L,
          "PK_SUORITUSVUOSI löytyy ja sen arvo on 2015");
      Assertions.assertEquals(
          new AvainArvoDTO("PK_SUORITUSLUKUKAUSI", "2"),
          hakemus.getAvaimet().stream()
              .filter(a -> "PK_SUORITUSLUKUKAUSI".equals(a.getAvain()))
              .findFirst()
              .get(),
          "PK_SUORITUSLUKUKAUSI löytyy ja sen arvo on 2");
      Assertions.assertTrue(
          hakemus.getAvaimet().stream()
                  .filter(
                      a ->
                          "PK_PAATTOTODISTUSVUOSI".equals(a.getAvain())
                              && "2015".equals(a.getArvo()))
                  .count()
              == 1L,
          "PK_PAATTOTODISTUSVUOSI löytyy ja sen arvo on 2015");
    }
    {
      HakemusDTO hakemus = new HakemusDTO();
      Oppija suoritus =
          new SuoritusrekisteriSpec.OppijaBuilder()
              .suoritus()
              .setPerusopetus()
              .setVahvistettu(true)
              .setValmistuminen("01.08.2015")
              .setValmis()
              .build()
              .build();

      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false, haku_syksy, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
      Assertions.assertTrue(
          hakemus.getAvaimet().stream()
                  .filter(
                      a -> "PK_SUORITUSVUOSI".equals(a.getAvain()) && "2015".equals(a.getArvo()))
                  .count()
              == 1L,
          "PK_SUORITUSVUOSI löytyy ja sen arvo on 2015");
      Assertions.assertTrue(
          hakemus.getAvaimet().stream()
                  .filter(
                      a -> "PK_SUORITUSLUKUKAUSI".equals(a.getAvain()) && "1".equals(a.getArvo()))
                  .count()
              == 1L,
          "PK_SUORITUSLUKUKAUSI löytyy ja sen arvo on 1");
      Assertions.assertTrue(
          hakemus.getAvaimet().stream()
                  .filter(
                      a ->
                          "PK_PAATTOTODISTUSVUOSI".equals(a.getAvain())
                              && "2015".equals(a.getArvo()))
                  .count()
              == 1L,
          "PK_PAATTOTODISTUSVUOSI löytyy ja sen arvo on 2015");
    }
    {
      HakemusDTO hakemus = new HakemusDTO();
      Oppija suoritus =
          new SuoritusrekisteriSpec.OppijaBuilder()
              .suoritus()
              .setPerusopetus()
              .setVahvistettu(true)
              .setValmistuminen("31.12.2008")
              .setValmis()
              .build()
              .build();

      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false, haku, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
      Assertions.assertTrue(
          hakemus.getAvaimet().stream().filter(a -> "PK_SUORITUSVUOSI".equals(a.getAvain())).count()
              == 0L,
          "PK_SUORITUSVUOSI ei löydy");
      Assertions.assertTrue(
          hakemus.getAvaimet().stream()
                  .filter(a -> "PK_SUORITUSLUKUKAUSI".equals(a.getAvain()))
                  .count()
              == 0L,
          "PK_SUORITUSLUKUKAUSI ei löydy");
      Assertions.assertTrue(
          hakemus.getAvaimet().stream()
                  .filter(a -> "PK_PAATTOTODISTUSVUOSI".equals(a.getAvain()))
                  .count()
              == 0L,
          "PK_PAATTOTODISTUSVUOSI ei löydy");
    }
  }

  @Test
  public void perusopetusOikeinKoskaVainValmisSuoritus() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija oikeinKoskaVainValmisSuoritus =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        oikeinKoskaVainValmisSuoritus,
        hakemus,
        true);
    Assertions.assertEquals(
        new AvainArvoDTO("PK_TILA", "true"),
        hakemus.getAvaimet().stream().filter(a -> "PK_TILA".equals(a.getAvain())).findFirst().get(),
        "PK_TILA löytyy ja sen arvo on true");
  }

  @Test
  public void pkTilaOnTrueJosSuoritusOnMerkittyValmiiksiMuutoinFalse() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija oikeinKoskaEiSuorituksia =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setValmistuminen("1.1.2015")
            .setKeskeytynyt()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        oikeinKoskaEiSuorituksia,
        hakemus,
        true);
    Assertions.assertTrue(
        hakemus.getAvaimet().stream()
                .filter(a -> "PK_TILA".equals(a.getAvain()) && "false".equals(a.getArvo()))
                .count()
            == 1L,
        "PK_TILA löytyy ja sen arvo on false");
  }

  @Test
  public void pkSuoritusArvosanojenPoisFiltterointi() {
    DateTime nyt = DateTime.now();
    HakemusDTO hakemus = new HakemusDTO();
    Oppija suoritus =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .arvosana()
            .setAine("AI")
            .setAsteikko("")
            .setArvosana("S")
            .setMyonnetty(nyt)
            .build()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        laskennanalkamisparametri(nyt.plusDays(1)),
        new HashMap<>(),
        suoritus,
        hakemus,
        true);
    Assertions.assertTrue(
        hakemus.getAvaimet().stream()
                .filter(a -> "PK_AI".equals(a.getAvain()) && "S".equals(a.getArvo()))
                .count()
            == 0L,
        "PK_AI ei löydy ja sen arvo ei ole S");
    Assertions.assertTrue(
        hakemus.getAvaimet().stream()
                .filter(a -> "PK_AI_SUORITETTU".equals(a.getAvain()) && "true".equals(a.getArvo()))
                .count()
            == 1L,
        "PK_AI_SUORITETTU löytyy arvolla true");
  }

  @Test
  public void pkSamanAineenToistuessaKaytetaanParasta() {
    DateTime nyt = DateTime.now();
    HakemusDTO hakemus = new HakemusDTO();
    Oppija suoritus =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .arvosana()
            .setAine("AI")
            .setAsteikko_4_10()
            .setArvosana("6")
            .setMyonnetty(nyt)
            .build()
            .arvosana()
            .setAine("AI")
            .setAsteikko_4_10()
            .setArvosana("8")
            .setMyonnetty(nyt)
            .build()
            .arvosana()
            .setAine("AI")
            .setAsteikko_4_10()
            .setArvosana("7")
            .setMyonnetty(nyt)
            .build()
            .build()
            .build();
    {
      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false,
          haku,
          "",
          laskennanalkamisparametri(nyt.plusDays(1)),
          new HashMap<>(),
          suoritus,
          hakemus,
          true);
      Assertions.assertTrue(
          hakemus.getAvaimet().stream()
                  .filter(a -> "PK_AI".equals(a.getAvain()) && "8".equals(a.getArvo()))
                  .count()
              == 1L,
          "PK_AI löytyy ja sen arvo on 8");
    }
  }

  @Test
  public void kaytetanVainKyseisenHakemuksenItseSyotettyjaArvosanoja() {
    DateTime nyt = DateTime.now();
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    Oppija suoritus =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setVahvistettu(false)
            .setMyontaja(HAKEMUS1_OID)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .arvosana()
            .setAine("AI")
            .setAsteikko_4_10()
            .setArvosana("6")
            .setMyonnetty(nyt)
            .build()
            .build()
            .suoritus()
            .setPerusopetus()
            .setVahvistettu(false)
            .setMyontaja(HAKEMUS2_OID)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .arvosana()
            .setAine("AI")
            .setAsteikko_4_10()
            .setArvosana("8")
            .setMyonnetty(nyt)
            .build()
            .build()
            .build();
    {
      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false,
          haku,
          "",
          laskennanalkamisparametri(nyt.plusDays(1)),
          new HashMap<>(),
          suoritus,
          hakemus,
          true);
      Assertions.assertTrue(
          hakemus.getAvaimet().stream()
                  .filter(a -> "PK_AI".equals(a.getAvain()) && "6".equals(a.getArvo()))
                  .count()
              == 1L,
          "PK_AI löytyy ja sen arvo on 6");
    }
  }

  @Test
  public void pkSamanArvosananToistuessaKaytetaanParastaPaitsiJosMuutOnValinnaisia() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija suoritus =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .arvosana()
            .setAine("AI")
            .setAsteikko_4_10()
            .setArvosana("6")
            .build()
            .arvosana()
            .setAine("AI")
            .setValinnainen()
            .setJarjestys(1)
            .setAsteikko_4_10()
            .setArvosana("8")
            .build()
            .arvosana()
            .setAine("AI")
            .setValinnainen()
            .setJarjestys(2)
            .setAsteikko_4_10()
            .setArvosana("7")
            .build()
            .arvosana()
            .setAine("AI")
            .setValinnainen()
            .setJarjestys(3)
            .setAsteikko_4_10()
            .setArvosana("9")
            .build()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
    Assertions.assertTrue(
        hakemus.getAvaimet().stream()
                .filter(a -> "PK_AI".equals(a.getAvain()) && "6".equals(a.getArvo()))
                .count()
            == 1L,
        "PK_AI löytyy ja sen arvo on 6");
    Assertions.assertTrue(
        hakemus.getAvaimet().stream().filter(a -> "PK_AI_VAL1".equals(a.getAvain())).count() == 1L,
        "PK_AI_VAL1 löytyy");
    Assertions.assertTrue(
        hakemus.getAvaimet().stream().filter(a -> "PK_AI_VAL2".equals(a.getAvain())).count() == 1L,
        "PK_AI_VAL2 löytyy");
    Assertions.assertTrue(
        hakemus.getAvaimet().stream().filter(a -> "PK_AI_VAL3".equals(a.getAvain())).count() == 1L,
        "PK_AI_VAL3 löytyy");
  }

  /** Eri asteikot yhdistettavilla arvosanoilla. Yhdistaminen ei mahdollista. */
  @Test
  public void pkSamaArvoMuttaAsteikotEiTasmaa() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija samaArvoMuttaAsteikotEiTasmaa =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setValmistuminen(HAKUKAUDELLA)
            .setVahvistettu(true)
            .setPerusopetus()
            .setValmis()
            .arvosana()
            .setAine("AI")
            .setAsteikko_4_10()
            .setArvosana("6")
            .build()
            .arvosana()
            .setAine("AI")
            .setAsteikko_1_5()
            .setArvosana("8")
            .build()
            .build()
            .build();
    Exception expected =
        Assertions.assertThrows(
            Exception.class,
            () ->
                hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
                    false,
                    haku,
                    "",
                    new ParametritDTO(),
                    new HashMap<>(),
                    samaArvoMuttaAsteikotEiTasmaa,
                    hakemus,
                    true));
    Assertions.assertEquals("Asteikot ei täsmää: 1-5 4-10", expected.getMessage());
  }

  @Test
  public void pkTuntematonAsteikkoMuttaTunnistetaanNumeroksi() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija tuntematonAsteikkoMuttaTunnistetaanNumeroksi =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .setValmis()
            .arvosana()
            .setAine("AI")
            .setAsteikko("")
            .setArvosana("6")
            .build()
            .arvosana()
            .setAine("AI")
            .setAsteikko("")
            .setArvosana("8")
            .build()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        tuntematonAsteikkoMuttaTunnistetaanNumeroksi,
        hakemus,
        true);
    Assertions.assertTrue(
        hakemus.getAvaimet().stream()
                .filter(a -> "PK_AI".equals(a.getAvain()) && "8".equals(a.getArvo()))
                .count()
            == 1L,
        "PK_AI löytyy ja sen arvo on 8");
  }

  /** Poikkeus koska yhdistaminen ei mahdollista */
  @Test
  public void pkTunnistamatonArvosana() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija tunnistamatonArvosana =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen(HAKUKAUDELLA)
            .setValmis()
            .arvosana()
            .setAine("AI")
            .setAsteikko("")
            .setArvosana("KUUS")
            .build()
            .arvosana()
            .setAine("AI")
            .setAsteikko("")
            .setArvosana("KAHDEKSAN")
            .build()
            .build()
            .build();
    Assertions.assertThrows(
        NumberFormatException.class,
        () ->
            hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
                false,
                haku,
                "",
                new ParametritDTO(),
                new HashMap<>(),
                tunnistamatonArvosana,
                hakemus,
                true));
  }

  @Test
  public void pkVahvistamatonSuoritus() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2015"));
          }
        });
    Oppija tunnistamatonArvosana =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setYksilollistaminen("Ei")
            .setVahvistettu(false)
            .setValmistuminen(HAKUKAUDELLA)
            .setValmis()
            .build()
            .build();
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        tunnistamatonArvosana,
        hakemus,
        true);
    assertEquals(PohjakoulutusToinenAste.PERUSKOULU, getFirstHakemusArvo(hakemus, "POHJAKOULUTUS"));
    assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
    assertEquals(17, hakemus.getAvaimet().size(), "not 17:" + hakemus.getAvaimet());
  }

  @Test
  public void pkParempiArvosanaVaikkaVahvistamatonSuoritus() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija tunnistamatonArvosana =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setValmistuminen("1.1.2015")
            .setValmis()
            .setVahvistettu(false)
            .arvosana()
            .setAine("AI")
            .setAsteikko("")
            .setArvosana("6")
            .build()
            .build()
            .suoritus()
            .setPerusopetus()
            .setValmistuminen("1.1.2015")
            .setValmis()
            .setVahvistettu(true)
            .arvosana()
            .setAine("AI")
            .setAsteikko("")
            .setArvosana("5")
            .build()
            .build()
            .build();
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        tunnistamatonArvosana,
        hakemus,
        true);
    assertEquals(8, hakemus.getAvaimet().size());
    assertEquals("6", getFirstHakemusArvo(hakemus, "PK_AI"));
  }

  // LUKIO:
  // LK_TILA = false // defaut jos suorituksen tila on
  // SUORITUS VAAN KERRAN MYÖS LUKIO TOUHUISSA KOSKA TULEE YTL:LTÄ
  // LUKION ARVOSANA MONEEN KERTAAN JA PARASTA KÄYTETÄÄN

  @Test
  public void lkTilaOnTrueJosSuoritusOnMerkittyValmiiksiMuutoinFalse() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija oikeinKoskaEiSuorituksia =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setLukio()
            .setValmistuminen(HAKUKAUDELLA)
            .setKeskeytynyt()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        oikeinKoskaEiSuorituksia,
        hakemus,
        true);
    Assertions.assertEquals(
        new AvainArvoDTO("LK_TILA", "false"),
        hakemus.getAvaimet().stream().filter(a -> "LK_TILA".equals(a.getAvain())).findFirst().get(),
        "LK_TILA on false");
  }

  @Test
  public void lkSuoritusVainKerran() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    Oppija virheellinenKoskaUseampiSuoritus =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setLukio()
            .setValmistuminen("1.1.2015")
            .setKesken()
            .setMyontaja(HAKEMUS1_OID)
            .build()
            .suoritus()
            .setLukio()
            .setValmistuminen("1.1.2015")
            .setValmis()
            .setVahvistettu(true)
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        virheellinenKoskaUseampiSuoritus,
        hakemus,
        true);
    Assertions.assertTrue(
        hakemus.getAvaimet().stream()
                .filter(a -> "LK_TILA".equals(a.getAvain()) && "true".equals(a.getArvo()))
                .count()
            == 1L,
        "LK_TILA löytyy ja sen arvo on true");
  }

  @Test
  public void lkSuoritusVainTaltaHakemuseltaJosEiVahvistettu1() {
    DateTime nyt = DateTime.now();
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    Oppija toisellaHakemuksellaKorkeampiArvosana =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setLukio()
            .setValmistuminen("1.1.2015")
            .setKesken()
            .setMyontaja(HAKEMUS2_OID)
            .setVahvistettu(false)
            .arvosana()
            .setAine("AI")
            .setAsteikko_4_10()
            .setArvosana("8")
            .setMyonnetty(nyt)
            .build()
            .build()
            .suoritus()
            .setLukio()
            .setValmistuminen("1.1.2015")
            .setValmis()
            .setMyontaja(HAKEMUS1_OID)
            .setVahvistettu(false)
            .arvosana()
            .setAine("AI")
            .setAsteikko_4_10()
            .setArvosana("6")
            .setMyonnetty(nyt)
            .build()
            .build()
            .build();
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        toisellaHakemuksellaKorkeampiArvosana,
        hakemus,
        true);
    Assertions.assertTrue(
        hakemus.getAvaimet().stream()
                .filter(a -> "LK_AI".equals(a.getAvain()) && "6".equals(a.getArvo()))
                .count()
            == 1L,
        "LK_AI löytyy ja sen arvo on 6");
  }

  @Test
  public void lkSuoritusVainTaltaHakemuseltaJosEiVahvistettu2() {
    HakemusDTO hakemus = new HakemusDTO();
    hakemus.setHakemusoid(HAKEMUS1_OID);
    Oppija toisellaHakemuksellaKorkeampiArvosana =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setLukio()
            .setValmistuminen("1.1.2015")
            .setValmis()
            .setMyontaja(HAKEMUS2_OID)
            .build()
            .suoritus()
            .setPerusopetus()
            .setValmistuminen("1.1.2012")
            .setValmis()
            .setMyontaja(HAKEMUS1_OID)
            .setVahvistettu(false)
            .build()
            .build();
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        toisellaHakemuksellaKorkeampiArvosana,
        hakemus,
        true);
    Assertions.assertTrue(
        hakemus.getAvaimet().stream()
                .filter(a -> "LK_TILA".equals(a.getAvain()) && "false".equals(a.getArvo()))
                .count()
            == 1L,
        "LK_TILA löytyy ja sen arvo on false");
  }

  @Test
  public void lkSamanArvosananToistuessaKaytetaanParasta() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija tuntematonAsteikkoMuttaTunnistetaanNumeroksi =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setLukio()
            .setValmistuminen("1.1.2015")
            .setValmis()
            .arvosana()
            .setAine("AI")
            .setAsteikko("")
            .setArvosana("6")
            .build()
            .arvosana()
            .setAine("AI")
            .setAsteikko("")
            .setArvosana("8")
            .build()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        tuntematonAsteikkoMuttaTunnistetaanNumeroksi,
        hakemus,
        true);
    Assertions.assertTrue(
        hakemus.getAvaimet().stream()
                .filter(a -> "LK_AI".equals(a.getAvain()) && "8".equals(a.getArvo()))
                .count()
            == 1L,
        "LK_AI löytyy ja sen arvo on 8");
  }

  @Test
  public void lkSamanArvosananToistuessaToisessaSuorituksessaKaytetaanParasta() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija tuntematonAsteikkoMuttaTunnistetaanNumeroksi =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setLukio()
            .setValmistuminen("1.1.2015")
            .setValmis()
            .arvosana()
            .setAine("AI")
            .setAsteikko("")
            .setArvosana("9")
            .build()
            .arvosana()
            .setAine("AI")
            .setAsteikko("")
            .setArvosana("8")
            .build()
            .build()
            .suoritus()
            .setLukio()
            .setValmistuminen("1.1.2015")
            .setValmis()
            .arvosana()
            .setAine("AI")
            .setAsteikko("")
            .setArvosana("10")
            .build()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        tuntematonAsteikkoMuttaTunnistetaanNumeroksi,
        hakemus,
        true);

    long count = hakemus.getAvaimet().stream().filter(a -> "LK_AI".equals(a.getAvain())).count();
    assertEquals(
        1,
        count,
        "käsitellyssä datassa pitäisi olla vain yksi arvosana (paras arvosana) per aine ");

    AvainArvoDTO avainArvoDTO =
        hakemus.getAvaimet().stream().filter(a -> "LK_AI".equals(a.getAvain())).findFirst().get();
    String arvo = "10";
    AvainArvoDTO expected = new AvainArvoDTO("LK_AI", arvo);
    assertEquals(
        expected,
        avainArvoDTO,
        String.format(
            "LK_AI arvo pitäisi olla toisesta suorituksesta löytyvä korotettu numero {}", arvo));
  }

  // KYMPPILUOKKA:
  // PK_TILA_10 = false // default
  // VAAN YKS SUORITUS (LISÄOPETUS)
  // PK_***_10 SUFFIX
  @Test
  public void kymppiluokanKorotuksissaMyosKeskeytettySuoritusHuomioidaan() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija oikeinKoskaKeskenerainenSuoritus =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setKymppiluokka()
            .setVahvistettu(true)
            .setValmistuminen(HAKUKAUDELLA)
            .setKeskeytynyt()
            .arvosana()
            .setAine("AI")
            .setArvosana("7")
            .build()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        oikeinKoskaKeskenerainenSuoritus,
        hakemus,
        true);
    Assertions.assertTrue(
        hakemus.getAvaimet().stream()
                .filter(a -> "PK_AI".equals(a.getAvain()) && "7".equals(a.getArvo()))
                .count()
            == 1L,
        "PK_AI löytyy ja sen arvo on 7");
  }

  @Test
  public void perusopetuksenOppiaineenOppimaara() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija oikeinKoskaVainValmisSuoritus =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetuksenOppiaineenOppimaara()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .arvosana()
            .setAine("AI")
            .setArvosana("8")
            .build()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        oikeinKoskaVainValmisSuoritus,
        hakemus,
        true);
    Assertions.assertTrue(
        hakemus.getAvaimet().stream()
                .filter(a -> "PK_AI".equals(a.getAvain()) && "8".equals(a.getArvo()))
                .count()
            == 1L,
        "PK_AI löytyy ja sen arvo on 8");
  }

  @Test
  public void perusopetuksenArvosanaaKorotettuPerusopetuksenOppiaineenOppimaarassa() {
    HakemusDTO hakemus = new HakemusDTO();
    Oppija oikeinKoskaVainValmisSuoritus =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setPerusopetus()
            .setVahvistettu(true)
            .setValmistuminen("1.1.2015")
            .arvosana()
            .setAine("AI")
            .setArvosana("8")
            .build()
            .build()
            .suoritus()
            .setPerusopetuksenOppiaineenOppimaara()
            .setVahvistettu(true)
            .setValmistuminen("12.12.2015")
            .arvosana()
            .setAine("AI")
            .setArvosana("9")
            .build()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        oikeinKoskaVainValmisSuoritus,
        hakemus,
        true);
    Assertions.assertTrue(
        hakemus.getAvaimet().stream()
                .filter(a -> "PK_AI".equals(a.getAvain()) && "9".equals(a.getArvo()))
                .count()
            == 1L,
        "PK_AI löytyy ja sen arvo on 9");
  }

  public static boolean testEquality(List<Map<String, String>> a, List<Map<String, String>> b) {
    return a.stream()
        .anyMatch(a0 -> b.stream().anyMatch(b0 -> b0.entrySet().containsAll(a0.entrySet())));
    // a.entrySet().containsAll(b.entrySet());
    /*
    Comparator<TreeMap<String,String>> comp = (m0,m1) -> {
        return 0;
    };
    Collections.sort(a, comp);
    Collections.sort(b, comp);
    */
  }

  public static boolean testEquality(AvainMetatiedotDTO a, List<AvainMetatiedotDTO> aa) {
    return aa.stream()
        .filter(x -> x.getAvain().equals(a.getAvain()))
        .anyMatch(x -> testEquality(a.getMetatiedot(), x.getMetatiedot()));
  }

  @Test
  public void yoTilaOnTrueJosSuoritusOnMerkittyValmiiksiMuutoinFalse() {
    {
      HakemusDTO hakemus = new HakemusDTO();
      Oppija suoritus =
          new SuoritusrekisteriSpec.OppijaBuilder()
              .suoritus()
              .setYo()
              .setVahvistettu(true)
              .setValmistuminen("1.1.2015")
              .setValmis()
              .arvosana()
              .setAine("AINEREAALI")
              .setLisatieto("PS")
              .setAsteikko_yo()
              .setArvosana("M")
              .setPisteet(20)
              .build()
              .build()
              .build();

      // YO-tila tulee edelleen merkitä vaikka yo-arvosanat laitetaan uuteen tietueeseen
      // AvainSuoritusTietoDTO
      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false, haku, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
      Assertions.assertEquals(
          new AvainArvoDTO("YO_TILA", "true"),
          hakemus.getAvaimet().stream()
              .filter(a -> "YO_TILA".equals(a.getAvain()))
              .findFirst()
              .get(),
          "YO_TILA löytyy ja sen arvo on true");
    }
    {
      HakemusDTO hakemus = new HakemusDTO();
      Oppija suoritus =
          new SuoritusrekisteriSpec.OppijaBuilder()
              .suoritus()
              .setYo()
              .setKesken()
              .arvosana()
              .setAine("AINEREAALI")
              .setLisatieto("PS")
              .setAsteikko_yo()
              .setArvosana("M")
              .setPisteet(20)
              .build()
              .build()
              .suoritus()
              .setYo()
              .setKeskeytynyt()
              .arvosana()
              .setAine("AINEREAALI")
              .setLisatieto("PS")
              .setAsteikko_yo()
              .setArvosana("M")
              .setPisteet(20)
              .build()
              .build()
              .build();

      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false, haku, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
      Assertions.assertTrue(
          hakemus.getAvaimet().stream()
                  .filter(a -> "YO_TILA".equals(a.getAvain()) && "false".equals(a.getArvo()))
                  .count()
              == 1L,
          "YO_TILA löytyy ja sen arvo on false");
    }
  }

  @Test
  public void testaaEnsikertalaisuusOppijaTiedoista() {
    {
      Haku haku =
          new Haku(
              "hakuoid",
              new HashMap<>(),
              new HashSet<>(),
              null,
              "haunkohdejoukko_12#1",
              null,
              null,
              null,
              null);
      HakemusDTO hakemus = new HakemusDTO();
      Oppija oppija = new Oppija();
      oppija.setEnsikertalainen(true);

      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
      Assertions.assertTrue(
          Boolean.valueOf(
              hakemus.getAvaimet().stream()
                  .filter(a -> a.getAvain().equals("ensikertalainen"))
                  .findFirst()
                  .get()
                  .getArvo()),
          "Ensikertalaisuus asetettu");
    }

    {
      Haku haku =
          new Haku(
              "hakuoid",
              new HashMap<>(),
              new HashSet<>(),
              null,
              "haunkohdejoukko_12#1",
              null,
              null,
              null,
              null);
      HakemusDTO hakemus = new HakemusDTO();
      Oppija oppija = new Oppija();
      oppija.setEnsikertalainen(false);

      hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
          false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
      Assertions.assertFalse(
          Boolean.valueOf(
              hakemus.getAvaimet().stream()
                  .filter(a -> a.getAvain().equals("ensikertalainen"))
                  .findFirst()
                  .get()
                  .getArvo()),
          "Ensikertalaisuus asetettu");
    }
  }

  @Test
  public void valinnaistenArvosanojenValinta() {
    HakemusDTO hakemus = new HakemusDTO();
    SuoritusrekisteriSpec.OppijaBuilder oppijaBuilder = new SuoritusrekisteriSpec.OppijaBuilder();
    // vahvistettu suoritus
    oppijaBuilder
        .suoritus()
        .setKymppiluokka()
        .setVahvistettu(true)
        .setValmistuminen(HAKUKAUDELLA)
        .setKeskeytynyt()
        .arvosana()
        .setAine("AI")
        .setArvosana("7")
        .build()
        .arvosana()
        .setAine("AI")
        .setValinnainen()
        .setJarjestys(1)
        .setArvosana("7")
        .build()
        .arvosana()
        .setAine("AI")
        .setValinnainen()
        .setJarjestys(2)
        .setArvosana("6")
        .build()
        .arvosana()
        .setAine("AI")
        .setValinnainen()
        .setJarjestys(3)
        .setArvosana("5")
        .build()
        .arvosana()
        .setAine("FY")
        .setArvosana("8")
        .build()
        .arvosana()
        .setAine("FY")
        .setValinnainen()
        .setJarjestys(1)
        .setArvosana("9")
        .build()
        .arvosana()
        .setAine("FY")
        .setValinnainen()
        .setJarjestys(2)
        .setArvosana("S")
        .build()
        .build();

    // vahvistamaton suoritus
    oppijaBuilder
        .suoritus()
        .setKymppiluokka()
        .setVahvistettu(false)
        .setValmistuminen(HAKUKAUDELLA)
        .setKeskeytynyt()
        .arvosana()
        .setAine("AI")
        .setArvosana("5")
        .build()
        .arvosana()
        .setAine("AI")
        .setValinnainen()
        .setJarjestys(1)
        .setArvosana("6")
        .build()
        .arvosana()
        .setAine("AI")
        .setValinnainen()
        .setJarjestys(2)
        .setArvosana("9")
        .build()
        .arvosana()
        .setAine("AI")
        .setValinnainen()
        .setJarjestys(3)
        .setArvosana("10")
        .build()
        .arvosana()
        .setAine("EN")
        .setValinnainen()
        .setJarjestys(1)
        .setArvosana("6")
        .build()
        .arvosana()
        .setAine("FY")
        .setArvosana("10")
        .build()
        .arvosana()
        .setAine("FY")
        .setValinnainen()
        .setJarjestys(1)
        .setArvosana("S")
        .build()
        .arvosana()
        .setAine("FY")
        .setValinnainen()
        .setJarjestys(2)
        .setArvosana("6")
        .build()
        .build();

    // vahvistamaton suoritus
    oppijaBuilder
        .suoritus()
        .setKymppiluokka()
        .setVahvistettu(false)
        .setValmistuminen(HAKUKAUDELLA)
        .setKeskeytynyt()
        .arvosana()
        .setAine("FY")
        .setArvosana("5")
        .build()
        .arvosana()
        .setAine("FY")
        .setValinnainen()
        .setJarjestys(1)
        .setArvosana("8")
        .build()
        .arvosana()
        .setAine("FY")
        .setValinnainen()
        .setJarjestys(2)
        .setArvosana("10")
        .build()
        .arvosana()
        .setAine("FY")
        .setValinnainen()
        .setJarjestys(3)
        .setArvosana("10")
        .build()
        .build();

    Map<String, String> kiinnostavatAvainParit =
        ImmutableMap.of(
            "PK_AI", "7",
            "PK_AI_VAL1", "6",
            "PK_AI_VAL2", "9",
            "PK_AI_VAL3", "10",
            // Yksittäinen valinnainen arvosana suorituksella
            "PK_EN_VAL1", "6");

    Map<String, String> kiinnostavatAvainParitFysiikka =
        ImmutableMap.of(
            "PK_FY", "10",
            "PK_FY_VAL1", "8",
            "PK_FY_VAL2", "10",
            "PK_FY_VAL3", "10");

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        oppijaBuilder.build(),
        hakemus,
        true);
    Assertions.assertEquals(
        kiinnostavatAvainParit,
        hakemus.getAvaimet().stream()
            .filter(a -> kiinnostavatAvainParit.containsKey(a.getAvain()))
            .collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo)),
        "Odotettiin paras arvosana ensimmäisestä suorituksesta ja parhaat valinnaiset jälkimmäisestä suorituksesta");
    Assertions.assertEquals(
        kiinnostavatAvainParitFysiikka,
        hakemus.getAvaimet().stream()
            .filter(a -> kiinnostavatAvainParitFysiikka.containsKey(a.getAvain()))
            .collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo)));
  }

  @Test
  public void valinnaistenArvosanojenValintaKunMukanaOnSuoritusmerkintoja() {
    HakemusDTO hakemus = new HakemusDTO();
    SuoritusrekisteriSpec.OppijaBuilder oppijaBuilder = new SuoritusrekisteriSpec.OppijaBuilder();
    // vahvistettu suoritus
    oppijaBuilder
        .suoritus()
        .setKymppiluokka()
        .setVahvistettu(true)
        .setValmistuminen(HAKUKAUDELLA)
        .setKeskeytynyt()
        .arvosana()
        .setAine("AI")
        .setArvosana("7")
        .build()
        .arvosana()
        .setAine("AI")
        .setValinnainen()
        .setJarjestys(1)
        .setArvosana("8")
        .build()
        .arvosana()
        .setAine("AI")
        .setValinnainen()
        .setJarjestys(2)
        .setArvosana("S")
        .build()
        .arvosana()
        .setAine("AI")
        .setValinnainen()
        .setJarjestys(3)
        .setArvosana("S")
        .build()
        .build();
    // vahvistamaton suoritus
    oppijaBuilder
        .suoritus()
        .setKymppiluokka()
        .setVahvistettu(true)
        .setValmistuminen(HAKUKAUDELLA)
        .setKeskeytynyt()
        .arvosana()
        .setAine("AI")
        .setArvosana("5")
        .build()
        .arvosana()
        .setAine("AI")
        .setValinnainen()
        .setJarjestys(1)
        .setArvosana("5")
        .build()
        .arvosana()
        .setAine("AI")
        .setValinnainen()
        .setJarjestys(2)
        .setArvosana("9")
        .build()
        .arvosana()
        .setAine("AI")
        .setValinnainen()
        .setJarjestys(3)
        .setArvosana("5")
        .build()
        .build();

    Map<String, String> kiinnostavatAvainParit =
        ImmutableMap.of(
            "PK_AI", "7",
            "PK_AI_VAL1", "8",
            "PK_AI_VAL2_SUORITETTU", "true",
            "PK_AI_VAL3_SUORITETTU", "true");
    Set<String> eiSaaLoytya = ImmutableSet.of("PK_AI_VAL2", "PK_AI_VAL3");

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        oppijaBuilder.build(),
        hakemus,
        true);
    Assertions.assertEquals(
        hakemus.getAvaimet().stream()
            .filter(a -> kiinnostavatAvainParit.containsKey(a.getAvain()))
            .collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo)),
        kiinnostavatAvainParit,
        "Odotettiin paras arvosana ensimmäisestä suorituksesta ja parhaat valinnaiset ensimmäisestä suorituksesta");
    Assertions.assertEquals(
        hakemus.getAvaimet().stream()
            .filter(a -> eiSaaLoytya.contains(a.getAvain()))
            .collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo)),
        Collections.emptyMap(),
        "Merkintöjä VAL2:lle ja VAL3:lle ei saa löytyä suoritusmerkintöjä huolimatta");
  }

  private Set<Map.Entry<String, String>> avaimetToEntrySet(Stream<AvainArvoDTO> avaimet) {
    return avaimet.collect(Collectors.toMap(a -> a.getAvain(), a -> a.getArvo())).entrySet();
  }

  @Test
  public void testaaToAvainMap() {
    {
      Map<String, Exception> poikkeukset = Maps.newHashMap();
      hakemuksetConverterUtil.toAvainMap(Collections.emptyList(), "", "", poikkeukset);
      assertTrue(poikkeukset.isEmpty());
    }
    {
      Map<String, Exception> poikkeukset = Maps.newHashMap();
      hakemuksetConverterUtil.toAvainMap(Arrays.asList(avain("a")), "", "", poikkeukset);
      assertTrue(poikkeukset.isEmpty());
    }
    {
      Map<String, Exception> poikkeukset = Maps.newHashMap();
      hakemuksetConverterUtil.toAvainMap(
          Arrays.asList(avain("b"), avain("a")), "", "", poikkeukset);
      assertTrue(poikkeukset.isEmpty());
    }
    {
      Map<String, Exception> poikkeukset = Maps.newHashMap();
      hakemuksetConverterUtil.toAvainMap(
          Arrays.asList(avain("a"), avain("a")), "", "", poikkeukset);
      assertTrue(!poikkeukset.isEmpty());
    }
  }

  @Test
  public void ammatillisenKielikoeLuetaanSuoritusrekisteristaJaHyvaksyttyDominoi() {
    HakemusDTO h = new HakemusDTO();
    h.setHakemusoid(HAKEMUS1_OID);
    h.setAvaimet(new ArrayList<>());
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setAmmatillisenKielikoe()
            .setVahvistettu(true)
            .setValmis()
            .setValmistuminen("1.10.2015")
            .setMyontaja(HAKEMUS1_OID)
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("FI")
            .setAsteikko_hyvaksytty()
            .setArvosana(hylatty)
            .setMyonnetty(new LocalDate(2015, 10, 1).toDateTimeAtStartOfDay())
            .build()
            .build()
            .suoritus()
            .setAmmatillisenKielikoe()
            .setVahvistettu(true)
            .setValmis()
            .setValmistuminen("18.5.2016")
            .setMyontaja(HAKEMUS2_OID)
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("FI")
            .setAsteikko_hyvaksytty()
            .setArvosana(hyvaksytty)
            .setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay())
            .build()
            .build()
            .suoritus()
            .setAmmatillisenKielikoe()
            .setVahvistettu(true)
            .setValmis()
            .setValmistuminen("23.9.2016")
            .setMyontaja("eri_hakemus_oid")
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("FI")
            .setAsteikko_hyvaksytty()
            .setArvosana(hylatty)
            .setMyonnetty(new LocalDate(2016, 9, 23).toDateTimeAtStartOfDay())
            .build()
            .build()
            .suoritus()
            .setAmmatillisenKielikoe()
            .setVahvistettu(true)
            .setValmis()
            .setValmistuminen("23.9.2016")
            .setMyontaja("eri_hakemus_oid")
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("SV")
            .setAsteikko_hyvaksytty()
            .setArvosana(hylatty)
            .setMyonnetty(new LocalDate(2016, 9, 23).toDateTimeAtStartOfDay())
            .build()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, h, true);
    Assertions.assertEquals("true", getFirstHakemusArvo(h, "kielikoe_fi"));
    Assertions.assertEquals("false", getFirstHakemusArvo(h, "kielikoe_sv"));
  }

  @Test
  public void ammatillisenKielikoetietoHakemukseltaYlikirjoitetaanSuoritusrekisterista() {
    HakemusDTO h = new HakemusDTO();
    h.setHakemusoid(HAKEMUS1_OID);
    h.setAvaimet(
        new ArrayList<>() {
          {
            add(new AvainArvoDTO("kielikoe_fi", "true"));
          }
        });
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setAmmatillisenKielikoe()
            .setVahvistettu(true)
            .setValmis()
            .setValmistuminen("18.5.2016")
            .setMyontaja(HAKEMUS1_OID)
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("FI")
            .setAsteikko_hyvaksytty()
            .setArvosana(hylatty)
            .setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay())
            .build()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, h, true);
    Assertions.assertEquals("false", getFirstHakemusArvo(h, "kielikoe_fi"));
  }

  @Test
  public void ammatillisenKielikokeenOsallistumistietoTuleeMyosSuoritusrekisterista() {
    HakemusDTO h = new HakemusDTO();
    h.setHakemusoid(HAKEMUS1_OID);
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setAmmatillisenKielikoe()
            .setVahvistettu(true)
            .setValmis()
            .setValmistuminen("18.5.2016")
            .setMyontaja(HAKEMUS1_OID)
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("FI")
            .setAsteikko_hyvaksytty()
            .setArvosana(hyvaksytty)
            .setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay())
            .build()
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("SV")
            .setAsteikko_hyvaksytty()
            .setArvosana(ei_osallistunut)
            .setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay())
            .build()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, h, true);
    Assertions.assertEquals(
        OSALLISTUI.name(), getFirstHakemusArvo(h, "kielikoe_fi-OSALLISTUMINEN"));
    Assertions.assertEquals(
        EI_OSALLISTUNUT.name(), getFirstHakemusArvo(h, "kielikoe_sv-OSALLISTUMINEN"));
    Assertions.assertEquals("true", getFirstHakemusArvo(h, "kielikoe_fi"));
    Assertions.assertEquals("", getFirstHakemusArvo(h, "kielikoe_sv"));
  }

  @Test
  public void
      ammatillisenKielikokeenOsallistumistiedoksiTuleeMerkitsemattaJosSuoritusOnEriHakemukselta() {
    HakemusDTO h = new HakemusDTO();
    h.setHakemusoid(HAKEMUS1_OID);
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setAmmatillisenKielikoe()
            .setVahvistettu(true)
            .setValmis()
            .setValmistuminen("18.5.2016")
            .setMyontaja(HAKEMUS2_OID)
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("FI")
            .setAsteikko_hyvaksytty()
            .setArvosana(hyvaksytty)
            .setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay())
            .build()
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("SV")
            .setAsteikko_hyvaksytty()
            .setArvosana(ei_osallistunut)
            .setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay())
            .build()
            .build()
            .build();

    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, h, true);
    Assertions.assertEquals(
        MERKITSEMATTA.name(), getFirstHakemusArvo(h, "kielikoe_fi-OSALLISTUMINEN"));
    Assertions.assertEquals(
        MERKITSEMATTA.name(), getFirstHakemusArvo(h, "kielikoe_sv-OSALLISTUMINEN"));
    Assertions.assertEquals("true", getFirstHakemusArvo(h, "kielikoe_fi"));
    Assertions.assertEquals("", getFirstHakemusArvo(h, "kielikoe_sv"));
  }

  @Test
  public void
      useampiAmmatillisenKielikoeArvosanatietoSamalleSuoritukselleSurestaPalauttaaHyvaksytynArvosananJosSellainenLoytyy() {
    HakemusDTO h = new HakemusDTO();
    h.setHakemusoid(HAKEMUS1_OID);
    h.setAvaimet(new ArrayList<>());
    Oppija oppijaJollaOnSekaHyvaksyttyEttaHylattyArvosana =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setAmmatillisenKielikoe()
            .setVahvistettu(true)
            .setValmis()
            .setValmistuminen("18.5.2016")
            .setMyontaja(HAKEMUS1_OID)
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("FI")
            .setAsteikko_hyvaksytty()
            .setArvosana(hylatty)
            .setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay())
            .build()
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("FI")
            .setAsteikko_hyvaksytty()
            .setArvosana(hyvaksytty)
            .setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay())
            .build()
            .build()
            .build();
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        oppijaJollaOnSekaHyvaksyttyEttaHylattyArvosana,
        h,
        true);
    Assertions.assertEquals("true", getFirstHakemusArvo(h, "kielikoe_fi"));

    h.setAvaimet(new ArrayList<>());
    Oppija oppijaJollaOnVainHylattyjaArvosanoja =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setAmmatillisenKielikoe()
            .setVahvistettu(true)
            .setValmis()
            .setValmistuminen("18.5.2016")
            .setMyontaja(HAKEMUS2_OID)
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("FI")
            .setAsteikko_hyvaksytty()
            .setArvosana(hylatty)
            .setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay())
            .build()
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("FI")
            .setAsteikko_hyvaksytty()
            .setArvosana(hylatty)
            .setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay())
            .build()
            .build()
            .build();
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        new ParametritDTO(),
        new HashMap<>(),
        oppijaJollaOnVainHylattyjaArvosanoja,
        h,
        true);
    Assertions.assertEquals("false", getFirstHakemusArvo(h, "kielikoe_fi"));
  }

  @Test
  public void ammatillisenKielikoeArvosanatiedonAsteikkoTuleeOllaHyvaksytty() {
    HakemusDTO h = new HakemusDTO();
    h.setHakemusoid(HAKEMUS1_OID);
    h.setAvaimet(new ArrayList<>());
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setAmmatillisenKielikoe()
            .setVahvistettu(true)
            .setValmis()
            .setValmistuminen("18.5.2016")
            .setMyontaja(HAKEMUS1_OID)
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("FI")
            .setAsteikko_Osakoe()
            .setArvosana(hylatty)
            .setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay())
            .build()
            .build()
            .build();

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
                false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, h, true));
  }

  @Test
  public void ammatillisenKielikoeArvosanaaEiHuomioidaJosSeOnValintalaskennanJalkeen() {
    HakemusDTO h = new HakemusDTO();
    h.setHakemusoid(HAKEMUS1_OID);
    h.setAvaimet(new ArrayList<>());
    Oppija oppija =
        new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus()
            .setAmmatillisenKielikoe()
            .setVahvistettu(true)
            .setValmis()
            .setValmistuminen("18.5.2016")
            .setMyontaja(HAKEMUS2_OID)
            .arvosana()
            .setAine("kielikoe")
            .setLisatieto("FI")
            .setAsteikko_hyvaksytty()
            .setArvosana(hyvaksytty)
            .setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay())
            .build()
            .build()
            .build();

    ParametritDTO parametritJoissLaskentaAlkoiEnnenSuoritusta =
        SuoritusrekisteriSpec.laskennanalkamisparametri(
            new LocalDate(2016, 5, 16).toDateTimeAtStartOfDay());
    hakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(
        false,
        haku,
        "",
        parametritJoissLaskentaAlkoiEnnenSuoritusta,
        new HashMap<>(),
        oppija,
        h,
        true);
    assertTrue(firstHakemusArvo(h, "kielikoe_fi").isEmpty());
  }

  private static String getFirstHakemusArvo(HakemusDTO hakemusDTO, String avain) {
    return firstHakemusArvo(hakemusDTO, avain).get();
  }

  private static Optional<String> firstHakemusArvo(HakemusDTO hakemusDTO, String avain) {
    return hakemusDTO.getAvaimet().stream()
        .filter(k -> k.getAvain().equals(avain))
        .map(AvainArvoDTO::getArvo)
        .findFirst();
  }

  private AvainArvoDTO avain(String avain) {
    AvainArvoDTO a = new AvainArvoDTO();
    a.setAvain(avain);
    return a;
  }
}
