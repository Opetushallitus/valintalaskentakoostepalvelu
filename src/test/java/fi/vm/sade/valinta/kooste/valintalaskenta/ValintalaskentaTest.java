package fi.vm.sade.valinta.kooste.valintalaskenta;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetJarjestyskriteeriDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetValinnanVaiheDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoJarjestyskriteereillaDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valintalaskenta.domain.dto.SuoritustiedotDTO;
import io.reactivex.Observable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;

public class ValintalaskentaTest {
  private static final Hakemus hakemus = new Hakemus();
  private static final AtaruHakemus ataruHakemus = new AtaruHakemus();
  private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource =
      mock(SuoritusrekisteriAsyncResource.class);
  private final ValintalaskentaAsyncResource valintalaskentaAsyncResource =
      mock(ValintalaskentaAsyncResource.class);
  private final ValintaperusteetAsyncResource valintaperusteetAsyncResource =
      mock(ValintaperusteetAsyncResource.class);
  private final TarjontaAsyncResource tarjontaAsyncResource = mock(TarjontaAsyncResource.class);

  private final ValintapisteAsyncResource valintapisteAsyncResource =
      mock(ValintapisteAsyncResource.class);
  private final String hakukohde1Oid = "h1";
  private final String hakukohde2Oid = "h2";
  private final String hakukohde3Oid = "h3";
  private final String valintatapajono1Oid = "vj1";
  private final String ataruHakukohdeOid = "1.2.246.562.20.90242725084";
  private final String ataruHakukohdeOid2 = "1.2.246.562.20.38103650677";
  private final String hakuOid = "hakuOid";
  private final String ataruHakuOid = "1.2.246.562.29.805206009510";

  private final Oppija oppijaFromSure1 = new Oppija();
  private final Oppija anonOppijaFromSure = new Oppija();
  private final List<String> hakemusOids = new ArrayList<>();

  @BeforeEach
  public void setUpTestData() {
    hakemus.setPersonOid("personOid");
    hakemusOids.add(hakemus.getOid());
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
    when(suoritusrekisteriAsyncResource.getSuorituksetByOppijas(
            List.of(personOid1), ataruHakuOid, false))
        .thenReturn(CompletableFuture.completedFuture(Collections.singletonList(oppijaFromSure1)));
    when(suoritusrekisteriAsyncResource.getSuorituksetByOppijas(
            List.of(personOid1), hakuOid, false))
        .thenReturn(CompletableFuture.completedFuture(Collections.singletonList(oppijaFromSure1)));
    when(suoritusrekisteriAsyncResource.getSuorituksetByOppijas(
            List.of("personOid"), ataruHakuOid, false))
        .thenReturn(
            CompletableFuture.completedFuture(Collections.singletonList(anonOppijaFromSure)));
    when(suoritusrekisteriAsyncResource.getSuorituksetByOppijas(
            List.of("personOid"), hakuOid, false))
        .thenReturn(
            CompletableFuture.completedFuture(Collections.singletonList(anonOppijaFromSure)));
    when(suoritusrekisteriAsyncResource.getSuorituksetByOppijas(
            Collections.emptyList(), ataruHakuOid, false))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(suoritusrekisteriAsyncResource.getSuorituksetByOppijas(
            Collections.emptyList(), hakuOid, false))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

    when(tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(hakuOid))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));
    when(tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(ataruHakuOid))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));
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
}
