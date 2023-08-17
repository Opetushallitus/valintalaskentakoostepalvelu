package fi.vm.sade.valinta.kooste.mocks;

import static fi.vm.sade.valinta.kooste.mocks.MockData.hakuOid;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.*;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.HakukohdeValintaperusteetDTO;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("mockresources")
@Service
public class MockTarjontaAsyncService implements TarjontaAsyncResource {
  private static Map<String, Haku> mockHaku = new HashMap<>();

  @Override
  public CompletableFuture<Haku> haeHaku(String hakuOid) {
    if (mockHaku.containsKey(hakuOid)) {
      return CompletableFuture.completedFuture(mockHaku.get(hakuOid));
    }
    return CompletableFuture.completedFuture(
        new Haku(hakuOid, new HashMap<>(), new HashSet<>(), null, null, null, null, null, null));
  }

  @Override
  public CompletableFuture<Set<String>> hakukohdeSearchByOrganizationGroupOids(
      Iterable<String> organizationGroupOids) {
    return null;
  }

  @Override
  public CompletableFuture<Set<String>> hakukohdeSearchByOrganizationOids(
      Iterable<String> organizationOids) {
    return null;
  }

  @Override
  public CompletableFuture<AbstractHakukohde> haeHakukohde(String hakukohdeOid) {
    return CompletableFuture.completedFuture(
        new TarjontaHakukohde(
            hakukohdeOid,
            null,
            new HashMap<>(),
            hakuOid,
            ImmutableSet.of("1.2.3.44444.5"),
            ImmutableSet.of("mocktoteutusoid"),
            null,
            new HashSet<>(),
            null,
            Collections.emptySet(),
            null));
  }

  @Override
  public CompletableFuture<Set<String>> haunHakukohteet(String hakuOid) {
    return null;
  }

  @Override
  public CompletableFuture<Toteutus> haeToteutus(String toteutusOid) {
    if ("mocktoteutusoid".equals(toteutusOid)) {
      Set<String> kielet = new HashSet<>();
      kielet.add("kieli_fi");
      kielet.add("kieli_sv");
      return CompletableFuture.completedFuture(
          new Toteutus(toteutusOid, null, null, null, kielet, new HashSet<>()));
    }
    return null;
  }

  @Override
  public CompletableFuture<Koulutus> haeKoulutus(String koulutusOid) {
    return null;
  }

  @Override
  public CompletableFuture<Set<String>> findHakuOidsForAutosyncTarjonta() {
    Set<String> set = new HashSet<>();
    set.add(hakuOid);
    set.add(hakuOid + "-1");
    return CompletableFuture.completedFuture(set);
  }

  @Override
  public CompletableFuture<Map<String, List<String>>> haunHakukohderyhmatCached(String hakuOid) {
    return hakukohdeRyhmasForHakukohdes(hakuOid);
  }

  @Override
  public CompletableFuture<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes(String hakuOid) {
    return CompletableFuture.completedFuture(Maps.newHashMap());
  }

  @Override
  public CompletableFuture<List<String>> hakukohdeRyhmasForHakukohde(String hakukohdeOid) {
    return null;
  }

  @Override
  public CompletableFuture<HakukohdeValintaperusteetDTO> findValintaperusteetByOid(
      String hakukohdeOid) {
    return null;
  }

  public static void setMockHaku(Haku mockHaku) {
    MockTarjontaAsyncService.mockHaku.put(mockHaku.oid, mockHaku);
  }

  public static void clear() {
    mockHaku = new HashMap<>();
  }
}
