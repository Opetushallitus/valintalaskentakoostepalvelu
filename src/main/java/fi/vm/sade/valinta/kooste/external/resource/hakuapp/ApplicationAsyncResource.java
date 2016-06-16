package fi.vm.sade.valinta.kooste.external.resource.hakuapp;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusPrototyyppi;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface ApplicationAsyncResource {
    List<String> DEFAULT_KEYS = Arrays.asList("applicationSystemId", "oid", "personOid", "answers.henkilotiedot", "answers.hakutoiveet", "hakutapa", "maxApplicationOptions");

    Observable<List<Hakemus>> getApplicationsByOid(String hakuOid, String hakukohdeOid);

    Observable<List<Hakemus>> getApplicationsByOids(String hakuOid, Collection<String> hakukohdeOids);

    Observable<List<Hakemus>> getApplicationsByOidsWithPOST(String hakuOid, Collection<String> hakukohdeOids);

    Observable<List<Hakemus>> getApplicationsByHakemusOids(Collection<String> hakemusOids);
    Observable<List<Hakemus>> getApplicationsByHakemusOids(Collection<String> hakemusOids, Collection<String> keys);

    Future<List<Hakemus>> putApplicationPrototypes(String hakuOid, String hakukohdeOid, String tarjoajaOid, Collection<HakemusPrototyyppi> hakemusPrototyypit);

    Observable<Hakemus> getApplication(String hakemusOid);

    Future<List<Hakemus>> getApplicationsByOids(Collection<String> hakemusOids);

    Peruutettava getApplicationsByOids(Collection<String> hakemusOids, Consumer<List<Hakemus>> callback, Consumer<Throwable> failureCallback);

    Peruutettava getApplicationAdditionalData(String hakuOid, String hakukohdeOid, Consumer<List<ApplicationAdditionalDataDTO>> callback, Consumer<Throwable> failureCallback);

    Peruutettava getApplicationAdditionalData(Collection<String> hakemusOids, Consumer<List<ApplicationAdditionalDataDTO>> callback, Consumer<Throwable> failureCallback);

    Observable<Response> putApplicationAdditionalData(String hakuOid, String hakukohdeOid, List<ApplicationAdditionalDataDTO> additionalData);

    Observable<Response> putApplicationAdditionalData(String hakuOid, List<ApplicationAdditionalDataDTO> additionalData);

    Observable<Response> changeStateOfApplicationsToPassive(List<String> hakemusOid, String reason);
}
