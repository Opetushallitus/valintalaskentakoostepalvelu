package fi.vm.sade.valinta.kooste.external.resource.hakuapp;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusPrototyyppi;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface ApplicationAsyncResource {

    List<String> DEFAULT_KEYS = Arrays.asList("applicationSystemId", "oid", "personOid", "answers.henkilotiedot", "answers.lisatiedot", "answers.hakutoiveet", "hakutapa", "maxApplicationOptions");
    List<String> DEFAULT_STATES = Arrays.asList("ACTIVE", "INCOMPLETE");
    int DEFAULT_ROW_LIMIT = 100000;
    int DEFAULT_PART_ROW_LIMIT = 10000;

    Observable<List<Hakemus>> getApplicationsByOid(String hakuOid, String hakukohdeOid);

    Observable<Set<String>> getApplicationOids(String hakuOid, String hakukohdeOid);

    Observable<List<Hakemus>> getApplicationsByOids(String hakuOid, Collection<String> hakukohdeOids);

    Observable<List<Hakemus>> getApplicationsByOidsWithPOST(String hakuOid, Collection<String> hakukohdeOids);

    Observable<List<Hakemus>> getApplicationsByHakemusOids(List<String> hakemusOids);
    List<Hakemus> getApplicationsByhakemusOidsInParts(String hakuOid, List<String> hakemusOids, Collection<String> keys);

    Observable<List<Hakemus>> putApplicationPrototypes(String hakuOid, String hakukohdeOid, String tarjoajaOid, Collection<HakemusPrototyyppi> hakemusPrototyypit);

    Observable<Hakemus> getApplication(String hakemusOid);

    Future<List<Hakemus>> getApplicationsByOids(Collection<String> hakemusOids);

    Peruutettava getApplicationsByOids(Collection<String> hakemusOids, Consumer<List<Hakemus>> callback, Consumer<Throwable> failureCallback);

    Observable<Response> changeStateOfApplicationsToPassive(List<String> hakemusOid, String reason);
}
