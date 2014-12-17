package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import jersey.repackaged.com.google.common.util.concurrent.Futures;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class MockHenkiloAsyncResource implements HenkiloAsyncResource {
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
