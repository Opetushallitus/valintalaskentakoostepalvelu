package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.util.concurrent.Futures;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Sukupuoli;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloCreateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi.SYNTYMAAIKAFORMAT;

@Service
public class MockOppijanumerorekisteriAsyncResource implements OppijanumerorekisteriAsyncResource {
    public static AtomicBoolean serviceIsAvailable = new AtomicBoolean(true);
    private static final Logger LOG = LoggerFactory.getLogger(MockOppijanumerorekisteriAsyncResource.class);
    public List<HenkiloCreateDTO> henkiloPrototyypit = null;
    private final Function<List<HenkiloCreateDTO>, Future<List<HenkiloPerustietoDto>>> futureSupplier;
    public MockOppijanumerorekisteriAsyncResource() {
        this.futureSupplier = hp -> {
            if (hp == null) {
                LOG.error("Null-prototyyppi lista!");
                throw new RuntimeException("Null-prototyyppi lista!");
            }
            //this.henkiloPrototyypit = henkiloPrototyypit;
            LOG.info("MockHenkilöAsyncResource sai {}kpl henkilöitä. Tehdään konversio ja palautetaan immediate future.", hp.size());
            henkiloPrototyypit = hp;
            return Futures.immediateFuture(henkiloPrototyypit.stream()
                    .map(MockOppijanumerorekisteriAsyncResource::toHenkiloPerustietoDto)
                    .collect(Collectors.toList()));
        };
    }
    public MockOppijanumerorekisteriAsyncResource(Function<List<HenkiloCreateDTO>, Future<List<HenkiloPerustietoDto>>> futureSupplier) {
        this.futureSupplier = futureSupplier;
    }
    @Override
    public Future<List<HenkiloPerustietoDto>> haeTaiLuoHenkilot(final List<HenkiloCreateDTO> hp) {
        return Optional.ofNullable(MockOppijanumerorekisteriAsyncResource.<List<HenkiloPerustietoDto>>serviceAvailableCheck()).orElseGet(
                () -> futureSupplier.apply(hp));
    }

    public static HenkiloPerustietoDto toHenkiloPerustietoDto(HenkiloCreateDTO proto) {
        final HenkiloPerustietoDto henkiloPerustietoDto = new HenkiloPerustietoDto();
        henkiloPerustietoDto.setHenkiloTyyppi(proto.henkiloTyyppi);
        henkiloPerustietoDto.setKutsumanimi(proto.kutsumanimi);
        henkiloPerustietoDto.setSukunimi(proto.sukunimi);
        henkiloPerustietoDto.setEtunimet(proto.etunimet);
        henkiloPerustietoDto.setHetu(proto.hetu);
        henkiloPerustietoDto.setOidHenkilo(MockData.hakijaOid);
        henkiloPerustietoDto.setSyntymaaika(LocalDate.parse(proto.syntymaaika, SYNTYMAAIKAFORMAT));
        henkiloPerustietoDto.setSukupuoli(Sukupuoli.toSukupuoliString(proto.sukupuoli));
        return henkiloPerustietoDto;
    }
    private static <T> Future<T> serviceAvailableCheck() {
        if(!serviceIsAvailable.get()) {
            return Futures.immediateFailedFuture(new RuntimeException("MockHenkilöpalvelu on kytketty pois päältä!"));
        }
        return null;
    }
}
