package fi.vm.sade.valinta.kooste.external.resource.hakuapp;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ApplicationAsyncResource {
    Observable<List<Hakemus>> getApplicationsByOid(String hakuOid, String hakukohdeOid);

    Future<List<Hakemus>> putApplicationPrototypes(String hakuOid, String hakukohdeOid, String tarjoajaOid, Collection<HakemusPrototyyppi> hakemusPrototyypit);

	Observable<Hakemus> getApplication(String hakemusOid);

    Future<List<Hakemus>> getApplicationsByOids(Collection<String> hakemusOids);

    Peruutettava getApplicationsByOids(Collection<String> hakemusOids, Consumer<List<Hakemus>> callback, Consumer<Throwable> failureCallback);

    Peruutettava getApplicationsByOid(String hakuOid, String hakukohdeOid, Consumer<List<Hakemus>> callback, Consumer<Throwable> failureCallback);

    Peruutettava getApplicationAdditionalData(String hakuOid, String hakukohdeOid, Consumer<List<ApplicationAdditionalDataDTO>> callback, Consumer<Throwable> failureCallback);

    Peruutettava getApplicationAdditionalData(Collection<String> hakemusOids, Consumer<List<ApplicationAdditionalDataDTO>> callback, Consumer<Throwable> failureCallback);

    Observable<Response> putApplicationAdditionalData(String hakuOid, String hakukohdeOid, List<ApplicationAdditionalDataDTO> additionalData);
}
