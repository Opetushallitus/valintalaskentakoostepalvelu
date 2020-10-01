package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeValintaperusteetV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultOrganization;
import io.reactivex.Observable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface TarjontaAsyncResource {
  CompletableFuture<Haku> haeHaku(String hakuOid);

  Observable<List<ResultOrganization>> hakukohdeSearchByOrganizationGroupOids(
      Collection<String> organizationGroupOids);

  Observable<List<ResultOrganization>> hakukohdeSearchByOrganizationOids(
      Collection<String> organizationOids);

  CompletableFuture<Hakukohde> haeHakukohde(String hakukohdeOid);

  CompletableFuture<Set<String>> haunHakukohteet(String hakuOid);

  CompletableFuture<Toteutus> haeToteutus(String toteutusOid);

  CompletableFuture<Koulutus> haeKoulutus(String koulutusOid);

  /**
   * Fetch from tarjonta-service the hakuOids that should be synchronized.
   *
   * @return Set of hakuOids as strings.
   */
  CompletableFuture<Set<String>> findHakuOidsForAutosyncTarjonta();

  CompletableFuture<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes(String hakuOid);

  CompletableFuture<HakukohdeValintaperusteetV1RDTO> findValintaperusteetByOid(String hakukohdeOid);
}
