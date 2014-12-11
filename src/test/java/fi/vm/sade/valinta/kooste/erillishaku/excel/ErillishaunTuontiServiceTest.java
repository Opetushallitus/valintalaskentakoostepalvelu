package fi.vm.sade.valinta.kooste.erillishaku.excel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.Gson;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiServiceImpl;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import jersey.repackaged.com.google.common.util.concurrent.Futures;

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
        Mockito.verify(prosessi, Mockito.timeout(1000).times(1)).valmistui("ok");

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
        assertEquals("haku1", tilaResult.hakuOid);
        assertEquals("kohde1", tilaResult.hakukohdeOid);
        assertEquals("varsinainen jono", tilaResult.valintatapajononNimi);
        assertEquals(1, tilaResult.erillishaunHakijat.size());
        final ErillishaunHakijaDTO hakija = tilaResult.erillishaunHakijat.iterator().next();
        assertEquals("etunimi", hakija.etunimi);
        assertEquals("sukunimi", hakija.sukunimi);
        assertEquals("jono1", hakija.valintatapajonoOid);
        assertEquals("hakemus1", hakija.hakemusOid);
        assertEquals("hakija1", hakija.hakijaOid);
        System.out.println(new Gson().toJson(tilaAsyncResource.results));
    }

    @Test
    public void henkilonLuontiEpaonnistuu() {
        final HenkiloAsyncResource failingHenkiloResource = Mockito.mock(HenkiloAsyncResource.class);
        Mockito.when(failingHenkiloResource.haeTaiLuoHenkilot(Mockito.any())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("simulated HTTP fail")));
        final ErillishaunTuontiServiceImpl tuontiService = new ErillishaunTuontiServiceImpl(tilaAsyncResource, applicationAsyncResource, failingHenkiloResource);
        tuontiService.tuo(prosessi, erillisHaku, getInputStream());
        Mockito.verify(prosessi, Mockito.timeout(1000).times(1)).keskeyta();
    }

    @Test
    public void tilojenTuontiEpaonnistuu() {
        final TilaAsyncResource failingResource = Mockito.mock(TilaAsyncResource.class);
        Mockito.when(failingResource.tuoErillishaunTilat(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("simulated HTTP fail")));
        final ErillishaunTuontiServiceImpl tuontiService = new ErillishaunTuontiServiceImpl(failingResource, applicationAsyncResource, henkiloAsyncResource);
        tuontiService.tuo(prosessi, erillisHaku, getInputStream());
        Mockito.verify(prosessi, Mockito.timeout(1000).times(1)).keskeyta();
    }

    @Test
    public void hakemustenLuontiEpaonnistuu() {
        final ApplicationAsyncResource failingResource = Mockito.mock(ApplicationAsyncResource.class);
        Mockito.when(failingResource.putApplicationPrototypes(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("simulated HTTP fail")));
        final ErillishaunTuontiServiceImpl tuontiService = new ErillishaunTuontiServiceImpl(tilaAsyncResource, failingResource, henkiloAsyncResource);
        tuontiService.tuo(prosessi, erillisHaku, getInputStream());
        Mockito.verify(prosessi, Mockito.timeout(1000).times(1)).keskeyta();
    }

    private InputStream getInputStream() {
        try {
            return new ClassPathResource("kustom_erillishaku.xlsx").getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

class MockHenkiloAsyncResource implements HenkiloAsyncResource {
    public List<HenkiloCreateDTO> henkiloPrototyypit;

    @Override
    public Future<List<Henkilo>> haeTaiLuoHenkilot(final List<HenkiloCreateDTO> henkiloPrototyypit) {
        this.henkiloPrototyypit = henkiloPrototyypit;
        return Futures.immediateFuture(henkiloPrototyypit.stream()
            .map(prototyyppi -> toHenkilo(prototyyppi))
            .collect(Collectors.toList()));
    }

    private Henkilo toHenkilo(HenkiloCreateDTO proto) {
        final Henkilo henkilo = new Henkilo();
        henkilo.setHenkiloTyyppi(proto.henkiloTyyppi);
        henkilo.setKutsumanimi(proto.kutsumanimi);
        henkilo.setSukunimi(proto.sukunimi);
        henkilo.setEtunimet(proto.etunimet);
        henkilo.setHetu(proto.hetu);
        henkilo.setOidHenkilo("hakija1");
        return henkilo;
    }
}

class MockApplicationAsyncResource implements ApplicationAsyncResource {
    static class Result {
        public final  String hakuOid;
        public final String hakukohdeOid;
        public final String tarjoajaOid;
        public final Collection<HakemusPrototyyppi> hakemusPrototyypit;

        public Result(final String hakuOid, final String hakukohdeOid, final String tarjoajaOid, final Collection<HakemusPrototyyppi> hakemusPrototyypit) {
            this.hakuOid = hakuOid;
            this.hakukohdeOid = hakukohdeOid;
            this.tarjoajaOid = tarjoajaOid;
            this.hakemusPrototyypit = hakemusPrototyypit;
        }
    }
    public final List<Result> results = new ArrayList<>();
    @Override
    public Future<List<Hakemus>> putApplicationPrototypes(final String hakuOid, final String hakukohdeOid, final String tarjoajaOid, final Collection<HakemusPrototyyppi> hakemusPrototyypit) {
        results.add(new Result(hakuOid, hakukohdeOid, tarjoajaOid, hakemusPrototyypit));
        return Futures.immediateFuture(hakemusPrototyypit.stream()
                .map(prototyyppi -> toHakemus(prototyyppi))
                .collect(Collectors.toList())
        );
    }

    private Hakemus toHakemus(HakemusPrototyyppi prototyyppi) {
        final Hakemus hakemus = new Hakemus();
        hakemus.setAnswers(new Answers());
        final Map<String, String> henkilotiedot = hakemus.getAnswers().getHenkilotiedot();
        henkilotiedot.put("Henkilotunnus", prototyyppi.henkilotunnus);
        henkilotiedot.put("Etunimet", prototyyppi.etunimi);
        henkilotiedot.put("Kutsumanimi", prototyyppi.etunimi);
        henkilotiedot.put("Sukunimi", prototyyppi.sukunimi);
        hakemus.setOid("hakemus1");
        hakemus.setPersonOid(prototyyppi.hakijaOid);
        return hakemus;
    }

    @Override
    public Future<List<ApplicationAdditionalDataDTO>> getApplicationAdditionalData(final String hakuOid, final String hakukohdeOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<List<Hakemus>> getApplicationsByOid(final String hakuOid, final String hakukohdeOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<List<Hakemus>> getApplicationsByOids(final Collection<String> hakemusOids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Peruutettava getApplicationsByOid(final String hakuOid, final String hakukohdeOid, final Consumer<List<Hakemus>> callback, final Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Peruutettava getApplicationAdditionalData(final String hakuOid, final String hakukohdeOid, final Consumer<List<ApplicationAdditionalDataDTO>> callback, final Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }
}

class MockTilaAsyncResource implements TilaAsyncResource {
    static class Result {
        public final String hakuOid;
        public final String hakukohdeOid;
        public final String valintatapajononNimi;
        public final Collection<ErillishaunHakijaDTO> erillishaunHakijat;

        public Result(final String hakuOid, final String hakukohdeOid, final String valintatapajononNimi, final Collection<ErillishaunHakijaDTO> erillishaunHakijat) {
            this.hakuOid = hakuOid;
            this.hakukohdeOid = hakukohdeOid;
            this.valintatapajononNimi = valintatapajononNimi;
            this.erillishaunHakijat = erillishaunHakijat;
        }
    }
    public final List<Result> results = new ArrayList<>();
    @Override
    public Future<Response> tuoErillishaunTilat(final String hakuOid, final String hakukohdeOid, final String valintatapajononNimi, final Collection<ErillishaunHakijaDTO> erillishaunHakijat) {
        results.add(new Result(hakuOid, hakukohdeOid, valintatapajononNimi, erillishaunHakijat));
        return Futures.immediateFuture(null);
    }
}