package fi.vm.sade.valinta.kooste.erillishaku.excel.mocks;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import jersey.repackaged.com.google.common.util.concurrent.Futures;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MockApplicationAsyncResource implements ApplicationAsyncResource {
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
    @Override
    public Future<List<Hakemus>> putApplicationPrototypes(final String hakuOid, final String hakukohdeOid, final String tarjoajaOid, final Collection<HakemusPrototyyppi> hakemusPrototyypit) {
        results.add(new Result(hakuOid, hakukohdeOid, tarjoajaOid, hakemusPrototyypit));
        return Futures.immediateFuture(hakemusPrototyypit.stream()
                        .map(prototyyppi -> toHakemus(prototyyppi))
                        .collect(Collectors.toList())
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
        hakemus.setOid("hakemus1");
        hakemus.setPersonOid(prototyyppi.hakijaOid);
        return hakemus;
    }
    @Override
    public Future<List<ApplicationAdditionalDataDTO>> getApplicationAdditionalData(final String hakuOid, final String hakukohdeOid) {
        throw new UnsupportedOperationException();
    }
    @Override
    public Future<List<Hakemus>> getApplicationsByOid(final String hakuOid, final String hakukohdeOid) {
        Hakemus hakemus = new Hakemus();
        hakemus.setOid("hakemus1");
        hakemus.setPersonOid("hakija1");
        Answers answers = new Answers();
        answers.getHenkilotiedot().put("Henkilotunnus", "hetu");
        hakemus.setAnswers(answers);
        return Futures.immediateFuture(Arrays.asList(hakemus));
    }
    @Override
    public Future<List<Hakemus>> getApplicationsByOids(final Collection<String> hakemusOids) {
        throw new UnsupportedOperationException();
    }
    @Override
    public Peruutettava getApplicationsByOid(final String hakuOid, final String hakukohdeOid, final Consumer<List<Hakemus>> callback, final Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }
    @Override
    public Peruutettava getApplicationAdditionalData(final String hakuOid, final String hakukohdeOid, final Consumer<List<ApplicationAdditionalDataDTO>> callback, final Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }
}
