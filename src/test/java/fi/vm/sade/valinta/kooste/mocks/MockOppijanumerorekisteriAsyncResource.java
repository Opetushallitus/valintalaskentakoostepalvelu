package fi.vm.sade.valinta.kooste.mocks;

import static fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi.SYNTYMAAIKAFORMAT_JSON;

import com.google.common.util.concurrent.Futures;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Sukupuoli;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloViiteDto;
import io.reactivex.Observable;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockOppijanumerorekisteriAsyncResource implements OppijanumerorekisteriAsyncResource {
  public static AtomicBoolean serviceIsAvailable = new AtomicBoolean(true);
  private static final Logger LOG =
      LoggerFactory.getLogger(MockOppijanumerorekisteriAsyncResource.class);
  public List<HenkiloCreateDTO> henkiloPrototyypit = null;
  private final Function<List<HenkiloCreateDTO>, Future<List<HenkiloPerustietoDto>>> futureSupplier;

  public MockOppijanumerorekisteriAsyncResource() {
    this.futureSupplier =
        hp -> {
          if (hp == null) {
            LOG.error("Null-prototyyppi lista!");
            throw new RuntimeException("Null-prototyyppi lista!");
          }
          // this.henkiloPrototyypit = henkiloPrototyypit;
          LOG.info(
              "MockOppijanumerorekisteriAsyncResource sai {} kpl henkilöitä. Tehdään konversio ja palautetaan immediate future.",
              hp.size());
          henkiloPrototyypit = hp;
          return Futures.immediateFuture(
              henkiloPrototyypit.stream()
                  .map(MockOppijanumerorekisteriAsyncResource::toHenkiloPerustietoDto)
                  .collect(Collectors.toList()));
        };
  }

  public MockOppijanumerorekisteriAsyncResource(
      Function<List<HenkiloCreateDTO>, Future<List<HenkiloPerustietoDto>>> futureSupplier) {
    this.futureSupplier = futureSupplier;
  }

  @Override
  public Observable<List<HenkiloPerustietoDto>> haeTaiLuoHenkilot(final List<HenkiloCreateDTO> hp) {
    return Observable.fromFuture(
        Optional.ofNullable(
                MockOppijanumerorekisteriAsyncResource
                    .<List<HenkiloPerustietoDto>>serviceAvailableCheck())
            .orElseGet(() -> futureSupplier.apply(hp)));
  }

  @Override
  public CompletableFuture<Map<String, HenkiloPerustietoDto>> haeHenkilot(List<String> personOids) {
    return CompletableFuture.completedFuture(new HashMap<>());
  }

  @Override
  public CompletableFuture<List<HenkiloViiteDto>> haeHenkiloOidDuplikaatit(Set<String> personOids) {
    return CompletableFuture.completedFuture(
        personOids.stream().map(oid -> new HenkiloViiteDto(oid, oid)).collect(Collectors.toList()));
  }

  public static HenkiloPerustietoDto toHenkiloPerustietoDto(HenkiloCreateDTO proto) {
    final HenkiloPerustietoDto henkiloPerustietoDto = new HenkiloPerustietoDto();
    henkiloPerustietoDto.setHenkiloTyyppi(proto.henkiloTyyppi);
    henkiloPerustietoDto.setKutsumanimi(proto.kutsumanimi);
    henkiloPerustietoDto.setSukunimi(proto.sukunimi);
    henkiloPerustietoDto.setEtunimet(proto.etunimet);
    henkiloPerustietoDto.setHetu(proto.hetu);
    henkiloPerustietoDto.setOidHenkilo(MockData.hakijaOid);
    henkiloPerustietoDto.setSyntymaaika(LocalDate.parse(proto.syntymaaika, SYNTYMAAIKAFORMAT_JSON));
    henkiloPerustietoDto.setSukupuoli(Sukupuoli.toSukupuoliString(proto.sukupuoli));
    return henkiloPerustietoDto;
  }

  private static <T> Future<T> serviceAvailableCheck() {
    if (!serviceIsAvailable.get()) {
      return Futures.immediateFailedFuture(
          new RuntimeException("MockOppijanumerorekisteripalvelu on kytketty pois päältä!"));
    }
    return null;
  }
}
