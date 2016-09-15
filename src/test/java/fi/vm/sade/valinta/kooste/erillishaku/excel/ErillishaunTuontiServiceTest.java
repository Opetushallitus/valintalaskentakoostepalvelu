package fi.vm.sade.valinta.kooste.erillishaku.excel;

import static fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi.SYNTYMAAIKAFORMAT;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuHenkiloOidilla;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuSyntymaAjalla;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuTuntemattomallaKielella;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.kkHakuToisenAsteenValintatuloksella;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.puutteellisiaTietojaAutotayttoaVarten;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.http.FailedHttpException;
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
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.HakukohteenValintatulosUpdateStatuses;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockData;
import fi.vm.sade.valinta.kooste.mocks.MockHenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockTilaAsyncResource;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoRecordDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoResultDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoResultDTO.Result;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
            assertEquals(1, tilaAsyncResource.results.size());
            assertEquals(5, tilaAsyncResource.results.stream()
                    .flatMap(r -> r.erillishaunHakijat.stream()).count());
            tilaAsyncResource.results.stream()
                    .flatMap(r -> r.erillishaunHakijat.stream())
                    .forEach(r -> {
                        assertEquals(ValintatuloksenTila.KESKEN, r.getValintatuloksenTila());
                        assertEquals(IlmoittautumisTila.EI_TEHTY, r.getIlmoittautumisTila());
                    });
        }
    }
    public final static class KorkeaKouluHaku extends ErillisHakuTuontiTestCase {
        @Test
        public void tuodaanToisenAsteenValintatilojaKorkeaKoulunHakuun() {
            importData(kkHakuToisenAsteenValintatuloksella());

            assertEquals(1, henkiloAsyncResource.henkiloPrototyypit.size());
            assertEquals(1, applicationAsyncResource.results.size());
            assertEquals(1, tilaAsyncResource.results.size());
            final MockTilaAsyncResource.Result tilaResult = tilaAsyncResource.results.get(0);
            assertEquals(MockData.hakuOid, tilaResult.hakuOid);
            assertEquals(1, tilaResult.erillishaunHakijat.size());
            final ErillishaunHakijaDTO hakija = tilaResult.erillishaunHakijat.iterator().next();
            assertEquals(MockData.valintatapajonoOid, hakija.valintatapajonoOid);
            assertEquals(ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI, hakija.getValintatuloksenTila());
            System.out.println(new Gson().toJson(tilaAsyncResource.results));
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

            assertEquals(1, tilaAsyncResource.results.size());
            final MockTilaAsyncResource.Result tilaResult = tilaAsyncResource.results.get(0);
            assertEquals(MockData.hakuOid, tilaResult.hakuOid);
            assertEquals(MockData.kohdeOid, tilaResult.hakukohdeOid);
            assertEquals("varsinainen jono", tilaResult.valintatapajononNimi);
            assertEquals(1, tilaResult.erillishaunHakijat.size());
            final ErillishaunHakijaDTO hakija = tilaResult.erillishaunHakijat.iterator().next();
            assertEquals("Tuomas", hakija.etunimi);
            assertEquals("Hakkarainen", hakija.sukunimi);
            assertEquals(MockData.valintatapajonoOid, hakija.valintatapajonoOid);
            assertEquals("hakemus1", hakija.hakemusOid);
            assertEquals("hakija1", hakija.hakijaOid);
            assertEquals(true, hakija.julkaistavissa);
            assertEquals(true, hakija.ehdollisestiHyvaksyttavissa);
            System.out.println(new Gson().toJson(tilaAsyncResource.results));
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
                return Futures.immediateFuture(Collections.singletonList(henkiloJollaOnEriOid));
            });
            importRows(Collections.singletonList(erillishakuRivi), mockHenkiloAsyncResource);

            assertEquals(1, henkiloPrototyypit.size());
            final HenkiloCreateDTO henkilo = henkiloPrototyypit.get(0);
            assertEquals(erillishakuRivi.getEtunimi(), henkilo.etunimet);
            assertEquals(erillishakuRivi.getEtunimi(), henkilo.kutsumanimi);
            assertEquals(erillishakuRivi.getSukunimi(), henkilo.sukunimi);
            assertEquals(erillishakuRivi.getHenkilotunnus(), henkilo.hetu);
            assertEquals(SYNTYMAAIKAFORMAT.parseDateTime(erillishakuRivi.getSyntymaAika()).toDate(), henkilo.syntymaaika);
            assertEquals(HenkiloTyyppi.OPPIJA, henkilo.henkiloTyyppi);

            assertEquals(1, applicationAsyncResource.results.size());
            applicationAsyncResource.results.get(0);
            final MockApplicationAsyncResource.Result appResult = applicationAsyncResource.results.get(0);
            assertEquals("haku1", appResult.hakuOid);
            assertEquals("kohde1", appResult.hakukohdeOid);
            assertEquals("tarjoaja1", appResult.tarjoajaOid);
            assertEquals(1, appResult.hakemusPrototyypit.size());
            final HakemusPrototyyppi hakemusProto = appResult.hakemusPrototyypit.iterator().next();
            assertEquals(personOidHenkiloPalvelusta, hakemusProto.getHakijaOid());
            assertEquals(erillishakuRivi.getHenkilotunnus(), hakemusProto.getHenkilotunnus());
            assertEquals(erillishakuRivi.getEtunimi(), hakemusProto.getEtunimi());
            assertEquals(erillishakuRivi.getSukunimi(), hakemusProto.getSukunimi());
            assertEquals(erillishakuRivi.getSyntymaAika(), hakemusProto.getSyntymaAika());

            assertEquals(1, tilaAsyncResource.results.size());
            final MockTilaAsyncResource.Result tilaResult = tilaAsyncResource.results.get(0);
            assertEquals(MockData.hakuOid, tilaResult.hakuOid);
            assertEquals(MockData.kohdeOid, tilaResult.hakukohdeOid);
            assertEquals("varsinainen jono", tilaResult.valintatapajononNimi);
            assertEquals(1, tilaResult.erillishaunHakijat.size());
            final ErillishaunHakijaDTO hakija = tilaResult.erillishaunHakijat.iterator().next();
            assertEquals(erillishakuRivi.getEtunimi(), hakija.etunimi);
            assertEquals(erillishakuRivi.getSukunimi(), hakija.sukunimi);
            assertEquals(MockData.valintatapajonoOid, hakija.valintatapajonoOid);
            assertEquals("hakemus1", hakija.hakemusOid);
            assertEquals(personOidHenkiloPalvelusta, hakija.hakijaOid);
            assertEquals(erillishakuRivi.isJulkaistaankoTiedot(), hakija.julkaistavissa);
            assertEquals(erillishakuRivi.isJulkaistaankoTiedot(), hakija.ehdollisestiHyvaksyttavissa);
        }
    }

    private static ErillishakuRivi createRow(String henkilotunnus, String personOid, String hakemusOid) {
        return new ErillishakuRivi(hakemusOid, "Toppurainen", "Joonas", henkilotunnus, "tuomas.toppurainen@example.com", "10.12.1975", "MIES", personOid,
            "FI", "HYVAKSYTTY", false, new Date(), "KESKEN", "EI_TEHTY", false, false, "FI", "045-6709709", "Kaisaniemenkatu 2 B", "00100", "Helsinki", "FIN", "FIN", "Helsinki", true, "FIN");
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
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, applicationAsyncResource, failingHenkiloResource, valintaTulosServiceAsyncResource, koodistoCachedAsyncResource, Schedulers.immediate());
            assertEquals(0, tilaAsyncResource.results.size());
            assertEquals(0, applicationAsyncResource.results.size());
            tuontiService.tuoExcelistä(null, prosessi, erillisHaku, kkHakuToisenAsteenValintatuloksella());
            Mockito.verify(prosessi).keskeyta();
        }

        @Test
        public void tilojenTuontiEpaonnistuu() {
            final TilaAsyncResource failingResource = mock(TilaAsyncResource.class);
            ResponseImpl response = (ResponseImpl)Response.ok(new HakukohteenValintatulosUpdateStatuses("viesti", new ArrayList<>()), MediaType.APPLICATION_JSON_TYPE).build();
            when(failingResource.tuoErillishaunTilat(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                    .thenReturn(Observable.error(new FailedHttpException(response)));
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(failingResource, applicationAsyncResource, henkiloAsyncResource, valintaTulosServiceAsyncResource, koodistoCachedAsyncResource, Schedulers.immediate());
            assertEquals(0, applicationAsyncResource.results.size());
            assertNull(henkiloAsyncResource.henkiloPrototyypit);
            tuontiService.tuoExcelistä("bob",prosessi, erillisHaku, kkHakuToisenAsteenValintatuloksella());
            Mockito.verify(prosessi).keskeyta((Collection<Poikkeus>)Matchers.any());
        }

        @Test
        public void hakemustenLuontiEpaonnistuu() {
            final ApplicationAsyncResource failingResource = mock(ApplicationAsyncResource.class);
            when(failingResource.putApplicationPrototypes(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("simulated HTTP fail")));
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, failingResource, henkiloAsyncResource, valintaTulosServiceAsyncResource, koodistoCachedAsyncResource, Schedulers.immediate());
            assertEquals(0, tilaAsyncResource.results.size());
            assertNull(henkiloAsyncResource.henkiloPrototyypit);
            KirjeProsessi prosessi = new ErillishakuProsessiDTO(1);

            tuontiService.tuoExcelistä(null,prosessi, erillisHaku, kkHakuToisenAsteenValintatuloksella());
            assertTrue(prosessi.isKeskeytetty());
        }
    }
}

class ErillisHakuTuontiTestCase {
    final MockHenkiloAsyncResource henkiloAsyncResource = new MockHenkiloAsyncResource();
    final MockApplicationAsyncResource applicationAsyncResource = new MockApplicationAsyncResource();
    final MockTilaAsyncResource tilaAsyncResource = new MockTilaAsyncResource();
    final KirjeProsessi prosessi = mock(KirjeProsessi.class);
    final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource = mock(ValintaTulosServiceAsyncResource.class);
    final KoodistoCachedAsyncResource koodistoCachedAsyncResource = mock(KoodistoCachedAsyncResource.class);
    final ErillishakuDTO erillisHaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "haku1", "kohde1", "tarjoaja1", "jono1", "varsinainen jono");

    @Before
    public void before() {
        when(valintaTulosServiceAsyncResource.tallenna(anyListOf(VastaanottoRecordDTO.class))).then(invocation -> Observable
                .just(((List<VastaanottoRecordDTO>) invocation.getArguments()[0]).stream().map(v -> {
                    VastaanottoResultDTO dto = new VastaanottoResultDTO();
                    dto.setHakemusOid(v.getHakemusOid());
                    dto.setHakukohdeOid(v.getHakukohdeOid());
                    dto.setHenkiloOid(v.getHenkiloOid());
                    Result result = new Result();
                    result.setStatus(OK.getStatusCode());
                    dto.setResult(result);
                    return dto;
                }).collect(Collectors.toList())));


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
        final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, applicationAsyncResource, henkiloAsyncResource, valintaTulosServiceAsyncResource, koodistoCachedAsyncResource, Schedulers.immediate());
        tuontiService.tuoExcelistä("frank", prosessi, erillisHaku, data);
        Mockito.verify(prosessi).valmistui("ok");
    }

    protected void importRows(List<ErillishakuRivi> rivit, MockHenkiloAsyncResource mockHenkiloAsyncResource) {
        final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, applicationAsyncResource, mockHenkiloAsyncResource, valintaTulosServiceAsyncResource, koodistoCachedAsyncResource, Schedulers.immediate());
        tuontiService.tuoJson("frank", prosessi, erillisHaku, rivit);
        Mockito.verify(prosessi).valmistui("ok");
    }
}
