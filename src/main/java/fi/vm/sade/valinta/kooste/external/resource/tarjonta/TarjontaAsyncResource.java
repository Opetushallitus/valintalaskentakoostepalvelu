package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeValintaperusteetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.koulutus.KomoV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.koulutus.KoulutusV1RDTO;
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

  CompletableFuture<HakukohdeV1RDTO> haeHakukohde(String hakukohdeOid);

  CompletableFuture<Set<String>> haunHakukohteet(String hakuOid);

  CompletableFuture<KoulutusV1RDTO> haeToteutus(String toteutusOid);

  CompletableFuture<KomoV1RDTO> haeKoulutus(String koulutusOid);

  /**
   * Fetch from tarjonta-service the hakuOids that should be synchronized.
   *
   * @return Set of hakuOids as strings.
   */
  Observable<Set<String>> findHakuOidsForAutosyncTarjonta();

  CompletableFuture<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes(String hakuOid);

  CompletableFuture<HakukohdeValintaperusteetV1RDTO> findValintaperusteetByOid(String hakukohdeOid);
}
