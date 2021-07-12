package fi.vm.sade.valinta.kooste.valintalaskenta;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetJarjestyskriteeriDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetValinnanVaiheDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoJarjestyskriteereillaDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.mocks.MockAtaruAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockOppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.KoskiService;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorFactory;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorSystem;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaStarter;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaStartParams;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusDto;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusTyyppi;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.SuoritustiedotDTO;
import io.reactivex.Observable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

public class ValintalaskentaTest {
  private static final Hakemus hakemus = new Hakemus();
  private static final AtaruHakemus ataruHakemus = new AtaruHakemus();
  private static final Date NYT = new Date();
  private final ApplicationAsyncResource applicationAsyncResource =
      mock(ApplicationAsyncResource.class);
  private final AtaruAsyncResource ataruAsyncResource = mock(AtaruAsyncResource.class);
  private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource =
      mock(SuoritusrekisteriAsyncResource.class);
  private final ValintalaskentaAsyncResource valintalaskentaAsyncResource =
      mock(ValintalaskentaAsyncResource.class);
  private final ValintaperusteetAsyncResource valintaperusteetAsyncResource =
      mock(ValintaperusteetAsyncResource.class);
  private final LaskentaSeurantaAsyncResource seurantaAsyncResource =
      mock(LaskentaSeurantaAsyncResource.class);
  private final TarjontaAsyncResource tarjontaAsyncResource = mock(TarjontaAsyncResource.class);
  private final OhjausparametritAsyncResource ohjausparametritAsyncResource =
      mock(OhjausparametritAsyncResource.class);
  private final ValintapisteAsyncResource valintapisteAsyncResource =
      mock(ValintapisteAsyncResource.class);
  private final KoskiService koskiService = mock(KoskiService.class);
  private final HakemuksetConverterUtil hakemuksetConverterUtil =
      new HakemuksetConverterUtil("9999-12-31");
  static OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource = new MockOppijanumerorekisteriAsyncResource();
  private final LaskentaActorSystem laskentaActorSystem =
      new LaskentaActorSystem(
          seurantaAsyncResource,
          new LaskentaStarter(
              ohjausparametritAsyncResource,
              valintaperusteetAsyncResource,
              seurantaAsyncResource,
              tarjontaAsyncResource),
          new LaskentaActorFactory(
              5,
              valintalaskentaAsyncResource,
              applicationAsyncResource,
              ataruAsyncResource,
              valintaperusteetAsyncResource,
              seurantaAsyncResource,
              suoritusrekisteriAsyncResource,
              tarjontaAsyncResource,
              valintapisteAsyncResource,
              koskiService,
              hakemuksetConverterUtil,
              oppijanumerorekisteriAsyncResource),
          8);
  private final String hakukohde1Oid = "h1";
  private final String hakukohde2Oid = "h2";
  private final String hakukohde3Oid = "h3";
  private final String valintatapajono1Oid = "vj1";
  private final String valintatapajono2Oid = "vj2";
  private final String valintatapajono3Oid = "vj3";
  private final String ataruHakukohdeOid = "1.2.246.562.20.90242725084";
  private final String ataruHakukohdeOid2 = "1.2.246.562.20.38103650677";
  private final String uuid = "uuid";
  private final String hakuOid = "hakuOid";
  private final String ataruHakuOid = "1.2.246.562.29.805206009510";
  private final HakuV1RDTO hakuDTO = new HakuV1RDTO();
  private final HakuV1RDTO ataruHakuDTO = new HakuV1RDTO();
  private final Oppija oppijaFromSure1 = new Oppija();
  private final Oppija anonOppijaFromSure = new Oppija();
  private final List<String> hakemusOids = new ArrayList<>();
  private final List<HakukohdeJaOrganisaatio> hakukohdeJaOrganisaatios =
      Arrays.asList(
          new HakukohdeJaOrganisaatio(hakukohde1Oid, "o1"),
          new HakukohdeJaOrganisaatio(hakukohde2Oid, "o2"),
          new HakukohdeJaOrganisaatio(hakukohde3Oid, "o3"));
  private List<HakukohdeJaOrganisaatio> ataruHakukohdeJaOrganisaatios =
      Arrays.asList(
          new HakukohdeJaOrganisaatio(ataruHakukohdeOid, "Organisaatio1"),
          new HakukohdeJaOrganisaatio(ataruHakukohdeOid2, "Organisaatio2"));
  private final AuditSession auditSession =
      new AuditSession(
          "1.2.3.4.5",
          Collections.singletonList("APP_VALINTA_EVERYTHING_CRUD"),
          "Firefox",
          "127.0.0.1");

  @Before
  public void setUpTestData() {
    hakemus.setPersonOid("personOid");
    hakemusOids.add(hakemus.getOid());
    hakuDTO.setOid(hakuOid);
    ataruHakuDTO.setOid(ataruHakuOid);
    ataruHakuDTO.setAtaruLomakeAvain("ataru-lomake-avain");
    String personOid1 = "1.2.246.562.24.86368188549";
    ataruHakemus.setHakemusOid("ataruHakemusOid");
    ataruHakemus.setPersonOid(personOid1);
    oppijaFromSure1.setOppijanumero(personOid1);
    anonOppijaFromSure.setOppijanumero("personOid");
    PisteetWithLastModified pisteet =
        new PisteetWithLastModified(
            Optional.empty(),
            Collections.singletonList(
                new Valintapisteet(
                    hakemus.getOid(),
                    hakemus.getPersonOid(),
                    "Frank",
                    "Tester",
                    Collections.emptyList())));

    PisteetWithLastModified ataruPisteet =
        new PisteetWithLastModified(
            Optional.empty(),
            Collections.singletonList(
                new Valintapisteet(
                    ataruHakemus.getHakemusOid(),
                    ataruHakemus.getPersonOid(),
                    "Zl2A5",
                    "TAUsuL4BQc",
                    Collections.emptyList())));

    when(valintaperusteetAsyncResource.haeValintaperusteet(any(), any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(
                    valintaperusteetWithValintatapajonoUsingValintalaskenta(
                        true, true, valintatapajono1Oid))));
    when(valintaperusteetAsyncResource.haeHakijaryhmat(hakukohde1Oid))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(valintaperusteetAsyncResource.haeHakijaryhmat(hakukohde2Oid))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(valintaperusteetAsyncResource.haeHakijaryhmat(hakukohde3Oid))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(valintaperusteetAsyncResource.haeHakijaryhmat(ataruHakukohdeOid))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(valintaperusteetAsyncResource.haeHakijaryhmat(ataruHakukohdeOid2))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

    when(valintapisteAsyncResource.getValintapisteetWithHakemusOidsAsFuture(
            anyList(), any(AuditSession.class)))
        .thenReturn(CompletableFuture.completedFuture(pisteet));
    when(valintapisteAsyncResource.getValintapisteet(
            eq(hakuOid), eq(hakukohde1Oid), any(AuditSession.class)))
        .thenReturn(CompletableFuture.completedFuture(pisteet));
    when(valintapisteAsyncResource.getValintapisteet(
            eq(hakuOid), eq(hakukohde2Oid), any(AuditSession.class)))
        .thenReturn(CompletableFuture.completedFuture(pisteet));
    when(valintapisteAsyncResource.getValintapisteet(
            eq(hakuOid), eq(hakukohde3Oid), any(AuditSession.class)))
        .thenReturn(CompletableFuture.completedFuture(pisteet));
    when(valintapisteAsyncResource.getValintapisteet(
            eq(ataruHakuOid), eq(ataruHakukohdeOid), any(AuditSession.class)))
        .thenReturn(CompletableFuture.completedFuture(ataruPisteet));
    when(valintapisteAsyncResource.getValintapisteet(
            eq(ataruHakuOid), eq(ataruHakukohdeOid2), any(AuditSession.class)))
        .thenReturn(CompletableFuture.completedFuture(ataruPisteet));

    when(valintalaskentaAsyncResource.laskeKaikki(any(), any(SuoritustiedotDTO.class)))
        .thenReturn(Observable.just("OK"));

    when(suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohde1Oid, hakuOid))
        .thenReturn(Observable.just(Collections.singletonList(new Oppija())));
    when(suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohde2Oid, hakuOid))
        .thenReturn(Observable.just(Collections.singletonList(new Oppija())));
    when(suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohde3Oid, hakuOid))
        .thenReturn(Observable.just(Collections.singletonList(new Oppija())));
    when(suoritusrekisteriAsyncResource.getOppijatByHakukohde(ataruHakukohdeOid, ataruHakuOid))
        .thenReturn(Observable.just(Collections.singletonList(new Oppija())));
    when(suoritusrekisteriAsyncResource.getOppijatByHakukohde(ataruHakukohdeOid2, ataruHakuOid))
        .thenReturn(Observable.just(Collections.singletonList(new Oppija())));
    when(suoritusrekisteriAsyncResource.getSuorituksetByOppijas(List.of(personOid1), ataruHakuOid))
        .thenReturn(CompletableFuture.completedFuture(Collections.singletonList(oppijaFromSure1)));
    when(suoritusrekisteriAsyncResource.getSuorituksetByOppijas(List.of(personOid1), hakuOid))
        .thenReturn(CompletableFuture.completedFuture(Collections.singletonList(oppijaFromSure1)));
    when(suoritusrekisteriAsyncResource.getSuorituksetByOppijas(List.of("personOid"), ataruHakuOid))
        .thenReturn(
            CompletableFuture.completedFuture(Collections.singletonList(anonOppijaFromSure)));
    when(suoritusrekisteriAsyncResource.getSuorituksetByOppijas(List.of("personOid"), hakuOid))
        .thenReturn(
            CompletableFuture.completedFuture(Collections.singletonList(anonOppijaFromSure)));
    when(suoritusrekisteriAsyncResource.getSuorituksetByOppijas(
            Collections.emptyList(), ataruHakuOid))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(suoritusrekisteriAsyncResource.getSuorituksetByOppijas(Collections.emptyList(), hakuOid))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

    when(tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(hakuOid))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));
    when(tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(ataruHakuOid))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));
  }

  @Test
  public void onnistuneestaValintalaskennastaPidetaanKirjaaSeurantapalveluun()
      throws InterruptedException {
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde1Oid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(new HakuappHakemusWrapper(hakemus))));
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde2Oid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(new HakuappHakemusWrapper(hakemus))));
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde3Oid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(new HakuappHakemusWrapper(hakemus))));
    when(seurantaAsyncResource.merkkaaHakukohteenTila(
            uuid, hakukohde1Oid, HakukohdeTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.noContent().build()));
    when(seurantaAsyncResource.merkkaaHakukohteenTila(
            uuid, hakukohde2Oid, HakukohdeTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.noContent().build()));
    when(seurantaAsyncResource.merkkaaHakukohteenTila(
            uuid, hakukohde3Oid, HakukohdeTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.noContent().build()));
    when(seurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.noContent().build()));
    when(seurantaAsyncResource.otaSeuraavaLaskentaTyonAlle())
        .thenReturn(Observable.just(Optional.empty()));
    when(koskiService.haeKoskiOppijat(
            any(String.class), any(), any(), any(SuoritustiedotDTO.class), any(Date.class)))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));

    LaskentaStartParams laskentaJaHaku =
        new LaskentaStartParams(
            auditSession,
            uuid,
            hakuOid,
            false,
            null,
            null,
            hakukohdeJaOrganisaatios,
            LaskentaTyyppi.HAKUKOHDE);

    laskentaActorSystem.suoritaValintalaskentaKerralla(hakuDTO, null, laskentaJaHaku);
    Thread.sleep(500);

    verify(seurantaAsyncResource).otaSeuraavaLaskentaTyonAlle();
    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(uuid, hakukohde1Oid, HakukohdeTila.VALMIS, Optional.empty());
    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(uuid, hakukohde2Oid, HakukohdeTila.VALMIS, Optional.empty());
    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(uuid, hakukohde3Oid, HakukohdeTila.VALMIS, Optional.empty());
    verify(seurantaAsyncResource).merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty());
    Mockito.verifyNoMoreInteractions(seurantaAsyncResource);
  }

  @Test
  public void valintaLaskentaHakeeKoskestaTiedot() throws InterruptedException {
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde1Oid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(new HakuappHakemusWrapper(hakemus))));
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde2Oid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(new HakuappHakemusWrapper(hakemus))));
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde3Oid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(new HakuappHakemusWrapper(hakemus))));
    when(seurantaAsyncResource.merkkaaHakukohteenTila(
            uuid, hakukohde1Oid, HakukohdeTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.noContent().build()));
    when(seurantaAsyncResource.merkkaaHakukohteenTila(
            uuid, hakukohde2Oid, HakukohdeTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.noContent().build()));
    when(seurantaAsyncResource.merkkaaHakukohteenTila(
            uuid, hakukohde3Oid, HakukohdeTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.noContent().build()));
    when(seurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.noContent().build()));
    when(seurantaAsyncResource.otaSeuraavaLaskentaTyonAlle())
        .thenReturn(Observable.just(Optional.empty()));
    when(koskiService.haeKoskiOppijat(
            any(String.class), any(), any(), any(SuoritustiedotDTO.class), any(Date.class)))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));
    when(valintalaskentaAsyncResource.laskeKaikki(
            any(LaskeDTO.class), any(SuoritustiedotDTO.class)))
        .thenAnswer(
            invocationOnMock -> {
              LaskeDTO laskeDTO = invocationOnMock.getArgument(0);
              assertThat(laskeDTO.getHakemus(), hasSize(1));
              return Observable.fromFuture(CompletableFuture.completedFuture("OK"));
            });

    LaskentaStartParams laskentaJaHaku =
        new LaskentaStartParams(
            auditSession,
            uuid,
            hakuOid,
            false,
            null,
            null,
            hakukohdeJaOrganisaatios,
            LaskentaTyyppi.HAKUKOHDE);

    laskentaActorSystem.suoritaValintalaskentaKerralla(hakuDTO, null, laskentaJaHaku);
    Thread.sleep(500);

    verify(seurantaAsyncResource).otaSeuraavaLaskentaTyonAlle();
    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(uuid, hakukohde1Oid, HakukohdeTila.VALMIS, Optional.empty());
    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(uuid, hakukohde2Oid, HakukohdeTila.VALMIS, Optional.empty());
    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(uuid, hakukohde3Oid, HakukohdeTila.VALMIS, Optional.empty());
    verify(seurantaAsyncResource).merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty());
    verify(valintalaskentaAsyncResource, times(3))
        .laskeKaikki(any(LaskeDTO.class), any(SuoritustiedotDTO.class));
    verify(koskiService, times(3))
        .haeKoskiOppijat(
            any(String.class), any(), any(), any(SuoritustiedotDTO.class), any(Date.class));
    Mockito.verifyNoMoreInteractions(valintalaskentaAsyncResource);
    Mockito.verifyNoMoreInteractions(seurantaAsyncResource);
  }

  @Test
  public void onnistuneestaValintalaskennastaAtaruHaullePidetaanKirjaaSeurantapalveluun()
      throws InterruptedException {
    when(ataruAsyncResource.getApplicationsByHakukohde(ataruHakukohdeOid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(
                    MockAtaruAsyncResource.getAtaruHakemusWrapper(
                        "1.2.246.562.11.00000000000000000063"))));
    when(ataruAsyncResource.getApplicationsByHakukohde(ataruHakukohdeOid2))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(
                    MockAtaruAsyncResource.getAtaruHakemusWrapper(
                        "1.2.246.562.11.00000000000000000063"))));
    when(seurantaAsyncResource.merkkaaHakukohteenTila(
            uuid, ataruHakukohdeOid, HakukohdeTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.noContent().build()));
    when(seurantaAsyncResource.merkkaaHakukohteenTila(
            uuid, ataruHakukohdeOid2, HakukohdeTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.noContent().build()));
    when(seurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.noContent().build()));
    when(seurantaAsyncResource.otaSeuraavaLaskentaTyonAlle())
        .thenReturn(Observable.just(Optional.empty()));
    when(koskiService.haeKoskiOppijat(
            any(String.class), any(), any(), any(SuoritustiedotDTO.class), any(Date.class)))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));

    LaskentaStartParams laskentaJaHaku =
        new LaskentaStartParams(
            auditSession,
            uuid,
            ataruHakuOid,
            false,
            null,
            null,
            ataruHakukohdeJaOrganisaatios,
            LaskentaTyyppi.HAKUKOHDE);

    laskentaActorSystem.suoritaValintalaskentaKerralla(ataruHakuDTO, null, laskentaJaHaku);
    Thread.sleep(500);

    verify(seurantaAsyncResource).otaSeuraavaLaskentaTyonAlle();
    verify(ataruAsyncResource).getApplicationsByHakukohde(ataruHakukohdeOid);
    verify(ataruAsyncResource).getApplicationsByHakukohde(ataruHakukohdeOid2);
    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(uuid, ataruHakukohdeOid, HakukohdeTila.VALMIS, Optional.empty());
    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(uuid, ataruHakukohdeOid2, HakukohdeTila.VALMIS, Optional.empty());
    verify(seurantaAsyncResource).merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty());
    Mockito.verifyNoMoreInteractions(seurantaAsyncResource);
  }

  @Test
  public void onnistuneestaValintaryhmalaskennastaPidetaanKirjaaSeurantapalveluun()
      throws InterruptedException {
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde1Oid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(new HakuappHakemusWrapper(hakemus))));
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde2Oid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(new HakuappHakemusWrapper(hakemus))));
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde3Oid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(new HakuappHakemusWrapper(hakemus))));

    Integer vaiheenNumero = 1;

    when(valintaperusteetAsyncResource.haeValintaperusteet(eq(hakukohde1Oid), eq(vaiheenNumero)))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(
                    valintaperusteetWithValintatapajonoUsingValintalaskenta(
                        true, true, valintatapajono1Oid))));
    when(valintaperusteetAsyncResource.haeValintaperusteet(eq(hakukohde2Oid), eq(vaiheenNumero)))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(
                    valintaperusteetWithValintatapajonoUsingValintalaskenta(
                        true, true, valintatapajono2Oid))));
    when(valintaperusteetAsyncResource.haeValintaperusteet(eq(hakukohde3Oid), eq(vaiheenNumero)))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(
                    valintaperusteetWithValintatapajonoUsingValintalaskenta(
                        true, true, valintatapajono3Oid))));

    when(valintaperusteetAsyncResource.haeHakijaryhmat(eq(hakukohde1Oid)))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(valintaperusteetAsyncResource.haeHakijaryhmat(eq(hakukohde2Oid)))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(valintaperusteetAsyncResource.haeHakijaryhmat(eq(hakukohde3Oid)))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(koskiService.haeKoskiOppijat(
            any(String.class), any(), any(), any(SuoritustiedotDTO.class), any(Date.class)))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));

    LaskentaStartParams laskentaJaHaku =
        new LaskentaStartParams(
            auditSession,
            uuid,
            hakuOid,
            false,
            vaiheenNumero,
            null,
            hakukohdeJaOrganisaatios,
            LaskentaTyyppi.VALINTARYHMA);

    when(valintalaskentaAsyncResource.laskeJaSijoittele(
            eq(uuid), anyList(), any(SuoritustiedotDTO.class)))
        .thenReturn(Observable.just("Valintaryhmälaskenta onnistui"));
    when(seurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.noContent().build()));
    when(seurantaAsyncResource.otaSeuraavaLaskentaTyonAlle())
        .thenReturn(Observable.just(Optional.empty()));

    laskentaActorSystem.suoritaValintalaskentaKerralla(hakuDTO, null, laskentaJaHaku);
    Thread.sleep(500);

    verify(valintaperusteetAsyncResource).haeValintaperusteet(eq(hakukohde1Oid), eq(vaiheenNumero));
    verify(valintaperusteetAsyncResource).haeValintaperusteet(eq(hakukohde2Oid), eq(vaiheenNumero));
    verify(valintaperusteetAsyncResource).haeValintaperusteet(eq(hakukohde3Oid), eq(vaiheenNumero));
    verify(valintaperusteetAsyncResource).haeHakijaryhmat(eq(hakukohde1Oid));
    verify(valintaperusteetAsyncResource).haeHakijaryhmat(eq(hakukohde2Oid));
    verify(valintaperusteetAsyncResource).haeHakijaryhmat(eq(hakukohde3Oid));
    Mockito.verifyNoMoreInteractions(valintaperusteetAsyncResource);

    verify(valintalaskentaAsyncResource)
        .laskeJaSijoittele(eq(uuid), anyList(), any(SuoritustiedotDTO.class));
    Mockito.verifyNoMoreInteractions(valintalaskentaAsyncResource);

    verify(seurantaAsyncResource).otaSeuraavaLaskentaTyonAlle();
    verify(seurantaAsyncResource).merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty());
    Mockito.verifyNoMoreInteractions(seurantaAsyncResource);
  }

  @Test
  public void puuttuvaJonokriteeriAiheuttaaLaskennanEpaonnistumisen() throws InterruptedException {
    int vaiheenNumero = 1;

    when(valintaperusteetAsyncResource.haeValintaperusteet(hakukohde1Oid, vaiheenNumero))
        .thenReturn(
            CompletableFuture.completedFuture(
                Arrays.asList(
                    valintaperusteetWithValintatapajonoUsingValintalaskenta(
                        true, true, valintatapajono1Oid),
                    valintaperusteetWithValintatapajonoUsingValintalaskenta(
                        true, false, valintatapajono2Oid),
                    valintaperusteetWithValintatapajonoUsingValintalaskenta(
                        false, true, valintatapajono3Oid))));
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde1Oid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(new HakuappHakemusWrapper(hakemus))));
    when(koskiService.haeKoskiOppijat(
            any(String.class), any(), any(), any(SuoritustiedotDTO.class), any(Date.class)))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));

    LaskentaStartParams laskentaJaHaku =
        new LaskentaStartParams(
            auditSession,
            uuid,
            hakuOid,
            false,
            vaiheenNumero,
            null,
            Collections.singletonList(new HakukohdeJaOrganisaatio(hakukohde1Oid, "o1")),
            LaskentaTyyppi.HAKUKOHDE);
    laskentaActorSystem.suoritaValintalaskentaKerralla(hakuDTO, null, laskentaJaHaku);
    Thread.sleep(500);

    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(
            eq(uuid),
            eq(hakukohde1Oid),
            eq(HakukohdeTila.KESKEYTETTY),
            getIlmoitusDtoOptional(
                "Hakukohteen h1 valintatapajonolla vj2 on joko valintalaskenta ilman jonokriteereitä"));
    verify(seurantaAsyncResource).merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty());
    Mockito.verifyNoMoreInteractions(seurantaAsyncResource);
  }

  private ValintaperusteetDTO valintaperusteetWithValintatapajonoUsingValintalaskenta(
      boolean kaytetaanValintalaskentaa, boolean hasJonokriteeri, String valintatapajonoOid) {
    ValintaperusteetDTO valintaperusteet = new ValintaperusteetDTO();

    ValintaperusteetValinnanVaiheDTO valinnanvaihe = new ValintaperusteetValinnanVaiheDTO();
    valintaperusteet.setValinnanVaihe(valinnanvaihe);

    ValintatapajonoJarjestyskriteereillaDTO valintatapajono =
        new ValintatapajonoJarjestyskriteereillaDTO();
    valintatapajono.setOid(valintatapajonoOid);
    valintatapajono.setKaytetaanValintalaskentaa(kaytetaanValintalaskentaa);
    valinnanvaihe.setValintatapajono(Collections.singletonList(valintatapajono));

    if (hasJonokriteeri) {
      valintatapajono.setJarjestyskriteerit(
          Collections.singletonList(new ValintaperusteetJarjestyskriteeriDTO()));
    }

    return valintaperusteet;
  }

  @Test
  public void valintalaskentaEiKaytossaAiheuttaaLaskennanEpaonnistumisen()
      throws InterruptedException {
    int vaiheenNumero = 1;

    when(valintaperusteetAsyncResource.haeValintaperusteet(hakukohde1Oid, vaiheenNumero))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(
                    valintaperusteetWithValintatapajonoUsingValintalaskenta(
                        false, false, valintatapajono1Oid))));
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde1Oid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(new HakuappHakemusWrapper(hakemus))));
    when(koskiService.haeKoskiOppijat(
            any(String.class), any(), any(), any(SuoritustiedotDTO.class), any(Date.class)))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));

    LaskentaStartParams laskentaJaHaku =
        new LaskentaStartParams(
            auditSession,
            uuid,
            hakuOid,
            false,
            vaiheenNumero,
            null,
            Collections.singletonList(new HakukohdeJaOrganisaatio(hakukohde1Oid, "o1")),
            LaskentaTyyppi.HAKUKOHDE);
    laskentaActorSystem.suoritaValintalaskentaKerralla(hakuDTO, null, laskentaJaHaku);
    Thread.sleep(500);

    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(
            eq(uuid),
            eq(hakukohde1Oid),
            eq(HakukohdeTila.KESKEYTETTY),
            getIlmoitusDtoOptional(
                String.format(
                    "Hakukohteen %s valittujen valinnanvaiheiden valintatapajonoissa ei käytetä valintalaskentaa",
                    hakukohde1Oid)));
    verify(seurantaAsyncResource).merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty());
    Mockito.verifyNoMoreInteractions(seurantaAsyncResource);
  }

  @Test
  public void epaonnistuneetLaskennatKirjataanSeurantapalveluun() throws InterruptedException {
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde1Oid))
        .thenReturn(
            CompletableFuture.failedFuture(
                new RuntimeException(
                    getClass().getSimpleName()
                        + " : Ei saatu haettua hakemuksia kohteelle "
                        + hakukohde1Oid)));
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde2Oid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(new HakuappHakemusWrapper(hakemus))));
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde3Oid))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(new HakuappHakemusWrapper(hakemus))));
    when(seurantaAsyncResource.merkkaaHakukohteenTila(
            uuid, hakukohde2Oid, HakukohdeTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.ok().build()));
    when(seurantaAsyncResource.merkkaaHakukohteenTila(
            uuid, hakukohde3Oid, HakukohdeTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.ok().build()));
    when(seurantaAsyncResource.merkkaaHakukohteenTila(
            uuid, hakukohde3Oid, HakukohdeTila.KESKEYTETTY, Optional.empty()))
        .thenReturn(Observable.just(Response.ok().build()));
    when(koskiService.haeKoskiOppijat(
            any(String.class), any(), any(), any(SuoritustiedotDTO.class), any(Date.class)))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));

    LaskentaStartParams laskentaJaHaku =
        new LaskentaStartParams(
            auditSession,
            uuid,
            hakuOid,
            false,
            null,
            null,
            hakukohdeJaOrganisaatios,
            LaskentaTyyppi.HAKUKOHDE);

    laskentaActorSystem.suoritaValintalaskentaKerralla(hakuDTO, null, laskentaJaHaku);
    Thread.sleep(500);

    verify(valintapisteAsyncResource, times(2))
        .getValintapisteetWithHakemusOidsAsFuture(eq(hakemusOids), any(AuditSession.class));
    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(uuid, hakukohde2Oid, HakukohdeTila.VALMIS, Optional.empty());
    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(uuid, hakukohde3Oid, HakukohdeTila.VALMIS, Optional.empty());
    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(
            eq(uuid),
            eq(hakukohde1Oid),
            eq(HakukohdeTila.KESKEYTETTY),
            getIlmoitusDtoOptional("Ei saatu haettua hakemuksia kohteelle " + hakukohde1Oid));
    verify(seurantaAsyncResource).merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty());
    Mockito.verifyNoMoreInteractions(seurantaAsyncResource);
  }

  @Test
  public void epaonnistuneetAtaruLaskennatKirjataanSeurantapalveluun() throws InterruptedException {
    final String ataruHakemusOid = "1.2.246.562.11.00000000000000000063";
    final List<String> ataruHakemusOids = new ArrayList<>();
    ataruHakemusOids.add(ataruHakemusOid);
    when(ataruAsyncResource.getApplicationsByHakukohde(ataruHakukohdeOid))
        .thenReturn(
            CompletableFuture.failedFuture(
                new RuntimeException(
                    getClass().getSimpleName()
                        + " : Ei saatu haettua hakemuksia kohteelle "
                        + ataruHakukohdeOid)));
    when(ataruAsyncResource.getApplicationsByHakukohde(ataruHakukohdeOid2))
        .thenReturn(
            CompletableFuture.completedFuture(
                Collections.singletonList(
                    MockAtaruAsyncResource.getAtaruHakemusWrapper(ataruHakemusOid))));
    when(seurantaAsyncResource.merkkaaHakukohteenTila(
            uuid, ataruHakukohdeOid2, HakukohdeTila.VALMIS, Optional.empty()))
        .thenReturn(Observable.just(Response.ok().build()));
    when(seurantaAsyncResource.merkkaaHakukohteenTila(
            uuid, ataruHakukohdeOid, HakukohdeTila.KESKEYTETTY, Optional.empty()))
        .thenReturn(Observable.just(Response.ok().build()));
    when(koskiService.haeKoskiOppijat(
            any(String.class), any(), any(), any(SuoritustiedotDTO.class), any(Date.class)))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));

    LaskentaStartParams laskentaJaHaku =
        new LaskentaStartParams(
            auditSession,
            uuid,
            ataruHakuOid,
            false,
            null,
            null,
            ataruHakukohdeJaOrganisaatios,
            LaskentaTyyppi.HAKUKOHDE);

    laskentaActorSystem.suoritaValintalaskentaKerralla(ataruHakuDTO, null, laskentaJaHaku);
    Thread.sleep(500);

    verify(ataruAsyncResource, times(2)).getApplicationsByHakukohde(ataruHakukohdeOid);
    verify(ataruAsyncResource).getApplicationsByHakukohde(ataruHakukohdeOid2);
    verify(valintapisteAsyncResource, times(1))
        .getValintapisteetWithHakemusOidsAsFuture(eq(ataruHakemusOids), any(AuditSession.class));
    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(uuid, ataruHakukohdeOid2, HakukohdeTila.VALMIS, Optional.empty());
    verify(seurantaAsyncResource)
        .merkkaaHakukohteenTila(
            eq(uuid),
            eq(ataruHakukohdeOid),
            eq(HakukohdeTila.KESKEYTETTY),
            getIlmoitusDtoOptional("Ei saatu haettua hakemuksia kohteelle " + ataruHakukohdeOid));
    verify(seurantaAsyncResource).merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty());
    Mockito.verifyNoMoreInteractions(seurantaAsyncResource);
  }

  private Optional<IlmoitusDto> getIlmoitusDtoOptional(String odotettuOtsikonSisalto) {
    return argThat(
        new ArgumentMatcher<>() {
          @Override
          public boolean matches(Optional<IlmoitusDto> argument) {
            if (argument.isPresent()) {
              IlmoitusDto ilmoitusDto = argument.get();
              return odotettuIlmoitustyyppi.equals(ilmoitusDto.getTyyppi())
                  && ilmoitusDto.getOtsikko().contains(odotettuOtsikonSisalto);
            } else {
              return false;
            }
          }

          private final IlmoitusTyyppi odotettuIlmoitustyyppi = IlmoitusTyyppi.VIRHE;

          @Override
          public String toString() {
            return IlmoitusDto.class.getSimpleName()
                + ", jossa "
                + odotettuIlmoitustyyppi
                + " ja otsikossa \""
                + odotettuOtsikonSisalto
                + "\"";
          }
        });
  }

  @Test
  public void poikkeukseenEpaonnistuneitaLaskentojaEiKirjataSeurantapalveluun()
      throws InterruptedException {
    when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde1Oid))
        .thenThrow(
            new RuntimeException(
                getClass().getSimpleName()
                    + " : Ei saatu taaskaan haettua hakemuksia kohteelle "
                    + hakukohde1Oid));

    laskentaActorSystem.suoritaValintalaskentaKerralla(
        hakuDTO,
        null,
        new LaskentaStartParams(
            auditSession,
            uuid,
            hakuOid,
            false,
            null,
            null,
            hakukohdeJaOrganisaatios,
            LaskentaTyyppi.HAKUKOHDE));
    Thread.sleep(500);

    Mockito.verifyNoMoreInteractions(seurantaAsyncResource);
  }
}
