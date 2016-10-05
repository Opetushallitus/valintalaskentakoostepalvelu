package fi.vm.sade.valinta.kooste.hakemukset.service;

import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AmmatillisenKielikoeMigrationService {
    private final ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource;
    private final AmmatillisenKielikoeMigrationPistesyottoService pistesyottoService;

    @Autowired
    public AmmatillisenKielikoeMigrationService(ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource,
                                                AmmatillisenKielikoeMigrationPistesyottoService pistesyottoService) {
        this.valintalaskentaValintakoeAsyncResource = valintalaskentaValintakoeAsyncResource;
        this.pistesyottoService = pistesyottoService;
    }

    public void migroiKielikoetuloksetSuoritusrekisteriin(Date since, String username, Consumer<Result> successHandler, BiConsumer<String, Throwable> exceptionHandler) {
        valintalaskentaValintakoeAsyncResource.haeAmmatillisenKielikokeenOsallistumiset(since).subscribe(valintakoeOsallistuminenDTOs -> {
            pistesyottoService.save(valintakoeOsallistuminenDTOs, new Result(since), successHandler, exceptionHandler, username);
        }, (exception -> exceptionHandler.accept("Virhe osallistumisia haettaessa", exception)));
    }

    private static final List<String> KIELIKOE_TUNNISTEET = Arrays.asList("kielikoe_fi", "kielikoe_sv");

    public static class Result {
        public final Date startingFrom;
        public final Map<Pair<String,Boolean>, Integer> applicationCounts;

        public Result(Date startingFrom) {
            this.startingFrom = startingFrom;
            applicationCounts = new HashMap<>();
            for (String t : KIELIKOE_TUNNISTEET) {
                applicationCounts.put(Pair.of(t, true), 0);
                applicationCounts.put(Pair.of(t, false), 0);
            }
        }

        public void add(String tunniste, boolean hyvaksytty) {
            Pair<String, Boolean> key = Pair.of(tunniste, hyvaksytty);
            Integer oldValue = applicationCounts.get(key);
            applicationCounts.put(key, oldValue + 1);
        }

        public Result plus(Result other) {
            Result sum = new Result(startingFrom);
            KIELIKOE_TUNNISTEET.forEach(tunniste -> {
                sum.applicationCounts.put(Pair.of(tunniste, false), falseCount(tunniste) + other.falseCount(tunniste));
                sum.applicationCounts.put(Pair.of(tunniste, true), trueCount(tunniste) + other.trueCount(tunniste));
            });
            return sum;
        }

        private Integer falseCount(String tunniste) {
            return applicationCounts.get(Pair.of(tunniste, false));
        }

        private Integer trueCount(String tunniste) {
            return applicationCounts.get(Pair.of(tunniste, true));
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
