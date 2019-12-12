package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Eligibility;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import io.reactivex.Observable;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class MockApplicationAsyncResource implements ApplicationAsyncResource {
    public static AtomicBoolean serviceIsAvailable = new AtomicBoolean(true);

    private static AtomicReference<List<HakemusWrapper>> resultReference = new AtomicReference<>();
    private static AtomicReference<List<HakemusWrapper>> resultByOidReference = new AtomicReference<>();
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

    private static <T> CompletableFuture<T> serviceAvailableCheck() {
        if(!serviceIsAvailable.get()) {
            return CompletableFuture.failedFuture(new RuntimeException("MockHakemuspalvelu on kytketty pois päältä!"));
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

    public static void setResult(List<HakemusWrapper> result) {
        resultReference.set(result);
    }

    public static void setResultByOid(List<HakemusWrapper> result) {
        resultByOidReference.set(result);
    }

    public static void clear() {
        additionalDataResultReference.set(null);
        additionalDataPutReference.set(null);
        additionalDataResultByOidReference.set(null);
        resultByOidReference.set(null);
        resultReference.set(null);
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByHakemusOids(List<String> hakemusOids) {
        List<HakemusWrapper> nonMatching = resultByOidReference.get().stream().filter(oid -> !hakemusOids.contains(oid.getOid())).collect(Collectors.toList());
        if (!nonMatching.isEmpty()) {
            throw new RuntimeException(String.format(
                    "Mock data contains OIDs %s not in query %s",
                    nonMatching.stream().map(HakemusWrapper::getOid).collect(Collectors.toList()),
                    hakemusOids
            ));
        }
        return Observable.just(resultByOidReference.get());
    }

    @Override
    public CompletableFuture<List<HakemusWrapper>> getApplicationsByhakemusOidsInParts(String hakuOid, List<String> hakemusOids, List<String> keys) {
        return CompletableFuture.completedFuture(resultReference.get());
    }

    @Override
    public Observable<List<HakemusWrapper>> putApplicationPrototypes(final String hakuOid, final String hakukohdeOid, final String tarjoajaOid, final Collection<HakemusPrototyyppi> hakemusPrototyypit) {
        return Observable.fromFuture(Optional.ofNullable(MockApplicationAsyncResource.<List<HakemusWrapper>>serviceAvailableCheck()).orElseGet(
                () -> {
                    results.add(new Result(hakuOid, hakukohdeOid, tarjoajaOid, hakemusPrototyypit));
                    return CompletableFuture.completedFuture(hakemusPrototyypit.stream()
                                    .map(prototyyppi -> toHakemus(prototyyppi))
                                    .collect(Collectors.toList())
                    );
                }
        ));
    }

    @Override
    public CompletableFuture<List<HakemusWrapper>> getApplicationsByOid(String hakuOid, String hakukohdeOid) {
        return getApplicationsByOids(hakuOid, Arrays.asList(hakukohdeOid));
    }

    @Override
    public Observable<Set<String>> getApplicationOids(String hakuOid, String hakukohdeOid) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public CompletableFuture<List<HakemusWrapper>> getApplicationsByOids(String hakuOid, Collection<String> hakukohdeOids) {
        return Optional.ofNullable(MockApplicationAsyncResource.<List<HakemusWrapper>>serviceAvailableCheck()).orElseGet(() -> {
            if (resultReference.get() != null) {
                return CompletableFuture.completedFuture(resultReference.get());
            } else {
                HakemusWrapper hakemus = getHakemus();
                return CompletableFuture.completedFuture((Arrays.asList(hakemus)));
            }
        });
    }

    @Override
    public CompletableFuture<List<HakemusWrapper>> getApplicationsByOidsWithPOST(String hakuOid, List<String> hakukohdeOids) {
        if (serviceIsAvailable.get()) {
            if (resultReference.get() != null) {
                return CompletableFuture.completedFuture(resultReference.get());
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
                return CompletableFuture.completedFuture(Arrays.asList(new HakuappHakemusWrapper(hakemus)));
            }
        }
        return CompletableFuture.failedFuture(new RuntimeException("MockHakemuspalvelu on kytketty pois päältä!"));
    }

    @Override
    public Observable<HakemusWrapper> getApplication(String hakuOid) {
        return Observable.just(resultByOidReference.get().iterator().next());
    }

    @Override
    public Observable<Response> changeStateOfApplicationsToPassive(List<String> hakemusOid, String reason) {
        return Observable.just(Response.ok().build());
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByOids(final Collection<String> hakemusOids) {
        return Observable.just(resultByOidReference.get());
    }

    private HakemusWrapper getHakemus() {
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
        return new HakuappHakemusWrapper(hakemus);
    }

    private HakemusWrapper toHakemus(HakemusPrototyyppi prototyyppi) {
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
        return new HakuappHakemusWrapper(hakemus);
    }
}
