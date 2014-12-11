package fi.vm.sade.valinta.kooste.erillishaku.excel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import fi.vm.sade.authentication.model.Henkilo;
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
    @Test
    public void tuontiSuoritetaan() throws IOException, InterruptedException {
        final HenkiloAsyncResource henkiloAsyncResource = new MockHenkiloAsyncResource();
        final ApplicationAsyncResource applicationAsyncResource = new MockApplicationAsyncResource();
        final TilaAsyncResource tilaAsyncResource = new MockTilaAsyncResource();
        final ErillishaunTuontiServiceImpl tuontiService = new ErillishaunTuontiServiceImpl(tilaAsyncResource, applicationAsyncResource, henkiloAsyncResource);
        final ErillishakuDTO erillisHaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "1", "1", "1", "1", "varsinainen jono");
        final InputStream inputStream = new ClassPathResource("kustom_erillishaku.xlsx").getInputStream();
        final KirjeProsessi prosessi = Mockito.mock(KirjeProsessi.class);
        tuontiService.tuo(prosessi, erillisHaku, inputStream);
        Mockito.verify(prosessi, Mockito.timeout(1000).times(1)).valmistui("ok");
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
        return Futures.immediateCancelledFuture();
    }
}

class MockApplicationAsyncResource implements ApplicationAsyncResource {
    @Override
    public Future<List<Hakemus>> putApplicationPrototypes(final String hakuOid, final String hakukohdeOid, final String tarjoajaOid, final Collection<HakemusPrototyyppi> hakemusPrototyypit) {
        return Futures.immediateFuture(hakemusPrototyypit.stream()
                .map(prototyyppi -> toHakemus(prototyyppi))
                .collect(Collectors.toList())
        );
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

    private Hakemus toHakemus(HakemusPrototyyppi prototyyppi) {
        final Hakemus hakemus = new Hakemus();
        hakemus.setAnswers(new Answers());
        hakemus.getAnswers().getHenkilotiedot().put("Henkilotunnus", prototyyppi.henkilotunnus);
        return hakemus;
    }
}

class MockHenkiloAsyncResource implements HenkiloAsyncResource {
    public List<HenkiloCreateDTO> henkiloPrototyypit;

    @Override
    public Future<List<Henkilo>> haeHenkilot(final List<HenkiloCreateDTO> henkiloPrototyypit) {
        this.henkiloPrototyypit = henkiloPrototyypit;
        return Futures.immediateFuture(henkiloPrototyypit.stream()
            .map(prototyyppi -> toHenkilo(prototyyppi))
            .collect(Collectors.toList()));
    }

    private Henkilo toHenkilo(HenkiloCreateDTO proto) {
        final Henkilo henkilo = new Henkilo();
        henkilo.setEtunimet(proto.etunimet);
        henkilo.setHetu(proto.hetu);
        return henkilo;
    }
}