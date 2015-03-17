package fi.vm.sade.valinta.kooste.erillishaku.excel;

import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuHenkiloOidilla;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuHetullaJaSyntymaAjalla;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuSyntymaAjalla;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;

import com.codepoetics.protonpack.StreamUtils;
import com.google.common.util.concurrent.Futures;
import com.google.gson.GsonBuilder;
import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuProsessiDTO;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.google.gson.Gson;

import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiService;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockData;
import fi.vm.sade.valinta.kooste.mocks.MockHenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockTilaAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import rx.schedulers.Schedulers;

@RunWith(Enclosed.class)
public class ErillishaunTuontiServiceTest {

    public final static class Autotaytto extends ErillisHakuTuontiTestCase {
        @Test
        public void tuodaanHylattyjaJaPeruutettujaJaVarallaOleviaPuutteellisinTiedoinAutosyotonTestaamiseksi() {
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, applicationAsyncResource, henkiloAsyncResource, Schedulers.immediate());
            tuontiService.tuoExcelistä(prosessi, erillisHaku, puutteellisiaTietojaAutotayttoaVarten());
            Mockito.verify(prosessi).valmistui("ok");

            // tarkistetaan tilatulokset
            assertEquals(1, tilaAsyncResource.results.size());
            assertEquals(5, tilaAsyncResource.results.stream()
                    .flatMap(r -> r.erillishaunHakijat.stream()).count());
            tilaAsyncResource.results.stream()
                    .flatMap(r -> r.erillishaunHakijat.stream())
                    .forEach(r -> {
                        Assert.assertTrue(ValintatuloksenTila.KESKEN.equals(r.getValintatuloksenTila()));
                        Assert.assertTrue(IlmoittautumisTila.EI_TEHTY.equals(r.getIlmoittautumisTila()));
                    });
        }
    }
    public final static class KorkeaKouluHaku extends ErillisHakuTuontiTestCase {
        @Test
        public void tuodaanToisenAsteenValintatilojaKorkeaKoulunHakuun() {
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, applicationAsyncResource, henkiloAsyncResource, Schedulers.immediate());
            tuontiService.tuoExcelistä(prosessi, erillisHaku, kkHakuToisenAsteenValintatuloksella());
            Mockito.verify(prosessi).valmistui("ok");

            // tarkistetaan henkilöt
            assertEquals(1, henkiloAsyncResource.henkiloPrototyypit.size());

            // tarkistetaan hakemukset
            assertEquals(1, applicationAsyncResource.results.size());

            // tarkistetaan tilatulokset
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
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, applicationAsyncResource, henkiloAsyncResource, Schedulers.immediate());
            tuontiService.tuoExcelistä(prosessi, erillisHaku, erillisHakuHetullaJaSyntymaAjalla());
            Mockito.verify(prosessi).valmistui("ok");

            // tarkistetaan henkilöt
            assertEquals(1, henkiloAsyncResource.henkiloPrototyypit.size());
            final HenkiloCreateDTO henkilo = henkiloAsyncResource.henkiloPrototyypit.get(0);
            assertEquals("Tuomas", henkilo.etunimet);
            assertEquals("Tuomas", henkilo.kutsumanimi);
            assertEquals("Hakkarainen", henkilo.sukunimi);
            assertEquals(MockData.hetu, henkilo.hetu);
            assertNotNull(henkilo.syntymaaika);
            assertEquals(HenkiloTyyppi.OPPIJA, henkilo.henkiloTyyppi);

            // tarkistetaan hakemukset
            assertEquals(1, applicationAsyncResource.results.size());
            applicationAsyncResource.results.get(0);
            final MockApplicationAsyncResource.Result appResult = applicationAsyncResource.results.get(0);
            assertEquals("haku1", appResult.hakuOid);
            assertEquals("kohde1", appResult.hakukohdeOid);
            assertEquals("tarjoaja1", appResult.tarjoajaOid);
            assertEquals(1, appResult.hakemusPrototyypit.size());
            final HakemusPrototyyppi hakemusProto = appResult.hakemusPrototyypit.iterator().next();
            assertEquals("hakija1", hakemusProto.hakijaOid);
            assertEquals(MockData.hetu, hakemusProto.henkilotunnus);
            assertEquals("Tuomas", hakemusProto.etunimi);
            assertEquals("Hakkarainen", hakemusProto.sukunimi);
            assertEquals("01.01.1901", hakemusProto.syntymaAika);

            // tarkistetaan tilatulokset
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
            System.out.println(new Gson().toJson(tilaAsyncResource.results));
        }
    }

    public final static class SyntymaAjalla extends ErillisHakuTuontiTestCase {
        @Test
        public void tuodaan() {
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, applicationAsyncResource, henkiloAsyncResource, Schedulers.immediate());
            tuontiService.tuoExcelistä(prosessi, erillisHaku, erillisHakuSyntymaAjalla());
            Mockito.verify(prosessi).valmistui("ok");

            assertEquals(1, applicationAsyncResource.results.size());
            applicationAsyncResource.results.get(0);
            final MockApplicationAsyncResource.Result appResult = applicationAsyncResource.results.get(0);
            assertEquals("haku1", appResult.hakuOid);
            assertEquals("kohde1", appResult.hakukohdeOid);
            assertEquals("tarjoaja1", appResult.tarjoajaOid);
            assertEquals(1, appResult.hakemusPrototyypit.size());
            final HakemusPrototyyppi hakemusProto = appResult.hakemusPrototyypit.iterator().next();
            assertEquals("hakija1", hakemusProto.hakijaOid);
            assertEquals("Tuomas", hakemusProto.etunimi);
            assertEquals("Hakkarainen", hakemusProto.sukunimi);
            assertEquals("01.01.1901", hakemusProto.syntymaAika);
        }
    }


    public final static class HakijaOidilla extends ErillisHakuTuontiTestCase {
        @Test
        public void tuodaan() {
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, applicationAsyncResource, henkiloAsyncResource, Schedulers.immediate());
            tuontiService.tuoExcelistä(prosessi, erillisHaku, erillisHakuHenkiloOidilla());
            Mockito.verify(prosessi).valmistui("ok");
        }
    }

    public final static class Errors extends ErillisHakuTuontiTestCase {
        @Test
        public void henkilonLuontiEpaonnistuu() {
            final HenkiloAsyncResource failingHenkiloResource = Mockito.mock(HenkiloAsyncResource.class);
            Mockito.when(failingHenkiloResource.haeTaiLuoHenkilot(Mockito.any())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("simulated HTTP fail")));
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, applicationAsyncResource, failingHenkiloResource, Schedulers.immediate());
            assertEquals(0, tilaAsyncResource.results.size());
            assertEquals(0, applicationAsyncResource.results.size());
            tuontiService.tuoExcelistä(prosessi, erillisHaku, erillisHakuHetullaJaSyntymaAjalla());
            Mockito.verify(prosessi).keskeyta();
        }

        @Test
        public void tilojenTuontiEpaonnistuu() {
            final TilaAsyncResource failingResource = Mockito.mock(TilaAsyncResource.class);
            Mockito.when(failingResource.tuoErillishaunTilat(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(new RuntimeException("simulated HTTP fail"));
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(failingResource, applicationAsyncResource, henkiloAsyncResource, Schedulers.immediate());
            assertEquals(0, applicationAsyncResource.results.size());
            assertNull(henkiloAsyncResource.henkiloPrototyypit);
            tuontiService.tuoExcelistä(prosessi, erillisHaku, erillisHakuHetullaJaSyntymaAjalla());
            Mockito.verify(prosessi).keskeyta();
        }

        @Test
        public void hakemustenLuontiEpaonnistuu() {
            final ApplicationAsyncResource failingResource = Mockito.mock(ApplicationAsyncResource.class);
            Mockito.when(failingResource.putApplicationPrototypes(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("simulated HTTP fail")));
            Mockito.when(failingResource.getApplicationsByOid(Mockito.anyString(), Mockito.anyString())).thenReturn(applicationAsyncResource.getApplicationsByOid("haku1", "kohde1"));
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, failingResource, henkiloAsyncResource, Schedulers.immediate());
            assertEquals(0, tilaAsyncResource.results.size());
            assertNull(henkiloAsyncResource.henkiloPrototyypit);
            KirjeProsessi prosessi = new ErillishakuProsessiDTO(1);
            tuontiService.tuoExcelistä(prosessi, erillisHaku, erillisHakuHetullaJaSyntymaAjalla());
            assertTrue(prosessi.isKeskeytetty());
        }
    }


    // TODO: testaa 1) pelkkä hetu 2) pelkkä hakija-oid 3) pelkkä syntymäpäivä 4) hetu+synt.päivä
    // ehkä ilman varsinaista excel-parsintaa. ehkä json-interfacesta? ks. jussin testi
}

class ErillisHakuTuontiTestCase {
    final MockHenkiloAsyncResource henkiloAsyncResource = new MockHenkiloAsyncResource();
    final MockApplicationAsyncResource applicationAsyncResource = new MockApplicationAsyncResource();
    final MockTilaAsyncResource tilaAsyncResource = new MockTilaAsyncResource();
    final KirjeProsessi prosessi = Mockito.mock(KirjeProsessi.class);
    final ErillishakuDTO erillisHaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "haku1", "kohde1", "tarjoaja1", "jono1", "varsinainen jono");
}
