package fi.vm.sade.valinta.kooste.erillishaku.excel;

import com.google.gson.Gson;
import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockData;
import fi.vm.sade.valinta.kooste.mocks.MockHenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockTilaAsyncResource;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiServiceImpl;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import jersey.repackaged.com.google.common.util.concurrent.Futures;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class ErillishaunTuontiServiceTest {
    final MockHenkiloAsyncResource henkiloAsyncResource = new MockHenkiloAsyncResource();
    final MockApplicationAsyncResource applicationAsyncResource = new MockApplicationAsyncResource();
    final MockTilaAsyncResource tilaAsyncResource = new MockTilaAsyncResource();
    final KirjeProsessi prosessi = Mockito.mock(KirjeProsessi.class);
    final ErillishakuDTO erillisHaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "haku1", "kohde1", "tarjoaja1", "jono1", "varsinainen jono");

    @Test
    public void tuontiSuoritetaan() throws IOException, InterruptedException {
        final ErillishaunTuontiServiceImpl tuontiService = new ErillishaunTuontiServiceImpl(tilaAsyncResource, applicationAsyncResource, henkiloAsyncResource);
        tuontiService.tuo(prosessi, erillisHaku, getInputStream());
        Mockito.verify(prosessi, Mockito.timeout(10000).times(1)).valmistui("ok");

        // tarkistetaan henkilöt
        assertEquals(1, henkiloAsyncResource.henkiloPrototyypit.size());
        final HenkiloCreateDTO henkilo = henkiloAsyncResource.henkiloPrototyypit.get(0);
        assertEquals("etunimi", henkilo.etunimet);
        assertEquals("etunimi", henkilo.kutsumanimi);
        assertEquals("sukunimi", henkilo.sukunimi);
        assertEquals("hetu", henkilo.hetu);
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
        assertEquals("hetu", hakemusProto.henkilotunnus);
        assertEquals("etunimi", hakemusProto.etunimi);
        assertEquals("sukunimi", hakemusProto.sukunimi);
        assertEquals(null, hakemusProto.syntymaAika);

        // tarkistetaan tilatulokset
        assertEquals(1, tilaAsyncResource.results.size());
        final MockTilaAsyncResource.Result tilaResult = tilaAsyncResource.results.get(0);
        assertEquals(MockData.hakuOid, tilaResult.hakuOid);
        assertEquals(MockData.kohdeOid, tilaResult.hakukohdeOid);
        assertEquals("varsinainen jono", tilaResult.valintatapajononNimi);
        assertEquals(1, tilaResult.erillishaunHakijat.size());
        final ErillishaunHakijaDTO hakija = tilaResult.erillishaunHakijat.iterator().next();
        assertEquals("etunimi", hakija.etunimi);
        assertEquals("sukunimi", hakija.sukunimi);
        assertEquals(MockData.valintatapajonoOid, hakija.valintatapajonoOid);
        assertEquals("hakemus1", hakija.hakemusOid);
        assertEquals("hakija1", hakija.hakijaOid);
        System.out.println(new Gson().toJson(tilaAsyncResource.results));
    }

    @Test
    public void henkilonLuontiEpaonnistuu() {
        final HenkiloAsyncResource failingHenkiloResource = Mockito.mock(HenkiloAsyncResource.class);
        Mockito.when(failingHenkiloResource.haeTaiLuoHenkilot(Mockito.any())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("simulated HTTP fail")));
        final ErillishaunTuontiServiceImpl tuontiService = new ErillishaunTuontiServiceImpl(tilaAsyncResource, applicationAsyncResource, failingHenkiloResource);
        assertEquals(0, tilaAsyncResource.results.size());
        assertEquals(0, applicationAsyncResource.results.size());
        tuontiService.tuo(prosessi, erillisHaku, getInputStream());
        Mockito.verify(prosessi, Mockito.timeout(10000).times(1)).valmistui("ok");
    }

    @Test
    public void tilojenTuontiEpaonnistuu() {
        final TilaAsyncResource failingResource = Mockito.mock(TilaAsyncResource.class);
        Mockito.when(failingResource.tuoErillishaunTilat(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("simulated HTTP fail")));
        final ErillishaunTuontiServiceImpl tuontiService = new ErillishaunTuontiServiceImpl(failingResource, applicationAsyncResource, henkiloAsyncResource);
        assertEquals(0, applicationAsyncResource.results.size());
        assertNull(henkiloAsyncResource.henkiloPrototyypit);
        tuontiService.tuo(prosessi, erillisHaku, getInputStream());
        Mockito.verify(prosessi, Mockito.timeout(10000).times(1)).keskeyta();
    }

    @Test
    public void hakemustenLuontiEpaonnistuu() {
        final ApplicationAsyncResource failingResource = Mockito.mock(ApplicationAsyncResource.class);
        Mockito.when(failingResource.putApplicationPrototypes(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("simulated HTTP fail")));
        Mockito.when(failingResource.getApplicationsByOid(Mockito.anyString(), Mockito.anyString())).thenReturn(applicationAsyncResource.getApplicationsByOid("haku1", "kohde1"));
        final ErillishaunTuontiServiceImpl tuontiService = new ErillishaunTuontiServiceImpl(tilaAsyncResource, failingResource, henkiloAsyncResource);
        assertEquals(0, tilaAsyncResource.results.size());
        assertNull(henkiloAsyncResource.henkiloPrototyypit);
        tuontiService.tuo(prosessi, erillisHaku, getInputStream());
        Mockito.verify(prosessi, Mockito.timeout(10000).times(1)).valmistui("ok");
    }

    private InputStream getInputStream() {
        try {
            return new ClassPathResource("kustom_erillishaku.xlsx").getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}