package fi.vm.sade.valinta.kooste.erillishaku.excel;

import static fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi.SYNTYMAAIKAFORMAT;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuHenkiloOidilla;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuSyntymaAjalla;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuTuntemattomallaKielella;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.kkHakuToisenAsteenValintatuloksella;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.puutteellisiaTietojaAutotayttoaVarten;
import static java.util.Collections.*;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static rx.Observable.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuProsessiDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiService;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Metadata;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.ValintatulosUpdateStatus;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Valinnantulos;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockData;
import fi.vm.sade.valinta.kooste.mocks.MockHenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoRecordDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoResultDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoResultDTO.Result;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@RunWith(Enclosed.class)
public class ErillishaunTuontiServiceTest {

    public final static class Autotaytto extends ErillisHakuTuontiTestCase {
        @Test
        public void tuodaanHylattyjaJaPeruutettujaJaVarallaOleviaPuutteellisinTiedoinAutosyotonTestaamiseksi() {
            importData(puutteellisiaTietojaAutotayttoaVarten());

            assertEquals(1, erillishaunValinnantulokset.size());
            assertEquals(5, erillishaunValinnantulokset.get("jono1").size()); //KESKEN-tilainen puuttuu
            erillishaunValinnantulokset.get("jono1").stream().forEach(v -> {
                assertEquals(IlmoittautumisTila.EI_TEHTY, v.getIlmoittautumistila());
                assertEquals(ValintatuloksenTila.KESKEN, v.getVastaanottotila());
            });
        }
    }
    public final static class KorkeaKouluHaku extends ErillisHakuTuontiTestCase {
        @Test
        public void tuodaanToisenAsteenValintatilojaKorkeaKoulunHakuun() {
            importData(kkHakuToisenAsteenValintatuloksella());

            assertEquals(1, henkiloAsyncResource.henkiloPrototyypit.size());
            assertEquals(1, applicationAsyncResource.results.size());
            assertEquals(1, erillishaunValinnantulokset.size());
            assertEquals(1, erillishaunValinnantulokset.get("jono1").size());
            erillishaunValinnantulokset.get("jono1").stream().forEach(v -> {
                assertEquals(IlmoittautumisTila.EI_TEHTY, v.getIlmoittautumistila());
                assertEquals(ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI, v.getVastaanottotila());
            });
        }

    }

    public final static class HetullaJaSyntymaAjalla extends ErillisHakuTuontiTestCase {
        @Test
        public void tuontiSuoritetaan() throws IOException, InterruptedException {
            importData(kkHakuToisenAsteenValintatuloksella());

            assertEquals(1, henkiloAsyncResource.henkiloPrototyypit.size());
            final HenkiloCreateDTO henkilo = henkiloAsyncResource.henkiloPrototyypit.get(0);
            assertEquals("Tuomas", henkilo.etunimet);
            assertEquals("Tuomas", henkilo.kutsumanimi);
            assertEquals("Hakkarainen", henkilo.sukunimi);
            assertEquals(MockData.hetu, henkilo.hetu);
            assertNotNull(henkilo.syntymaaika);
            assertEquals(HenkiloTyyppi.OPPIJA, henkilo.henkiloTyyppi);

            assertEquals(1, applicationAsyncResource.results.size());
            applicationAsyncResource.results.get(0);
            final MockApplicationAsyncResource.Result appResult = applicationAsyncResource.results.get(0);
            assertEquals("haku1", appResult.hakuOid);
            assertEquals("kohde1", appResult.hakukohdeOid);
            assertEquals("tarjoaja1", appResult.tarjoajaOid);
            assertEquals(1, appResult.hakemusPrototyypit.size());
            final HakemusPrototyyppi hakemusProto = appResult.hakemusPrototyypit.iterator().next();
            assertEquals("hakija1", hakemusProto.getHakijaOid());
            assertEquals(MockData.hetu, hakemusProto.getHenkilotunnus());
            assertEquals("Tuomas", hakemusProto.getEtunimi());
            assertEquals("Hakkarainen", hakemusProto.getSukunimi());
            assertEquals("01.01.1901", hakemusProto.getSyntymaAika());
            assertEquals(Maksuvelvollisuus.REQUIRED, hakemusProto.getMaksuvelvollisuus());

            assertEquals(1, erillishaunValinnantulokset.size());
            assertEquals(1, erillishaunValinnantulokset.get("jono1").size());
            erillishaunValinnantulokset.get("jono1").stream().forEach(v -> {
                assertEquals(IlmoittautumisTila.EI_TEHTY, v.getIlmoittautumistila());
                assertEquals(ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI, v.getVastaanottotila());
                assertEquals(MockData.valintatapajonoOid, v.getValintatapajonoOid());
                assertEquals("hakemus1", v.getHakemusOid());
                assertEquals("hakija1", v.getHenkiloOid());
                assertEquals(true, v.getJulkaistavissa());
                assertEquals(true, v.getEhdollisestiHyvaksyttavissa());
                assertEquals(MockData.kohdeOid, v.getHakukohdeOid());
            });
        }
    }

    public final static class SyntymaAjalla extends ErillisHakuTuontiTestCase {
        @Test
        public void tuodaan() {
            importData(erillisHakuSyntymaAjalla());

            assertEquals(1, applicationAsyncResource.results.size());
            applicationAsyncResource.results.get(0);
            final MockApplicationAsyncResource.Result appResult = applicationAsyncResource.results.get(0);
            assertEquals("haku1", appResult.hakuOid);
            assertEquals("kohde1", appResult.hakukohdeOid);
            assertEquals("tarjoaja1", appResult.tarjoajaOid);
            assertEquals(1, appResult.hakemusPrototyypit.size());
            final HakemusPrototyyppi hakemusProto = appResult.hakemusPrototyypit.iterator().next();
            assertEquals("hakija1", hakemusProto.getHakijaOid());
            assertEquals("Tuomas", hakemusProto.getEtunimi());
            assertEquals("Hakkarainen", hakemusProto.getSukunimi());
            assertEquals("01.01.1901", hakemusProto.getSyntymaAika());
            assertEquals(Maksuvelvollisuus.NOT_REQUIRED, hakemusProto.getMaksuvelvollisuus());
        }
    }

    public final static class HetullaJaSyntymaAjallaJaOidillaJokaOnEriKuinHenkilopalvelustaPalaava extends ErillisHakuTuontiTestCase {
        @Test
        public void tuontiSuoritetaan() throws IOException, InterruptedException, ParseException {
            String hakemusOidOnHakemusAndSijoittelu = "hakija1";
            String personOidHenkiloPalvelusta = "eri.henkiloOid.henkiloPalvelusta";
            ErillishakuRivi erillishakuRivi = createRow("101275-937P", hakemusOidOnHakemusAndSijoittelu, "hakemus1");
            List<HenkiloCreateDTO> henkiloPrototyypit = new ArrayList<>();
            MockHenkiloAsyncResource mockHenkiloAsyncResource = new MockHenkiloAsyncResource(dtos -> {
                Henkilo henkiloJollaOnEriOid = MockHenkiloAsyncResource.toHenkilo(dtos.get(0));
                henkiloJollaOnEriOid.setOidHenkilo(personOidHenkiloPalvelusta);
                henkiloPrototyypit.addAll(dtos);
                return Futures.immediateFuture(singletonList(henkiloJollaOnEriOid));
            });
            importRows(singletonList(erillishakuRivi), mockHenkiloAsyncResource);

            assertEquals(1, henkiloPrototyypit.size());
            final HenkiloCreateDTO henkilo = henkiloPrototyypit.get(0);
            assertEquals(erillishakuRivi.getEtunimi(), henkilo.etunimet);
            assertEquals(erillishakuRivi.getEtunimi(), henkilo.kutsumanimi);
            assertEquals(erillishakuRivi.getSukunimi(), henkilo.sukunimi);
            assertEquals(erillishakuRivi.getHenkilotunnus(), henkilo.hetu);
            assertEquals(SYNTYMAAIKAFORMAT.parseDateTime(erillishakuRivi.getSyntymaAika()).toDate(), henkilo.syntymaaika);
            assertEquals(HenkiloTyyppi.OPPIJA, henkilo.henkiloTyyppi);

            assertEquals(1, erillishaunValinnantulokset.size());
            assertEquals(1, erillishaunValinnantulokset.get("jono1").size());
            erillishaunValinnantulokset.get("jono1").stream().forEach(v -> {
                assertEquals(IlmoittautumisTila.EI_TEHTY, v.getIlmoittautumistila());
                assertEquals(ValintatuloksenTila.KESKEN, v.getVastaanottotila());
                assertEquals(MockData.valintatapajonoOid, v.getValintatapajonoOid());
                assertEquals("hakemus1", v.getHakemusOid());
                assertEquals(personOidHenkiloPalvelusta, v.getHenkiloOid());
                assertEquals(erillishakuRivi.isJulkaistaankoTiedot(), v.getJulkaistavissa());
                assertEquals(erillishakuRivi.getEhdollisestiHyvaksyttavissa(), v.getEhdollisestiHyvaksyttavissa());
                assertEquals(MockData.kohdeOid, v.getHakukohdeOid());
            });
        }
    }

    private static ErillishakuRivi createRow(String henkilotunnus, String personOid, String hakemusOid) {
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

    public final static class TuntemattomallaAidinkielella extends ErillisHakuTuontiTestCase {
        @Test
        public void poistetaaanKielikoodistaDesimaalit() {
            importData(erillisHakuTuntemattomallaKielella());
            applicationAsyncResource.results.get(0);
            assertEquals(1, applicationAsyncResource.results.size());
        }
    }

    public final static class HakijaOidilla extends ErillisHakuTuontiTestCase {
        @Test
        public void tuodaan() {
            importData(erillisHakuHenkiloOidilla());
        }
    }

    public final static class Errors extends ErillisHakuTuontiTestCase {

        @Test
        public void henkilonLuontiEpaonnistuu() {
            final HenkiloAsyncResource failingHenkiloResource = mock(HenkiloAsyncResource.class);
            when(failingHenkiloResource.haeTaiLuoHenkilot(Mockito.any())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("simulated HTTP fail")));
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(
                    applicationAsyncResource,
                    failingHenkiloResource,
                    valintaTulosServiceAsyncResource,
                    koodistoCachedAsyncResource,
                    Schedulers.immediate()
            );
            assertEquals(0, applicationAsyncResource.results.size());
            tuontiService.tuoExcelistä(null, prosessi, erillisHaku, kkHakuToisenAsteenValintatuloksella());
            Mockito.verify(prosessi).keskeyta();
        }

        @Test
        public void tilojenTuontiEpaonnistuu() {
            final ValintaTulosServiceAsyncResource failingValintaTuloseServiceAsyncResource = mock(ValintaTulosServiceAsyncResource.class);
            when(failingValintaTuloseServiceAsyncResource.postErillishaunValinnantulokset(anyObject(), anyString(), anyList())).thenAnswer(i -> {
                String valintatapajonoOid = i.getArgumentAt(1, String.class);
                return Observable.just(((List<Valinnantulos>)i.getArgumentAt(2, List.class)).stream().map(v ->
                        new ValintatulosUpdateStatus(500, "Something wrong", valintatapajonoOid, v.getHakemusOid())).collect(Collectors.toList()));
            });
            when(failingValintaTuloseServiceAsyncResource.tallenna(anyListOf(VastaanottoRecordDTO.class))).then(invocation -> Observable
                    .just(((List<VastaanottoRecordDTO>) invocation.getArguments()[0]).stream().map(v -> {
                        VastaanottoResultDTO dto = new VastaanottoResultDTO();
                        dto.setHakemusOid(v.getHakemusOid());
                        dto.setHakukohdeOid(v.getHakukohdeOid());
                        dto.setHenkiloOid(v.getHenkiloOid());
                        Result result = new Result();
                        result.setStatus(OK.getStatusCode());
                        dto.setResult(result);
                        return dto;
                    }).collect(Collectors.toList()))
            );
            when(failingValintaTuloseServiceAsyncResource.fetchLukuvuosimaksut(anyString(), any())).thenReturn(just(emptyList()));

            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(
                    applicationAsyncResource,
                    henkiloAsyncResource,
                    failingValintaTuloseServiceAsyncResource,
                    koodistoCachedAsyncResource,
                    Schedulers.immediate()
            );
            tuontiService.tuoExcelistä(new AuditSession("bob", emptyList(), "", ""),prosessi, erillisHaku, kkHakuToisenAsteenValintatuloksella());
            Mockito.verify(prosessi).keskeyta(Matchers.<Collection<Poikkeus>>any());
        }

        @Test
        public void hakemustenLuontiEpaonnistuu() {
            final ApplicationAsyncResource failingResource = mock(ApplicationAsyncResource.class);
            when(failingResource.putApplicationPrototypes(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("simulated HTTP fail")));
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(
                    failingResource,
                    henkiloAsyncResource,
                    valintaTulosServiceAsyncResource,
                    koodistoCachedAsyncResource,
                    Schedulers.immediate()
            );
            assertNull(henkiloAsyncResource.henkiloPrototyypit);
            KirjeProsessi prosessi = new ErillishakuProsessiDTO(1);

            tuontiService.tuoExcelistä(null,prosessi, erillisHaku, kkHakuToisenAsteenValintatuloksella());
            assertEquals(0, erillishaunValinnantulokset.size());
            assertTrue(prosessi.isKeskeytetty());
        }
    }
}

class ErillisHakuTuontiTestCase {
    final MockHenkiloAsyncResource henkiloAsyncResource = new MockHenkiloAsyncResource();
    final MockApplicationAsyncResource applicationAsyncResource = new MockApplicationAsyncResource();
    final KirjeProsessi prosessi = mock(KirjeProsessi.class);
    final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource = new MockValintaTulosServiceAsyncResource();
    final Map<String,List<Valinnantulos>> erillishaunValinnantulokset = ((MockValintaTulosServiceAsyncResource)valintaTulosServiceAsyncResource).erillishaunValinnantulokset;
    final KoodistoCachedAsyncResource koodistoCachedAsyncResource = mock(KoodistoCachedAsyncResource.class);
    final ErillishakuDTO erillisHaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "haku1", "kohde1", "tarjoaja1", "jono1");

    @Before
    public void before() {
        mockKoodisto();
    }

    private void mockKoodisto() {
        Map<String, Koodi> kieliKoodit = ImmutableMap.of(
                "FI", new Koodi(),
                "99", new Koodi(),
                "SV", new Koodi(),
                "NO", new Koodi());

        when(koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.KIELI)).thenReturn(kieliKoodit);

        Map<String, Koodi> maaKoodit = ImmutableMap.of(
                "FIN", new Koodi());
        when(koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1)).thenReturn(maaKoodit);

        Koodi kuntaKoodi = new Koodi();
        kuntaKoodi.setMetadata(Arrays.asList(createMetadata("Helsinki", "FI"), createMetadata("Helsingfors", "SV")));
        kuntaKoodi.setKoodiArvo("091");

        Map<String, Koodi> kuntaKoodit = ImmutableMap.of(
                "091", kuntaKoodi);
        when(koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.KUNTA)).thenReturn(kuntaKoodit);

        Koodi postinumeroKoodi = new Koodi();
        postinumeroKoodi.setMetadata(Arrays.asList(createMetadata("HELSINKI", "FI"), createMetadata("HELSINGFORS", "SV")));
        postinumeroKoodi.setKoodiArvo("00100");

        Map<String, Koodi> postinumeroKoodit = ImmutableMap.of(
                "00100", postinumeroKoodi);
        when(koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI)).thenReturn(postinumeroKoodit);
    }

    private Metadata createMetadata(String nimi, String kieli) {
        Metadata metadata = new Metadata();
        metadata.setKieli(kieli);
        metadata.setNimi(nimi);
        return metadata;
    }

    protected void importData(InputStream data) {
        final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(
                applicationAsyncResource,
                henkiloAsyncResource,
                valintaTulosServiceAsyncResource,
                koodistoCachedAsyncResource,
                Schedulers.immediate()
        );
        tuontiService.tuoExcelistä(new AuditSession("frank", new ArrayList<String>(), "", ""), prosessi, erillisHaku, data);
        Mockito.verify(prosessi).valmistui("ok");
    }

    protected void importRows(List<ErillishakuRivi> rivit, MockHenkiloAsyncResource mockHenkiloAsyncResource) {
        final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(
                applicationAsyncResource,
                mockHenkiloAsyncResource,
                valintaTulosServiceAsyncResource,
                koodistoCachedAsyncResource,
                Schedulers.immediate()
        );
        tuontiService.tuoJson(new AuditSession("frank", new ArrayList<String>(), "", ""), prosessi, erillisHaku, rivit, false);
        Mockito.verify(prosessi).valmistui("ok");
    }
}
