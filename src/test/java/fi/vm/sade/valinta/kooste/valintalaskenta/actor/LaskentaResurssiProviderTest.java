package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetJarjestyskriteeriDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetValinnanVaiheDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoJarjestyskriteereillaDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.mocks.MockAtaruAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.dao.ParametritDao;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.SuoritustiedotDTO;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

public class LaskentaResurssiProviderTest {
  private static final String HAKU_OID = "1.2.246.562.29.00000000001";
  private static final String HAKUKOHDE_OID = "1.2.246.562.20.90242725084";
  private static final String HAKEMUS_OID = "1.2.246.562.11.00000000000000000063";
  private static final String KOSKI_JSON = "[{\"oid\":\"1.2.246.562.15.12345678901\"}]";

  private final AtaruAsyncResource ataruAsyncResource = mock(AtaruAsyncResource.class);
  private final ValintaperusteetAsyncResource valintaperusteetAsyncResource =
      mock(ValintaperusteetAsyncResource.class);
  private final TarjontaAsyncResource tarjontaAsyncResource = mock(TarjontaAsyncResource.class);
  private final ValintapisteAsyncResource valintapisteAsyncResource =
      mock(ValintapisteAsyncResource.class);
  private final KoskiService koskiService = mock(KoskiService.class);
  private final OhjausparametritAsyncResource ohjausparametritAsyncResource =
      mock(OhjausparametritAsyncResource.class);

  private final HakemusWrapper hakemus = MockAtaruAsyncResource.getAtaruHakemusWrapper(HAKEMUS_OID);

  private LaskentaResurssiProvider laskentaResurssiProvider;

  @BeforeEach
  public void setUp() {
    laskentaResurssiProvider =
        new LaskentaResurssiProvider(
            ataruAsyncResource,
            valintaperusteetAsyncResource,
            tarjontaAsyncResource,
            valintapisteAsyncResource,
            koskiService,
            new HakemuksetConverterUtil(
                "9999-12-31", "9999-12-31", mock(HarkinnanvaraisuusAsyncResource.class)),
            mock(OppijanumerorekisteriAsyncResource.class),
            ohjausparametritAsyncResource,
            mock(CloudWatchClient.class),
            mock(ParametritDao.class),
            "test");

    when(ohjausparametritAsyncResource.haeHaunOhjausparametrit(HAKU_OID))
        .thenReturn(CompletableFuture.completedFuture(new ParametritDTO()));
    when(tarjontaAsyncResource.haeHaku(HAKU_OID))
        .thenReturn(
            CompletableFuture.completedFuture(
                new Haku(
                    HAKU_OID,
                    Map.of("fi", "Testihaku"),
                    Collections.emptySet(),
                    "lomake-avain",
                    "haunkohdejoukko_11#1",
                    "kausi_s#1",
                    2026,
                    "kausi_k#1",
                    2027)));
    when(tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(HAKU_OID))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));
    when(valintaperusteetAsyncResource.haeValintaperusteet(eq(HAKUKOHDE_OID), any()))
        .thenReturn(CompletableFuture.completedFuture(List.of(valintaperusteet())));
    when(ataruAsyncResource.getApplicationsByHakukohde(eq(HAKUKOHDE_OID), anyBoolean()))
        .thenReturn(CompletableFuture.completedFuture(List.of(hakemus)));
    when(valintapisteAsyncResource.getValintapisteetWithHakemusOidsAsFuture(
            anyList(), any(AuditSession.class)))
        .thenReturn(
            CompletableFuture.completedFuture(
                new PisteetWithLastModified(Optional.empty(), Collections.emptyList())));
    when(koskiService.haeKoskiOppijat(eq(HAKUKOHDE_OID), any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              SuoritustiedotDTO suoritustiedot = invocation.getArgument(3);
              suoritustiedot.asetaKoskiopiskeluoikeudet(hakemus.getPersonOid(), KOSKI_JSON);
              return CompletableFuture.completedFuture(Collections.emptyMap());
            });
  }

  @Test
  public void lahtotiedotSisaltavatHakemuksetJaKoskenOpiskeluoikeudet() throws Exception {
    LaskeDTO laskeDTO =
        laskentaResurssiProvider
            .fetchResourcesForOneLaskenta(
                "uuid",
                HAKU_OID,
                HAKUKOHDE_OID,
                null,
                new AuditSession(),
                false,
                false,
                false,
                new Date())
            .get(10, TimeUnit.SECONDS);

    assertEquals(1, laskeDTO.getHakemus().size());
    HakemusDTO hakemusDTO = laskeDTO.getHakemus().get(0);
    assertEquals(HAKEMUS_OID, hakemusDTO.getHakemusoid());
    assertEquals(KOSKI_JSON, hakemusDTO.getKoskiOpiskeluoikeudetJson());
  }

  @Test
  public void lahtotiedoissaKerrotaanEttaSuoritusrekisterinTietojaEiHaettu() throws Exception {
    LaskeDTO laskeDTO =
        laskentaResurssiProvider
            .fetchResourcesForOneLaskenta(
                "uuid",
                HAKU_OID,
                HAKUKOHDE_OID,
                null,
                new AuditSession(),
                false,
                false,
                false,
                new Date())
            .get(10, TimeUnit.SECONDS);

    JsonNode json = Jackson2ObjectMapperBuilder.json().build().valueToTree(laskeDTO);
    assertTrue(json.get("suoritusrekisterinTiedotHaettu").isBoolean());
    assertFalse(json.get("suoritusrekisterinTiedotHaettu").asBoolean());
  }

  private static ValintaperusteetDTO valintaperusteet() {
    ValintatapajonoJarjestyskriteereillaDTO valintatapajono =
        new ValintatapajonoJarjestyskriteereillaDTO();
    valintatapajono.setOid("jono1");
    valintatapajono.setKaytetaanValintalaskentaa(true);
    valintatapajono.setJarjestyskriteerit(List.of(new ValintaperusteetJarjestyskriteeriDTO()));

    ValintaperusteetValinnanVaiheDTO valinnanVaihe = new ValintaperusteetValinnanVaiheDTO();
    valinnanVaihe.setValintatapajono(List.of(valintatapajono));

    ValintaperusteetDTO valintaperusteet = new ValintaperusteetDTO();
    valintaperusteet.setValinnanVaihe(valinnanVaihe);
    return valintaperusteet;
  }
}
