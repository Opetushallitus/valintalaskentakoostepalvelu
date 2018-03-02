package fi.vm.sade.valinta.kooste.external.resource.hakuapp;

import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusPrototyyppi;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ApplicationAsyncResource {

    List<String> DEFAULT_KEYS = Arrays.asList("applicationSystemId", "oid", "personOid", "answers.henkilotiedot", "answers.lisatiedot", "answers.hakutoiveet", "hakutapa", "maxApplicationOptions");
    List<String> DEFAULT_STATES = Arrays.asList("ACTIVE", "INCOMPLETE");
    int DEFAULT_ROW_LIMIT = 100000;

    Observable<List<AtaruHakemus>> getAtaruApplicationsByHakukohde(String hakukohdeOid);

    Observable<List<Hakemus>> getApplicationsByOid(String hakuOid, String hakukohdeOid);

    Observable<Set<String>> getApplicationOids(String hakuOid, String hakukohdeOid);

    Observable<List<Hakemus>> getApplicationsByOids(String hakuOid, Collection<String> hakukohdeOids);

    Observable<List<Hakemus>> getApplicationsByOidsWithPOST(String hakuOid, Collection<String> hakukohdeOids);

    Observable<List<Hakemus>> getApplicationsByHakemusOids(List<String> hakemusOids);
    Observable<List<Hakemus>> getApplicationsByhakemusOidsInParts(String hakuOid, List<String> hakemusOids, Collection<String> keys);

    Observable<List<Hakemus>> putApplicationPrototypes(String hakuOid, String hakukohdeOid, String tarjoajaOid, Collection<HakemusPrototyyppi> hakemusPrototyypit);

    Observable<Hakemus> getApplication(String hakemusOid);

    Observable<List<Hakemus>> getApplicationsByOids(Collection<String> hakemusOids);

    Observable<Response> changeStateOfApplicationsToPassive(List<String> hakemusOid, String reason);
}
