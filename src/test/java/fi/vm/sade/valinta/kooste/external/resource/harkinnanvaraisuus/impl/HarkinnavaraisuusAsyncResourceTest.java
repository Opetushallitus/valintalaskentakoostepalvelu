package fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuudenSyy;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakemuksenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakutoiveenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloViiteDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.impl.OppijanumerorekisteriAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.impl.SuoritusrekisteriAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.testapp.MockServicesApp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HarkinnavaraisuusAsyncResourceTest {

  SuoritusrekisteriAsyncResource mockSure = mock(SuoritusrekisteriAsyncResourceImpl.class);
  OppijanumerorekisteriAsyncResource mockOnr = mock(OppijanumerorekisteriAsyncResourceImpl.class);
  public static final String PK_KOMO = "1.2.246.562.13.62959769647";

  @BeforeEach
  public void init() {
    MockServicesApp.start();
  }

  @Test
  public void testAtarutietojenYliajoSurenTiedoilla()
      throws ExecutionException, InterruptedException, TimeoutException {

    // Luodaan kaksi hakemusta, toisella on suressa yksilöllistetty MA+AI ja toisella tavallinen
    // valmis peruskoulusuoritus.
    String leikkuriPvm = "2032-06-06";

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
        new HarkinnanvaraisuusAsyncResourceImpl(leikkuriPvm, mockSure, mockOnr);

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
