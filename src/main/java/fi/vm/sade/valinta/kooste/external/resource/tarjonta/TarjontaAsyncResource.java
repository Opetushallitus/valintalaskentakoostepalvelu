package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultOrganization;
import io.reactivex.Observable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TarjontaAsyncResource {
    Observable<HakuV1RDTO> haeHaku(String hakuOid);

    Observable<List<ResultOrganization>> hakukohdeSearchByOrganizationGroupOids(Collection<String> organizationGroupOids);
    Observable<List<ResultOrganization>> hakukohdeSearchByOrganizationOids(Collection<String> organizationOids);

    Observable<HakukohdeV1RDTO> haeHakukohde(String hakukohdeOid);

    /**
     * Fetch from tarjonta-service the hakuOids that should be synchronized.
     *
     * @return Set of hakuOids as strings.
     */
    Observable<Set<String>> findHakuOidsForAutosyncTarjonta();

    Observable<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes(String hakuOid);
}
