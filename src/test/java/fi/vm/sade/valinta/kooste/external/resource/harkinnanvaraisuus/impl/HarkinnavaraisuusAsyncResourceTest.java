package fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.impl;

import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakutoive;
import fi.vm.sade.valinta.kooste.external.resource.ataru.impl.AtaruAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuudenSyy;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakemuksenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakutoiveenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloViiteDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.impl.OppijanumerorekisteriAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.impl.SuoritusrekisteriAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.util.AtaruHakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;

public class HarkinnavaraisuusAsyncResourceTest {

  AtaruAsyncResource mockAtaru = mock(AtaruAsyncResourceImpl.class);
  SuoritusrekisteriAsyncResource mockSure = mock(SuoritusrekisteriAsyncResourceImpl.class);
  OppijanumerorekisteriAsyncResource mockOnr = mock(OppijanumerorekisteriAsyncResourceImpl.class);
  public static final String PK_KOMO = "1.2.246.562.13.62959769647";

  @Before
  public void init() {
    startShared();
  }

  @Test
  public void testHakemustenHarkinnanvaraisuudet()
      throws ExecutionException, InterruptedException, TimeoutException {

    String leikkuriPvm = "2022-06-06";

    List<String> hakemusOids = new ArrayList<>();
    String hakemusOid1 = "1.2.246.562.11.00001010666";
    String hakemusOid2 = "1.2.246.562.11.00001010667";
    String hakukohdeOid1 = "1.2.246.562.20.42208535555";
    String hakukohdeOid2 = "1.2.246.562.20.42208535556";

    AtaruHakutoive hakutoive1 = new AtaruHakutoive();
    hakutoive1.setHarkinnanvaraisuus(HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN);
    hakutoive1.setHakukohdeOid(hakukohdeOid1);
    AtaruHakutoive hakutoive2 = new AtaruHakutoive();
    hakutoive2.setHakukohdeOid(hakukohdeOid2);
    hakutoive2.setHarkinnanvaraisuus(HarkinnanvaraisuudenSyy.ATARU_SOSIAALISET_SYYT);

    AtaruHakemus ataruh1 = new AtaruHakemus();
    ataruh1.setHakemusOid(hakemusOid1);
    ataruh1.setHakutoiveet(List.of(hakutoive1));
    AtaruHakemus ataruh2 = new AtaruHakemus();
    ataruh2.setHakemusOid(hakemusOid2);
    ataruh2.setHakutoiveet(List.of(hakutoive2));

    HakemusWrapper hw1 = new AtaruHakemusWrapper(ataruh1, new HenkiloPerustietoDto());
    HakemusWrapper hw2 = new AtaruHakemusWrapper(ataruh2, new HenkiloPerustietoDto());

    hakemusOids.add(hakemusOid1);
    hakemusOids.add(hakemusOid2);

    List<HakemusWrapper> ataruResult = new ArrayList<>();
    ataruResult.add(hw1);
    ataruResult.add(hw2);

    HakemuksenHarkinnanvaraisuus expected1 =
        new HakemuksenHarkinnanvaraisuus(
            hakemusOid1,
            List.of(
                new HakutoiveenHarkinnanvaraisuus(
                    hakukohdeOid1, HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN)));
    HakemuksenHarkinnanvaraisuus expected2 =
        new HakemuksenHarkinnanvaraisuus(
            hakemusOid2,
            List.of(
                new HakutoiveenHarkinnanvaraisuus(
                    hakukohdeOid2, HarkinnanvaraisuudenSyy.SURE_YKS_MAT_AI)));

    HarkinnanvaraisuusAsyncResource h =
        new HarkinnanvaraisuusAsyncResourceImpl(leikkuriPvm, mockAtaru, mockSure, mockOnr);

    when(mockAtaru.getApplicationsByOidsWithHarkinnanvaraisuustieto(hakemusOids))
        .thenReturn(CompletableFuture.completedFuture(ataruResult));
    when(mockAtaru.getApplicationsByOidsWithHarkinnanvaraisuustieto(hakemusOids))
        .thenReturn(CompletableFuture.completedFuture(ataruResult));

    List<HakemuksenHarkinnanvaraisuus> hhv =
        h.getHarkinnanvaraisuudetForHakemuksesOnlyFromAtaru(hakemusOids).get(10, SECONDS);
    assertTrue(
        hhv.stream()
            .anyMatch(
                hakemuksenHarkinnanvaraisuus ->
                    hakemuksenHarkinnanvaraisuus
                        .getHakutoiveet()
                        .get(0)
                        .getHarkinnanvaraisuudenSyy()
                        .equals(HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN)));
    assertTrue(
        hhv.stream()
            .anyMatch(
                hakemuksenHarkinnanvaraisuus ->
                    hakemuksenHarkinnanvaraisuus
                        .getHakutoiveet()
                        .get(0)
                        .getHarkinnanvaraisuudenSyy()
                        .equals(HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN)));
  }

  @Test
  public void testHakemustenHarkinnanvaraisuudetYliajoSurenTiedoilla()
      throws ExecutionException, InterruptedException, TimeoutException {

    // Luodaan kaksi hakemusta, toisella on suressa yksilöllistetty MA+AI ja toisella tavallinen
    // valmis peruskoulusuoritus.
    String leikkuriPvm = "2022-06-06";
    List<String> hakemusOids = new ArrayList<>();
    String hakemusOid1 = "1.2.246.562.11.00001010666";
    String hakemusOid2 = "1.2.246.562.11.00001010667";
    String hakukohdeOid1 = "1.2.246.562.20.42208535555";
    String hakukohdeOid2 = "1.2.246.562.20.42208535556";
    String henkiloOid1 = "1.2.246.562.24.47613331111";
    String aliasForHenkiloOid1 = "1.2.246.562.24.47613331112";
    String henkiloOid2 = "1.2.246.562.24.47613332222";

    AtaruHakutoive hakutoive1 = new AtaruHakutoive();
    hakutoive1.setHarkinnanvaraisuus(HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN);
    hakutoive1.setHakukohdeOid(hakukohdeOid1);
    AtaruHakutoive hakutoive2 = new AtaruHakutoive();
    hakutoive2.setHakukohdeOid(hakukohdeOid2);
    hakutoive2.setHarkinnanvaraisuus(HarkinnanvaraisuudenSyy.ATARU_SOSIAALISET_SYYT);

    AtaruHakemus ataruh1 = new AtaruHakemus();
    ataruh1.setHakemusOid(hakemusOid1);
    ataruh1.setHakutoiveet(List.of(hakutoive1));
    ataruh1.setPersonOid(henkiloOid1);
    AtaruHakemus ataruh2 = new AtaruHakemus();
    ataruh2.setHakemusOid(hakemusOid2);
    ataruh2.setHakutoiveet(List.of(hakutoive2));
    ataruh2.setPersonOid(henkiloOid2);

    HenkiloPerustietoDto henkilo1 = new HenkiloPerustietoDto();
    henkilo1.setOidHenkilo(henkiloOid1);

    HenkiloPerustietoDto henkilo2 = new HenkiloPerustietoDto();
    henkilo2.setOidHenkilo(henkiloOid2);

    HakemusWrapper hw1 = new AtaruHakemusWrapper(ataruh1, henkilo1);
    HakemusWrapper hw2 = new AtaruHakemusWrapper(ataruh2, henkilo2);

    assertEquals(hw1.getPersonOid(), henkiloOid1);
    assertEquals(hw2.getPersonOid(), henkiloOid2);

    hakemusOids.add(hakemusOid1);
    hakemusOids.add(hakemusOid2);

    List<HakemusWrapper> ataruResult = new ArrayList<>();
    ataruResult.add(hw1);
    ataruResult.add(hw2);

    Suoritus pkSuoritusYksMatAi = new Suoritus();
    pkSuoritusYksMatAi.setHenkiloOid(aliasForHenkiloOid1);
    pkSuoritusYksMatAi.setKomo(PK_KOMO);
    pkSuoritusYksMatAi.setTila("VALMIS");
    pkSuoritusYksMatAi.setVahvistettu(true);
    pkSuoritusYksMatAi.setLahdeArvot(Map.of("foo", "true", "yksilollistetty_ma_ai", "true"));

    Suoritus pkSuoritusValmis = new Suoritus();
    pkSuoritusValmis.setHenkiloOid(henkiloOid2);
    pkSuoritusValmis.setKomo(PK_KOMO);
    pkSuoritusValmis.setTila("VALMIS");
    pkSuoritusValmis.setVahvistettu(true);
    pkSuoritusValmis.setLahdeArvot(Map.of("foo", "true"));

    SuoritusJaArvosanat sa1 = new SuoritusJaArvosanat();
    sa1.setSuoritus(pkSuoritusYksMatAi);
    SuoritusJaArvosanat sa2 = new SuoritusJaArvosanat();
    sa2.setSuoritus(pkSuoritusValmis);

    Oppija o1 = new Oppija();
    o1.setSuoritukset(List.of(sa1));
    o1.setOppijanumero(aliasForHenkiloOid1);

    Oppija o2 = new Oppija();
    o2.setSuoritukset(List.of(sa2));
    o2.setOppijanumero(henkiloOid2);

    List<Oppija> sureResult = List.of(o1, o2);

    HarkinnanvaraisuusAsyncResource h =
        new HarkinnanvaraisuusAsyncResourceImpl(leikkuriPvm, mockAtaru, mockSure, mockOnr);

    List<HenkiloViiteDto> onrResult =
        List.of(new HenkiloViiteDto(aliasForHenkiloOid1, henkiloOid1));

    when(mockAtaru.getApplicationsByOidsWithHarkinnanvaraisuustieto(hakemusOids))
        .thenReturn(CompletableFuture.completedFuture(ataruResult));
    when(mockSure.getSuorituksetForOppijasWithoutEnsikertalaisuus(
            List.of(henkiloOid1, henkiloOid2)))
        .thenReturn(CompletableFuture.completedFuture(sureResult));
    when(mockOnr.haeHenkiloOidDuplikaatit(Set.of(henkiloOid1, henkiloOid2)))
        .thenReturn(CompletableFuture.completedFuture(onrResult));

    CompletableFuture<List<HakemuksenHarkinnanvaraisuus>> hhv =
        h.getHarkinnanvaraisuudetForHakemukses(hakemusOids);

    assertEquals(
        1,
        hhv.get().stream()
            .filter(
                hakemuksenHarkinnanvaraisuus ->
                    hakemuksenHarkinnanvaraisuus
                        .getHakutoiveet()
                        .get(0)
                        .getHarkinnanvaraisuudenSyy()
                        .equals(HarkinnanvaraisuudenSyy.SURE_YKS_MAT_AI))
            .count());
  }

  @Test
  public void testAtarutietojenYliajoSurenTiedoilla()
      throws ExecutionException, InterruptedException, TimeoutException {

    // Luodaan kaksi hakemusta, toisella on suressa yksilöllistetty MA+AI ja toisella tavallinen
    // valmis peruskoulusuoritus.
    String leikkuriPvm = "2022-06-06";

    String hakemusOid1 = "1.2.246.562.11.00001010666";
    String hakemusOid2 = "1.2.246.562.11.00001010667";
    String hakukohdeOid1 = "1.2.246.562.20.42208535555";
    String hakukohdeOid2 = "1.2.246.562.20.42208535556";
    String hakukohdeOid3 = "1.2.246.562.20.42208535557";
    String henkiloOid1 = "1.2.246.562.24.47613331111";
    String aliasForHenkiloOid1 = "1.2.246.562.24.47613331112";
    String henkiloOid2 = "1.2.246.562.24.47613332222";

    Suoritus pkSuoritusYksMatAi = new Suoritus();
    pkSuoritusYksMatAi.setHenkiloOid(aliasForHenkiloOid1);
    pkSuoritusYksMatAi.setKomo(PK_KOMO);
    pkSuoritusYksMatAi.setTila("VALMIS");
    pkSuoritusYksMatAi.setVahvistettu(true);
    pkSuoritusYksMatAi.setLahdeArvot(Map.of("foo", "true", "yksilollistetty_ma_ai", "true"));

    Suoritus pkSuoritusValmis = new Suoritus();
    pkSuoritusValmis.setHenkiloOid(henkiloOid2);
    pkSuoritusValmis.setKomo(PK_KOMO);
    pkSuoritusValmis.setTila("VALMIS");
    pkSuoritusValmis.setVahvistettu(true);
    pkSuoritusValmis.setLahdeArvot(Map.of("foo", "true"));

    SuoritusJaArvosanat sa1 = new SuoritusJaArvosanat();
    sa1.setSuoritus(pkSuoritusYksMatAi);
    SuoritusJaArvosanat sa2 = new SuoritusJaArvosanat();
    sa2.setSuoritus(pkSuoritusValmis);

    Oppija o1 = new Oppija();
    o1.setSuoritukset(List.of(sa1));
    o1.setOppijanumero(aliasForHenkiloOid1);

    Oppija o2 = new Oppija();
    o2.setSuoritukset(List.of(sa2));
    o2.setOppijanumero(henkiloOid2);

    List<Oppija> sureResult = List.of(o1, o2);

    HarkinnanvaraisuusAsyncResource h =
        new HarkinnanvaraisuusAsyncResourceImpl(leikkuriPvm, mockAtaru, mockSure, mockOnr);

    List<HenkiloViiteDto> onrResult =
        List.of(new HenkiloViiteDto(aliasForHenkiloOid1, henkiloOid1));

    when(mockSure.getSuorituksetForOppijasWithoutEnsikertalaisuus(
            List.of(henkiloOid1, henkiloOid2)))
        .thenReturn(CompletableFuture.completedFuture(sureResult));
    when(mockOnr.haeHenkiloOidDuplikaatit(Set.of(henkiloOid1, henkiloOid2)))
        .thenReturn(CompletableFuture.completedFuture(onrResult));

    HakemuksenHarkinnanvaraisuus hhv1 =
        new HakemuksenHarkinnanvaraisuus(
            hakemusOid1,
            List.of(
                new HakutoiveenHarkinnanvaraisuus(
                    hakukohdeOid1, HarkinnanvaraisuudenSyy.ATARU_OPPIMISVAIKEUDET),
                new HakutoiveenHarkinnanvaraisuus(
                    hakukohdeOid2, HarkinnanvaraisuudenSyy.ATARU_OPPIMISVAIKEUDET)));
    hhv1.setHenkiloOid(henkiloOid1);

    HakemuksenHarkinnanvaraisuus hhv2 =
        new HakemuksenHarkinnanvaraisuus(
            hakemusOid2,
            List.of(
                new HakutoiveenHarkinnanvaraisuus(
                    hakukohdeOid1, HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN),
                new HakutoiveenHarkinnanvaraisuus(
                    hakukohdeOid3, HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN)));
    hhv2.setHenkiloOid(henkiloOid2);

    CompletableFuture<List<HakemuksenHarkinnanvaraisuus>> hhv =
        h.getSyncedHarkinnanvaraisuudes(List.of(hhv1, hhv2));

    assertEquals(
        1,
        hhv.get().stream()
            .filter(
                hakemuksenHarkinnanvaraisuus ->
                    hakemuksenHarkinnanvaraisuus
                        .getHakutoiveet()
                        .get(0)
                        .getHarkinnanvaraisuudenSyy()
                        .equals(HarkinnanvaraisuudenSyy.SURE_YKS_MAT_AI))
            .count());
    assertEquals(
        1, hhv.get().stream().filter(hark -> hakemusOid1.equals(hark.getHakemusOid())).count());
    assertEquals(
        1, hhv.get().stream().filter(hark -> hakemusOid2.equals(hark.getHakemusOid())).count());
  }
}