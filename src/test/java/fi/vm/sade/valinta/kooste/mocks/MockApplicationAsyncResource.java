package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.util.concurrent.Futures;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.*;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class MockApplicationAsyncResource implements ApplicationAsyncResource {
    public static AtomicBoolean serviceIsAvailable = new AtomicBoolean(true);

    private static AtomicReference<List<Hakemus>> resultReference = new AtomicReference<>();
    private static AtomicReference<List<AtaruHakemus>> ataruResultReference = new AtomicReference<>();
    private static AtomicReference<List<Hakemus>> resultByOidReference = new AtomicReference<>();
    private static AtomicReference<List<ApplicationAdditionalDataDTO>> additionalDataResultReference = new AtomicReference<>();
    private static AtomicReference<List<ApplicationAdditionalDataDTO>> additionalDataResultByOidReference = new AtomicReference<>();
    private static AtomicReference<List<ApplicationAdditionalDataDTO>> additionalDataPutReference = new AtomicReference<>();

    public static class Result {
        public final String hakuOid;
        public final String hakukohdeOid;
        public final String tarjoajaOid;
        public final Collection<HakemusPrototyyppi> hakemusPrototyypit;
        public Result(final String hakuOid, final String hakukohdeOid, final String tarjoajaOid, final Collection<HakemusPrototyyppi> hakemusPrototyypit) {
            this.hakuOid = hakuOid;
            this.hakukohdeOid = hakukohdeOid;
            this.tarjoajaOid = tarjoajaOid;
            this.hakemusPrototyypit = hakemusPrototyypit;
        }
    }

    public final List<Result> results = new ArrayList<>();

    private static <T> Future<T> serviceAvailableCheck() {
        if(!serviceIsAvailable.get()) {
            return Futures.immediateFailedFuture(new RuntimeException("MockHakemuspalvelu on kytketty pois päältä!"));
        }
        return null;
    }


    public static void setAdditionalDataResult(List<ApplicationAdditionalDataDTO> result) {
        additionalDataResultReference.set(result);
    }

    public static void setAdditionalDataResultByOid(List<ApplicationAdditionalDataDTO> result) {
        additionalDataResultByOidReference.set(result);
    }

    public static List<ApplicationAdditionalDataDTO> getAdditionalDataInput() {
        return additionalDataPutReference.get();
    }

    public static void setResult(List<Hakemus> result) {
        resultReference.set(result);
    }

    public static void setResultByOid(List<Hakemus> result) {
        resultByOidReference.set(result);
    }

    public static void clear() {
        additionalDataResultReference.set(null);
        additionalDataPutReference.set(null);
        additionalDataResultByOidReference.set(null);
        resultByOidReference.set(null);
        ataruResultReference.set(null);
        resultReference.set(null);
    }

    @Override
    public Observable<List<AtaruHakemus>> getAtaruApplicationsByHakukohde(String hakukohdeOid) {
        return Observable.from(Optional.ofNullable(MockApplicationAsyncResource.<List<AtaruHakemus>>serviceAvailableCheck()).orElseGet(() -> {
            if (ataruResultReference.get() != null) {
                return Futures.immediateFuture(ataruResultReference.get());
            } else {
                AtaruHakemus hakemus = getAtaruHakemus();
                return Futures.immediateFuture(Arrays.asList(hakemus));
            }
        }));
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByHakemusOids(List<String> hakemusOids) {
        List<Hakemus> nonMatching = resultByOidReference.get().stream().filter(oid -> !hakemusOids.contains(oid.getOid())).collect(Collectors.toList());
        if (!nonMatching.isEmpty()) {
            throw new RuntimeException(String.format(
                    "Mock data contains OIDs %s not in query %s",
                    nonMatching.stream().map(h -> h.getOid()).collect(Collectors.toList()),
                    hakemusOids
            ));
        }
        return Observable.just(resultByOidReference.get());
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByhakemusOidsInParts(String hakuOid, List<String> hakemusOids, Collection<String> keys) {
        return Observable.just(resultReference.get());
    }

    @Override
    public Observable<List<Hakemus>> putApplicationPrototypes(final String hakuOid, final String hakukohdeOid, final String tarjoajaOid, final Collection<HakemusPrototyyppi> hakemusPrototyypit) {
        return Observable.from(Optional.ofNullable(MockApplicationAsyncResource.<List<Hakemus>>serviceAvailableCheck()).orElseGet(
                () -> {
                    results.add(new Result(hakuOid, hakukohdeOid, tarjoajaOid, hakemusPrototyypit));
                    return Futures.immediateFuture(hakemusPrototyypit.stream()
                                    .map(prototyyppi -> toHakemus(prototyyppi))
                                    .collect(Collectors.toList())
                    );
                }
        ));
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByOid(String hakuOid, String hakukohdeOid) {
        return getApplicationsByOids(hakuOid, Arrays.asList(hakukohdeOid));
    }

    @Override
    public Observable<Set<String>> getApplicationOids(String hakuOid, String hakukohdeOid) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByOids(String hakuOid, Collection<String> hakukohdeOids) {
        return Observable.from(Optional.ofNullable(MockApplicationAsyncResource.<List<Hakemus>>serviceAvailableCheck()).orElseGet(() -> {
            if (resultReference.get() != null) {
                return Futures.immediateFuture(resultReference.get());
            } else {
                Hakemus hakemus = getHakemus();
                return Futures.immediateFuture(Arrays.asList(hakemus));
            }
        }));
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByOidsWithPOST(String hakuOid, Collection<String> hakukohdeOids) {
        return Observable.from(Optional.ofNullable(MockApplicationAsyncResource.<List<Hakemus>>serviceAvailableCheck()).orElseGet(() -> {
            if (resultReference.get() != null) {
                return Futures.immediateFuture(resultReference.get());
            } else {
                Hakemus hakemus = new Hakemus();
                hakemus.setOid(MockData.hakemusOid);
                hakemus.setPersonOid(MockData.hakijaOid);
                Answers answers = new Answers();
                answers.getHenkilotiedot().put("Henkilotunnus", MockData.hetu);
                answers.getHenkilotiedot().put("Etunimet", MockData.etunimi);
                answers.getHenkilotiedot().put("Kutsumanimi", MockData.etunimi);
                answers.getHenkilotiedot().put("Sukunimi", MockData.sukunimi);
                answers.getHenkilotiedot().put("syntymaaika", MockData.syntymaAika);
                hakemus.setAnswers(answers);
                Eligibility e = new Eligibility(MockData.kohdeOid, null, null, MockData.maksuvelvollisuus);
                hakemus.getPreferenceEligibilities().add(e);
                return Futures.immediateFuture(Arrays.asList(hakemus));
            }
        }));
    }

    @Override
    public Observable<Hakemus> getApplication(String hakuOid) {
        return Observable.just(resultByOidReference.get().iterator().next());
    }

    @Override
    public Observable<Response> changeStateOfApplicationsToPassive(List<String> hakemusOid, String reason) {
        return Observable.just(Response.ok().build());
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByOids(final Collection<String> hakemusOids) {
        return Observable.just(resultByOidReference.get());
    }

    private AtaruHakemus getAtaruHakemus() {
        try {
            List<AtaruHakemus> hakemukset = new Gson().fromJson(IOUtils
                    .toString(new ClassPathResource("ataruhakemukset.json")
                            .getInputStream()), new TypeToken<List<AtaruHakemus>>() {}.getType());

            return hakemukset.stream()
                    .filter(h -> "1.2.246.562.11.00000000000000000063".equals(h.getHakemusOid()))
                    .distinct().iterator().next();
        } catch (Exception e) {
            System.err.println("Couldn't fetch mock ataru application");
            return new AtaruHakemus();
        }

    }

    private Hakemus getHakemus() {
        Hakemus hakemus = new Hakemus();
        hakemus.setOid(MockData.hakemusOid);
        hakemus.setPersonOid(MockData.hakijaOid);
        Answers answers = new Answers();
        answers.getHenkilotiedot().put("Henkilotunnus", MockData.hetu);
        answers.getHenkilotiedot().put("Etunimet", MockData.etunimi);
        answers.getHenkilotiedot().put("Kutsumanimi", MockData.etunimi);
        answers.getHenkilotiedot().put("Sukunimi", MockData.sukunimi);
        answers.getHenkilotiedot().put("syntymaaika", MockData.syntymaAika);
        hakemus.setAnswers(answers);
        return hakemus;
    }

    private Hakemus toHakemus(HakemusPrototyyppi prototyyppi) {
        final Hakemus hakemus = new Hakemus();
        hakemus.setAnswers(new Answers());
        final Map<String, String> henkilotiedot = hakemus.getAnswers().getHenkilotiedot();
        henkilotiedot.put("Henkilotunnus", prototyyppi.getHenkilotunnus());
        henkilotiedot.put("Etunimet", prototyyppi.getEtunimi());
        henkilotiedot.put("Kutsumanimi", prototyyppi.getEtunimi());
        henkilotiedot.put("Sukunimi", prototyyppi.getSukunimi());
        henkilotiedot.put("syntymaaika", prototyyppi.getSyntymaAika());
        hakemus.setOid(MockData.hakemusOid);
        hakemus.setPersonOid(prototyyppi.getHakijaOid());
        return hakemus;
    }
}
