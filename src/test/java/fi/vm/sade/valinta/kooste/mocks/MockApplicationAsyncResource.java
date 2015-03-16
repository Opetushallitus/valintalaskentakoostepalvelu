package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.util.concurrent.Futures;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class MockApplicationAsyncResource implements ApplicationAsyncResource {
    public static AtomicBoolean serviceIsAvailable = new AtomicBoolean(true);

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
    private static AtomicReference<List<Hakemus>> resultReference = new AtomicReference<>();

    public static void setResult(List<Hakemus> result) {
        resultReference.set(result);
    }
    public static void clear() {
        resultReference.set(null);
    }

    @Override
    public Peruutettava getApplicationsByOid(final String hakuOid, final String hakukohdeOid, final Consumer<List<Hakemus>> callback, final Consumer<Throwable> failureCallback) {
        callback.accept(resultReference.get());
        return new PeruutettavaImpl(Futures.immediateCancelledFuture());
    }

    @Override
    public Future<List<Hakemus>> putApplicationPrototypes(final String hakuOid, final String hakukohdeOid, final String tarjoajaOid, final Collection<HakemusPrototyyppi> hakemusPrototyypit) {
        return Optional.ofNullable(MockApplicationAsyncResource.<List<Hakemus>>serviceAvailableCheck()).orElseGet(
                () -> {
                    results.add(new Result(hakuOid, hakukohdeOid, tarjoajaOid, hakemusPrototyypit));
                    return Futures.immediateFuture(hakemusPrototyypit.stream()
                                    .map(prototyyppi -> toHakemus(prototyyppi))
                                    .collect(Collectors.toList())
                    );
                }
        );
    }
    private Hakemus toHakemus(HakemusPrototyyppi prototyyppi) {
        final Hakemus hakemus = new Hakemus();
        hakemus.setAnswers(new Answers());
        final Map<String, String> henkilotiedot = hakemus.getAnswers().getHenkilotiedot();
        henkilotiedot.put("Henkilotunnus", prototyyppi.henkilotunnus);
        henkilotiedot.put("Etunimet", prototyyppi.etunimi);
        henkilotiedot.put("Kutsumanimi", prototyyppi.etunimi);
        henkilotiedot.put("Sukunimi", prototyyppi.sukunimi);
        henkilotiedot.put("syntymaaika", prototyyppi.syntymaAika);
        hakemus.setOid(MockData.hakemusOid);
        hakemus.setPersonOid(prototyyppi.hakijaOid);
        return hakemus;
    }
    @Override
    public Future<List<ApplicationAdditionalDataDTO>> getApplicationAdditionalData(final String hakuOid, final String hakukohdeOid) {
        throw new UnsupportedOperationException();
    }
    @Override
    public Future<List<Hakemus>> getApplicationsByOid(final String hakuOid, final String hakukohdeOid) {
        return Optional.ofNullable(MockApplicationAsyncResource.<List<Hakemus>>serviceAvailableCheck()).orElseGet(
                () -> {
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
                    return Futures.immediateFuture(Arrays.asList(hakemus));
                }
        );
    }
    @Override
    public Future<List<Hakemus>> getApplicationsByOids(final Collection<String> hakemusOids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Peruutettava getApplicationAdditionalData(final String hakuOid, final String hakukohdeOid, final Consumer<List<ApplicationAdditionalDataDTO>> callback, final Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }
}
