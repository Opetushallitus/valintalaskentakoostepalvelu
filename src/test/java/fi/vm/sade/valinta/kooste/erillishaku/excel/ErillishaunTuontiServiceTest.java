package fi.vm.sade.valinta.kooste.erillishaku.excel;

import static fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi.SYNTYMAAIKAFORMAT;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi.SYNTYMAAIKAFORMAT_JSON;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishaunTuontiServiceTest.PERSON_1_OID;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishaunTuontiServiceTest.getDummyHaku;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuHenkiloOidilla;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuSyntymaAjalla;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuTuntemattomallaKielella;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.kkHakuToisenAsteenValintatuloksella;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.puutteellisiaTietojaAutotayttoaVarten;
import static io.reactivex.Observable.just;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuProsessiDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiService;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Metadata;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.ValintatulosUpdateStatus;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Valinnantulos;
import fi.vm.sade.valinta.kooste.mocks.*;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class ErillishaunTuontiServiceTest {
  protected static final String PERSON_1_OID = "1.2.246.562.24.64735725450";
  protected static final String PERSON_2_OID = "1.2.246.562.24.64735725451";

  public static TarjontaAsyncResource mockTarjonta() {
    final TarjontaAsyncResource tarjontaAsyncResource = mock(TarjontaAsyncResource.class);
    when(tarjontaAsyncResource.haeHaku(Mockito.any()))
        .thenReturn(CompletableFuture.completedFuture(getDummyHaku()));
    return tarjontaAsyncResource;
  }

  public static Haku getDummyHaku() {
    HakuV1RDTO dto = new HakuV1RDTO();
    dto.setTarjoajaOids(new String[0]);
    Haku haku = new Haku(dto);
    return haku;
  }

  @Nested
  public final class Autotaytto extends ErillisHakuTuontiTestCase {
    @Test
    public void
        tuodaanHylattyjaJaPeruutettujaJaVarallaOleviaPuutteellisinTiedoinAutosyotonTestaamiseksi() {
      importData(puutteellisiaTietojaAutotayttoaVarten());

      assertEquals(1, erillishaunValinnantulokset.size());
      assertEquals(5, erillishaunValinnantulokset.get("jono1").size()); // KESKEN-tilainen puuttuu
      erillishaunValinnantulokset
          .get("jono1")
          .forEach(
              v -> {
                assertEquals(IlmoittautumisTila.EI_TEHTY, v.getIlmoittautumistila());
                assertEquals(ValintatuloksenTila.KESKEN, v.getVastaanottotila());
              });
    }
  }

  @Nested
  public final class KorkeaKouluHaku extends ErillisHakuTuontiTestCase {
    @Test
    public void tuodaanToisenAsteenValintatilojaKorkeaKoulunHakuun() {
      importData(kkHakuToisenAsteenValintatuloksella());

      // assertEquals(1, henkiloAsyncResource.henkiloPrototyypit.size());
      assertEquals(1, applicationAsyncResource.results.size());
      assertEquals(1, erillishaunValinnantulokset.size());
      assertEquals(1, erillishaunValinnantulokset.get("jono1").size());
      erillishaunValinnantulokset
          .get("jono1")
          .forEach(
              v -> {
                assertEquals(IlmoittautumisTila.EI_TEHTY, v.getIlmoittautumistila());
                assertEquals(ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI, v.getVastaanottotila());
              });
    }
  }

  @Nested
  public final class HetullaJaSyntymaAjalla extends ErillisHakuTuontiTestCase {
    @Test
    public void tuontiSuoritetaan() {
      importData(kkHakuToisenAsteenValintatuloksella());

      /* assertEquals(1, henkiloAsyncResource.henkiloPrototyypit.size());
      final HenkiloCreateDTO henkilo = henkiloAsyncResource.henkiloPrototyypit.get(0);
      assertEquals("Tuomas", henkilo.etunimet);
      assertEquals("Tuomas", henkilo.kutsumanimi);
      assertEquals("Hakkarainen", henkilo.sukunimi);
      assertEquals(MockData.hetu, henkilo.hetu);
      assertNotNull(henkilo.syntymaaika);
      assertEquals(HenkiloTyyppi.OPPIJA, henkilo.henkiloTyyppi); */

      assertEquals(1, applicationAsyncResource.results.size());
      applicationAsyncResource.results.get(0);
      final MockAtaruAsyncResource.Result appResult = applicationAsyncResource.results.get(0);
      assertEquals(1, appResult.hakemusPrototyypit.size());
      final AtaruHakemusPrototyyppi hakemusProto = appResult.hakemusPrototyypit.iterator().next();
      assertEquals("haku1", hakemusProto.getHakuOid());
      assertEquals("kohde1", hakemusProto.getHakukohdeOid());
      // assertEquals("hakija1", hakemusProto.getHakijaOid());
      assertEquals(MockData.hetu, hakemusProto.getHenkilotunnus());
      assertEquals("Tuomas", hakemusProto.getEtunimi());
      assertEquals("Hakkarainen", hakemusProto.getSukunimi());
      assertEquals("01.01.1901", hakemusProto.getSyntymaAika());
      assertEquals(Maksuvelvollisuus.REQUIRED, hakemusProto.getMaksuvelvollisuus());

      assertEquals(1, erillishaunValinnantulokset.size());
      assertEquals(1, erillishaunValinnantulokset.get("jono1").size());
      erillishaunValinnantulokset
          .get("jono1")
          .forEach(
              v -> {
                assertEquals(IlmoittautumisTila.EI_TEHTY, v.getIlmoittautumistila());
                assertEquals(ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI, v.getVastaanottotila());
                assertEquals(MockData.valintatapajonoOid, v.getValintatapajonoOid());
                assertEquals("hakemus1", v.getHakemusOid());
                // assertEquals("hakija1", v.getHenkiloOid());
                assertEquals(true, v.getJulkaistavissa());
                assertEquals(true, v.getEhdollisestiHyvaksyttavissa());
                assertEquals(MockData.kohdeOid, v.getHakukohdeOid());
              });
    }
  }

  @Nested
  public final class SyntymaAjalla extends ErillisHakuTuontiTestCase {
    @Test
    public void tuodaan() {
      importData(erillisHakuSyntymaAjalla());

      assertEquals(1, applicationAsyncResource.results.size());
      applicationAsyncResource.results.get(0);
      final MockAtaruAsyncResource.Result appResult = applicationAsyncResource.results.get(0);
      assertEquals(1, appResult.hakemusPrototyypit.size());
      final AtaruHakemusPrototyyppi hakemusProto = appResult.hakemusPrototyypit.iterator().next();
      assertEquals("haku1", hakemusProto.getHakuOid());
      assertEquals("kohde1", hakemusProto.getHakukohdeOid());
      // assertEquals("hakija1", hakemusProto.getHakijaOid());
      assertEquals("Tuomas", hakemusProto.getEtunimi());
      assertEquals("Hakkarainen", hakemusProto.getSukunimi());
      assertEquals("01.01.1901", hakemusProto.getSyntymaAika());
      assertEquals(Maksuvelvollisuus.NOT_REQUIRED, hakemusProto.getMaksuvelvollisuus());
    }
  }

  @Nested
  public final class HetullaJaSyntymaAjallaJaOidillaJokaOnEriKuinHenkilopalvelustaPalaava
      extends ErillisHakuTuontiTestCase {
    @Test
    public void tuontiSuoritetaan() {
      String hakemusOidOnHakemusAndSijoittelu = "hakija1";
      String personOidHenkiloPalvelusta = "eri.henkiloOid.henkiloPalvelusta";
      ErillishakuRivi erillishakuRivi =
          createRow("101275-937P", hakemusOidOnHakemusAndSijoittelu, "hakemus1");
      List<HenkiloCreateDTO> henkiloPrototyypit = new ArrayList<>();
      MockOppijanumerorekisteriAsyncResource mockHenkiloAsyncResource =
          new MockOppijanumerorekisteriAsyncResource(
              dtos -> {
                HenkiloPerustietoDto henkiloJollaOnEriOid =
                    MockOppijanumerorekisteriAsyncResource.toHenkiloPerustietoDto(dtos.get(0));
                henkiloJollaOnEriOid.setOidHenkilo(personOidHenkiloPalvelusta);
                henkiloPrototyypit.addAll(dtos);
                return Futures.immediateFuture(singletonList(henkiloJollaOnEriOid));
              });
      importRows(singletonList(erillishakuRivi), mockHenkiloAsyncResource);

      /* assertEquals(1, henkiloPrototyypit.size());
      final HenkiloCreateDTO henkilo = henkiloPrototyypit.get(0);
      assertEquals(erillishakuRivi.getEtunimi(), henkilo.etunimet);
      assertEquals(erillishakuRivi.getEtunimi(), henkilo.kutsumanimi);
      assertEquals(erillishakuRivi.getSukunimi(), henkilo.sukunimi);
      assertEquals(erillishakuRivi.getHenkilotunnus(), henkilo.hetu);
      LocalDate erillishakuSyntymaAika =
          LocalDate.parse(erillishakuRivi.getSyntymaAika(), SYNTYMAAIKAFORMAT);
      LocalDate henkiloSyntymaAika = LocalDate.parse(henkilo.syntymaaika, SYNTYMAAIKAFORMAT_JSON);
      assertEquals(erillishakuSyntymaAika, henkiloSyntymaAika);
      assertEquals(HenkiloTyyppi.OPPIJA, henkilo.henkiloTyyppi); */

      assertEquals(1, erillishaunValinnantulokset.size());
      assertEquals(1, erillishaunValinnantulokset.get("jono1").size());
      erillishaunValinnantulokset
          .get("jono1")
          .forEach(
              v -> {
                assertEquals(IlmoittautumisTila.EI_TEHTY, v.getIlmoittautumistila());
                assertEquals(ValintatuloksenTila.KESKEN, v.getVastaanottotila());
                assertEquals(MockData.valintatapajonoOid, v.getValintatapajonoOid());
                assertEquals("hakemus1", v.getHakemusOid());
                // assertEquals(personOidHenkiloPalvelusta, v.getHenkiloOid());
                assertEquals(erillishakuRivi.isJulkaistaankoTiedot(), v.getJulkaistavissa());
                assertEquals(
                    erillishakuRivi.getEhdollisestiHyvaksyttavissa(),
                    v.getEhdollisestiHyvaksyttavissa());
                assertEquals(MockData.kohdeOid, v.getHakukohdeOid());
              });
    }
  }

  private static ErillishakuRivi createRow(
      String henkilotunnus, String personOid, String hakemusOid) {
    return new ErillishakuRiviBuilder()
        .hakemusOid(hakemusOid)
        .sukunimi("Toppurainen")
        .etunimi("Joonas")
        .henkilotunnus(henkilotunnus)
        .sahkoposti("tuomas.toppurainen@example.com")
        .syntymaAika("10.12.1975")
        .sukupuoli(Sukupuoli.MIES)
        .personOid(personOid)
        .aidinkieli("FI")
        .hakemuksenTila("HYVAKSYTTY")
        .ehdollisestiHyvaksyttavissa(false)
        .hyvaksymiskirjeLahetetty(new Date())
        .vastaanottoTila("KESKEN")
        .ilmoittautumisTila("EI_TEHTY")
        .julkaistaankoTiedot(false)
        .poistetaankoRivi(false)
        .asiointikieli("FI")
        .puhelinnumero("045-6709709")
        .osoite("Kaisaniemenkatu 2 B")
        .postinumero("00100")
        .postitoimipaikka("Helsinki")
        .asuinmaa("FIN")
        .kansalaisuus("FIN")
        .kotikunta("Helsinki")
        .toisenAsteenSuoritus(true)
        .toisenAsteenSuoritusmaa("FIN")
        .maksuvelvollisuus(Maksuvelvollisuus.NOT_CHECKED)
        .build();
  }

  @Nested
  public final class TuntemattomallaAidinkielella extends ErillisHakuTuontiTestCase {
    @Test
    public void poistetaaanKielikoodistaDesimaalit() {
      importData(erillisHakuTuntemattomallaKielella());
      applicationAsyncResource.results.get(0);
      assertEquals(1, applicationAsyncResource.results.size());
    }
  }

  @Nested
  public final class HakijaOidilla extends ErillisHakuTuontiTestCase {
    @Test
    public void tuodaan() {
      importData(erillisHakuHenkiloOidilla());
    }
  }

  @Nested
  public final class Errors extends ErillisHakuTuontiTestCase {

    @Test
    public void tilojenTuontiEpaonnistuu() {
      final ValintaTulosServiceAsyncResource failingValintaTuloseServiceAsyncResource =
          mock(ValintaTulosServiceAsyncResource.class);
      final TarjontaAsyncResource tarjontaAsyncResource = mockTarjonta();
      when(failingValintaTuloseServiceAsyncResource.postErillishaunValinnantulokset(
              any(), anyString(), anyList()))
          .thenAnswer(
              i -> {
                String valintatapajonoOid = i.getArgument(1);
                return Observable.just(
                    ((List<Valinnantulos>) i.getArgument(2))
                        .stream()
                            .map(
                                v ->
                                    new ValintatulosUpdateStatus(
                                        500,
                                        "Something wrong",
                                        valintatapajonoOid,
                                        v.getHakemusOid()))
                            .collect(Collectors.toList()));
              });
      when(failingValintaTuloseServiceAsyncResource.fetchLukuvuosimaksut(anyString(), any()))
          .thenReturn(just(emptyList()));

      final ErillishaunTuontiService tuontiService =
          new ErillishaunTuontiService(
              applicationAsyncResource,
              henkiloAsyncResource,
              failingValintaTuloseServiceAsyncResource,
              koodistoCachedAsyncResource,
              tarjontaAsyncResource,
              Schedulers.trampoline());
      tuontiService.tuoExcelistä(
          new AuditSession(PERSON_2_OID, emptyList(), "", ""),
          prosessi,
          erillisHaku,
          kkHakuToisenAsteenValintatuloksella());
      Mockito.verify(prosessi).keskeyta(ArgumentMatchers.<Collection<Poikkeus>>any());
    }

    @Test
    public void hakemustenLuontiEpaonnistuu() {
      final AtaruAsyncResource failingResource = mock(AtaruAsyncResource.class);
      when(failingResource.putApplicationPrototypes(Mockito.any()))
          .thenReturn(Observable.error(new RuntimeException("simulated HTTP fail")));
      final TarjontaAsyncResource tarjontaAsyncResource = mockTarjonta();
      final ErillishaunTuontiService tuontiService =
          new ErillishaunTuontiService(
              failingResource,
              henkiloAsyncResource,
              valintaTulosServiceAsyncResource,
              koodistoCachedAsyncResource,
              tarjontaAsyncResource,
              Schedulers.trampoline());
      assertNull(henkiloAsyncResource.henkiloPrototyypit);
      KirjeProsessi prosessi = new ErillishakuProsessiDTO(1);

      tuontiService.tuoExcelistä(
          new AuditSession(PERSON_2_OID, emptyList(), "", ""),
          prosessi,
          erillisHaku,
          kkHakuToisenAsteenValintatuloksella());
      assertEquals(0, erillishaunValinnantulokset.size());
      assertTrue(prosessi.isKeskeytetty());
    }
  }
}

class ErillisHakuTuontiTestCase {

  public static TarjontaAsyncResource mockTarjonta() {
    final TarjontaAsyncResource tarjontaAsyncResource = mock(TarjontaAsyncResource.class);
    when(tarjontaAsyncResource.haeHaku(Mockito.any()))
        .thenReturn(CompletableFuture.completedFuture(getDummyHaku()));
    return tarjontaAsyncResource;
  }

  final MockOppijanumerorekisteriAsyncResource henkiloAsyncResource =
      new MockOppijanumerorekisteriAsyncResource();
  final MockAtaruAsyncResource applicationAsyncResource = new MockAtaruAsyncResource();
  final KirjeProsessi prosessi = mock(KirjeProsessi.class);
  final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource =
      new MockValintaTulosServiceAsyncResource();
  final Map<String, List<Valinnantulos>> erillishaunValinnantulokset =
      ((MockValintaTulosServiceAsyncResource) valintaTulosServiceAsyncResource)
          .erillishaunValinnantulokset;
  final KoodistoCachedAsyncResource koodistoCachedAsyncResource =
      mock(KoodistoCachedAsyncResource.class);
  final ErillishakuDTO erillisHaku =
      new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "haku1", "kohde1", "tarjoaja1", "jono1");

  @BeforeEach
  public void before() {
    mockKoodisto();
  }

  private void mockKoodisto() {
    Map<String, Koodi> kieliKoodit =
        ImmutableMap.of(
            "FI", new Koodi(),
            "99", new Koodi(),
            "SV", new Koodi(),
            "NO", new Koodi());

    when(koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.KIELI))
        .thenReturn(kieliKoodit);

    Map<String, Koodi> maaKoodit = ImmutableMap.of("FIN", new Koodi());
    when(koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1))
        .thenReturn(maaKoodit);

    Koodi kuntaKoodi = new Koodi();
    kuntaKoodi.setMetadata(
        Arrays.asList(createMetadata("Helsinki", "FI"), createMetadata("Helsingfors", "SV")));
    kuntaKoodi.setKoodiArvo("091");

    Map<String, Koodi> kuntaKoodit = ImmutableMap.of("091", kuntaKoodi);
    when(koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.KUNTA))
        .thenReturn(kuntaKoodit);

    Koodi postinumeroKoodi = new Koodi();
    postinumeroKoodi.setMetadata(
        Arrays.asList(createMetadata("HELSINKI", "FI"), createMetadata("HELSINGFORS", "SV")));
    postinumeroKoodi.setKoodiArvo("00100");

    Map<String, Koodi> postinumeroKoodit = ImmutableMap.of("00100", postinumeroKoodi);
    when(koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI))
        .thenReturn(postinumeroKoodit);

    Koodi ehdollisenHyvaksynnanKoodi1 = new Koodi();
    ehdollisenHyvaksynnanKoodi1.setMetadata(
        Arrays.asList(
            createMetadata(
                "Ehdollinen: lopullinen tutkintotodistus toimitettava määräaikaan mennessä", "FI"),
            createMetadata("Villkor: lämna in ditt slutliga examensbetyg inom utsatt tid", "SV"),
            createMetadata(
                "Condition: Submit your final qualification certificate by the deadline", "EN")));
    ehdollisenHyvaksynnanKoodi1.setKoodiArvo("ltt");

    Koodi ehdollisenHyvaksynnanKoodi2 = new Koodi();
    ehdollisenHyvaksynnanKoodi2.setMetadata(
        Arrays.asList(
            createMetadata("Muu", "FI"),
            createMetadata("Annan", "SV"),
            createMetadata("Other", "EN")));
    ehdollisenHyvaksynnanKoodi2.setKoodiArvo("muu");

    Koodi ehdollisenHyvaksynnanKoodi3 = new Koodi();
    ehdollisenHyvaksynnanKoodi3.setMetadata(
        Arrays.asList(
            createMetadata(
                "Ehdollinen: lukuvuosimaksu maksettava määräaikaan mennessä, ennen kuin voit ilmoittautua",
                "FI"),
            createMetadata(
                "Villkor: betala läsårsavgiften inom utsatt tid för att du ska kunna anmäla dig",
                "SV"),
            createMetadata(
                "Condition: You have to pay the tuition fee by the deadline before you can enroll as a student",
                "EN")));
    ehdollisenHyvaksynnanKoodi3.setKoodiArvo("lvm");

    Koodi ehdollisenHyvaksynnanKoodi4 = new Koodi();
    ehdollisenHyvaksynnanKoodi4.setMetadata(
        Arrays.asList(
            createMetadata(
                "Ehdollinen: tutkintotodistuskopio hakuperusteena olleesta tutkinnosta toimitettava määräaikaan mennessä",
                "FI"),
            createMetadata(
                "Villkor: lämna in kopia av examensbetyget för den examen som du använt som ansökningsgrund inom utsatt",
                "SV"),
            createMetadata(
                "Condition: Submit a copy of the qualification certificate of the qualification you used to prove your eligibility by the deadline",
                "EN")));
    ehdollisenHyvaksynnanKoodi4.setKoodiArvo("ttk");

    Map<String, Koodi> ehdollisenHyvaksynnanKoodit =
        ImmutableMap.of(
            "ltt",
            ehdollisenHyvaksynnanKoodi1,
            "muu",
            ehdollisenHyvaksynnanKoodi2,
            "lvm",
            ehdollisenHyvaksynnanKoodi3,
            "ttk",
            ehdollisenHyvaksynnanKoodi4);

    when(koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.HYVAKSYNNAN_EHDOT))
        .thenReturn(ehdollisenHyvaksynnanKoodit);
  }

  private Metadata createMetadata(String nimi, String kieli) {
    Metadata metadata = new Metadata();
    metadata.setKieli(kieli);
    metadata.setNimi(nimi);
    return metadata;
  }

  protected void importData(InputStream data) {
    final TarjontaAsyncResource tarjontaAsyncResource = mockTarjonta();
    final ErillishaunTuontiService tuontiService =
        new ErillishaunTuontiService(
            applicationAsyncResource,
            henkiloAsyncResource,
            valintaTulosServiceAsyncResource,
            koodistoCachedAsyncResource,
            tarjontaAsyncResource,
            Schedulers.trampoline());
    tuontiService.tuoExcelistä(
        new AuditSession(PERSON_1_OID, new ArrayList<>(), "", ""), prosessi, erillisHaku, data);
    Mockito.verify(prosessi).valmistui("ok");
  }

  protected void importRows(
      List<ErillishakuRivi> rivit,
      MockOppijanumerorekisteriAsyncResource mockHenkiloAsyncResource) {
    final TarjontaAsyncResource tarjontaAsyncResource = mockTarjonta();
    final ErillishaunTuontiService tuontiService =
        new ErillishaunTuontiService(
            applicationAsyncResource,
            mockHenkiloAsyncResource,
            valintaTulosServiceAsyncResource,
            koodistoCachedAsyncResource,
            tarjontaAsyncResource,
            Schedulers.trampoline());
    tuontiService.tuoJson(
        new AuditSession(PERSON_1_OID, new ArrayList<>(), "", ""),
        prosessi,
        erillisHaku,
        rivit,
        false);
    Mockito.verify(prosessi).valmistui("ok");
  }
}
