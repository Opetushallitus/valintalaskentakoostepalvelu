package fi.vm.sade.valinta.kooste.erillishaku.excel;

import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuHenkiloOidilla;
import static fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData.erillisHakuHetullaJaSyntymaAjalla;
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

import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;

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
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.HakukohteenValintatulosUpdateStatuses;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.ValintatulosUpdateStatus;
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
import junit.framework.Assert;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.message.MessageImpl;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
                        Assert.assertTrue(ValintatuloksenTila.KESKEN.equals(r.getValintatuloksenTila()));
                        Assert.assertTrue(IlmoittautumisTila.EI_TEHTY.equals(r.getIlmoittautumisTila()));
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
            importData(erillisHakuHetullaJaSyntymaAjalla());

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
            assertEquals("hakija1", hakemusProto.hakijaOid);
            assertEquals(MockData.hetu, hakemusProto.henkilotunnus);
            assertEquals("Tuomas", hakemusProto.etunimi);
            assertEquals("Hakkarainen", hakemusProto.sukunimi);
            assertEquals("01.01.1901", hakemusProto.syntymaAika);

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
            importData(erillisHakuSyntymaAjalla());

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
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, applicationAsyncResource, failingHenkiloResource, valintaTulosServiceAsyncResource, Schedulers.immediate());
            assertEquals(0, tilaAsyncResource.results.size());
            assertEquals(0, applicationAsyncResource.results.size());
            tuontiService.tuoExcelistä(null, prosessi, erillisHaku, erillisHakuHetullaJaSyntymaAjalla());
            Mockito.verify(prosessi).keskeyta();
        }

        @Test
        public void tilojenTuontiEpaonnistuu() {
            final TilaAsyncResource failingResource = mock(TilaAsyncResource.class);
            ResponseImpl response = (ResponseImpl)Response.ok(new HakukohteenValintatulosUpdateStatuses("viesti", new ArrayList<>()), MediaType.APPLICATION_JSON_TYPE).build();
            when(failingResource.tuoErillishaunTilat(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                    .thenReturn(Observable.error(new FailedHttpException(response)));
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(failingResource, applicationAsyncResource, henkiloAsyncResource, valintaTulosServiceAsyncResource, Schedulers.immediate());
            assertEquals(0, applicationAsyncResource.results.size());
            assertNull(henkiloAsyncResource.henkiloPrototyypit);
            tuontiService.tuoExcelistä(null,prosessi, erillisHaku, erillisHakuHetullaJaSyntymaAjalla());
            Mockito.verify(prosessi).keskeyta((Collection<Poikkeus>)Matchers.any());
        }

        @Test
        public void hakemustenLuontiEpaonnistuu() {
            final ApplicationAsyncResource failingResource = mock(ApplicationAsyncResource.class);
            when(failingResource.putApplicationPrototypes(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("simulated HTTP fail")));
            final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, failingResource, henkiloAsyncResource, valintaTulosServiceAsyncResource, Schedulers.immediate());
            assertEquals(0, tilaAsyncResource.results.size());
            assertNull(henkiloAsyncResource.henkiloPrototyypit);
            KirjeProsessi prosessi = new ErillishakuProsessiDTO(1);
            tuontiService.tuoExcelistä(null,prosessi, erillisHaku, erillisHakuHetullaJaSyntymaAjalla());
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

    }

    protected void importData(InputStream data) {
        final ErillishaunTuontiService tuontiService = new ErillishaunTuontiService(tilaAsyncResource, applicationAsyncResource, henkiloAsyncResource, valintaTulosServiceAsyncResource, Schedulers.immediate());
        tuontiService.tuoExcelistä(null, prosessi, erillisHaku, data);
        Mockito.verify(prosessi).valmistui("ok");
    }
}
