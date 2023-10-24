package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.KielisyysDto;
import fi.vm.sade.valinta.kooste.util.AtaruHakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import io.reactivex.Observable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.mockito.internal.util.collections.Sets;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Profile("mockresources")
@Service
public class MockAtaruAsyncResource implements AtaruAsyncResource {

  public static AtomicBoolean serviceIsAvailable = new AtomicBoolean(true);

  private static <T> CompletableFuture<T> serviceAvailableCheck() {
    if (!serviceIsAvailable.get()) {
      return CompletableFuture.failedFuture(
          new RuntimeException("MockAtaru on kytketty pois päältä!"));
    }
    return null;
  }

  private static List<HakemusWrapper> byHakukohdeRes = Lists.newArrayList();
  private static List<HakemusWrapper> byOidsResult = Lists.newArrayList();

  public static class Result {
    public final Collection<AtaruHakemusPrototyyppi> hakemusPrototyypit;

    public Result(final Collection<AtaruHakemusPrototyyppi> hakemusPrototyypit) {
      this.hakemusPrototyypit = hakemusPrototyypit;
    }
  }

  public final List<MockAtaruAsyncResource.Result> results = new ArrayList<>();

  @Override
  public CompletableFuture<List<HakemusWrapper>> getApplicationsByHakukohde(String hakukohdeOid) {
    return CompletableFuture.completedFuture(byHakukohdeRes);
  }

  @Override
  public CompletableFuture<List<HakemusWrapper>> getApplicationsByHakukohde(
      String hakukohdeOid, boolean withHarkinnanvaraisuustieto) {
    return CompletableFuture.completedFuture(byHakukohdeRes);
  }

  @Override
  public CompletableFuture<List<HakemusWrapper>> getApplicationsByOids(List<String> oids) {
    return CompletableFuture.completedFuture(byOidsResult);
  }

  @Override
  public CompletableFuture<List<HakemusWrapper>> getApplicationsByOidsWithHarkinnanvaraisuustieto(
      List<String> oids) {
    return CompletableFuture.completedFuture(byOidsResult); // todo add harkinnanvaraisuustietos
  }

  @Override
  public Observable<List<HakemusWrapper>> putApplicationPrototypes(
      Collection<AtaruHakemusPrototyyppi> hakemusPrototyypit) {
    return Observable.fromFuture(
        Optional.ofNullable(MockAtaruAsyncResource.<List<HakemusWrapper>>serviceAvailableCheck())
            .orElseGet(
                () -> {
                  results.add(new Result(hakemusPrototyypit));
                  return CompletableFuture.completedFuture(
                      hakemusPrototyypit.stream()
                          .map(prototyyppi -> toHakemus(prototyyppi))
                          .collect(Collectors.toList()));
                }));
  }

  public static void setByHakukohdeResult(List<HakemusWrapper> hakemukset) {
    byHakukohdeRes = hakemukset;
  }

  public static void setByOidsResult(List<HakemusWrapper> hakemukset) {
    byOidsResult = hakemukset;
  }

  public static void clear() {
    byOidsResult = Lists.newArrayList();
    byHakukohdeRes = Lists.newArrayList();
  }

  public static List<AtaruHakemus> getAtaruHakemukset(Set<String> oids) {
    try {
      List<AtaruHakemus> hakemukset =
          new Gson()
              .fromJson(
                  IOUtils.toString(new ClassPathResource("ataruhakemukset.json").getInputStream()),
                  new TypeToken<List<AtaruHakemus>>() {}.getType());

      if (oids == null) {
        return hakemukset;
      } else {
        return hakemukset.stream()
            .filter(h -> oids.contains(h.getHakemusOid()))
            .collect(Collectors.toList());
      }
    } catch (Exception e) {
      throw new RuntimeException("Couldn't fetch mock ataru application", e);
    }
  }

  private static HenkiloPerustietoDto createHenkilo() {
    HenkiloPerustietoDto henkilo = new HenkiloPerustietoDto();
    henkilo.setSukunimi("TAUsuL4BQc");
    henkilo.setEtunimet("Zl2A5");
    henkilo.setOidHenkilo("1.2.246.562.24.86368188549");
    henkilo.setHetu("020202A0202");
    henkilo.setAidinkieli(new KielisyysDto("fi", "suomi"));
    return henkilo;
  }

  public static HakemusWrapper getAtaruHakemusWrapper(String s) {
    AtaruHakemus hakemus = getAtaruHakemukset(Sets.newSet(s)).iterator().next();
    return new AtaruHakemusWrapper(hakemus, createHenkilo());
  }

  private HakemusWrapper toHakemus(AtaruHakemusPrototyyppi prototyyppi) {
    final AtaruHakemus hakemus = new AtaruHakemus();

    HenkiloPerustietoDto henkilo = new HenkiloPerustietoDto();
    henkilo.setSukunimi(prototyyppi.getSukunimi());
    henkilo.setEtunimet(prototyyppi.getEtunimi());
    henkilo.setHetu(prototyyppi.getHenkilotunnus());
    henkilo.setKutsumanimi(prototyyppi.getEtunimi()); // TODO
    henkilo.setSyntymaaika(
        LocalDate.parse(prototyyppi.getSyntymaAika(), DateTimeFormatter.ofPattern("dd.MM.yyyy")));

    hakemus.setHakemusOid(MockData.hakemusOid);
    hakemus.setPersonOid(prototyyppi.getHakijaOid());
    return new AtaruHakemusWrapper(hakemus, henkilo);
  }
}
