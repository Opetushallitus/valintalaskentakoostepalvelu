package fi.vm.sade.valinta.kooste.hakemukset.service;

import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.ei_osallistunut;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hylatty;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hyvaksytty;

import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana;
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

    public static final List<String> KIELIKOE_TUNNISTEET = Arrays.asList("kielikoe_fi", "kielikoe_sv");

    @Autowired
    public AmmatillisenKielikoeMigrationService(ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource,
                                                AmmatillisenKielikoeMigrationPistesyottoService pistesyottoService) {
        this.valintalaskentaValintakoeAsyncResource = valintalaskentaValintakoeAsyncResource;
        this.pistesyottoService = pistesyottoService;
    }

    public void migroiKielikoetuloksetSuoritusrekisteriin(Date since, String username, Consumer<Result> successHandler, BiConsumer<String, Throwable> exceptionHandler) {
        valintalaskentaValintakoeAsyncResource.haeAmmatillisenKielikokeenOsallistumiset(since).subscribe(valintakoeOsallistuminenDTOs -> {
            Result initialResult = new Result(since);
            if (valintakoeOsallistuminenDTOs.isEmpty()) {
                successHandler.accept(initialResult);
            }
            pistesyottoService.save(valintakoeOsallistuminenDTOs, initialResult, successHandler, exceptionHandler, username);
        }, (exception -> exceptionHandler.accept("Virhe osallistumisia haettaessa", exception)));
    }

    public static class Result {
        public final Date startingFrom;
        public final Map<Pair<String,SureHyvaksyttyArvosana>, Integer> applicationCounts;

        public Result(Date startingFrom) {
            this.startingFrom = startingFrom;
            applicationCounts = new HashMap<>();
            for (String t : KIELIKOE_TUNNISTEET) {
                applicationCounts.put(Pair.of(t, hyvaksytty), 0);
                applicationCounts.put(Pair.of(t, hylatty), 0);
                applicationCounts.put(Pair.of(t, ei_osallistunut), 0);
            }
        }

        public void add(String tunniste, SureHyvaksyttyArvosana arvosana) {
            Pair<String, SureHyvaksyttyArvosana> key = Pair.of(tunniste, arvosana);
            Integer oldValue = applicationCounts.get(key);
            applicationCounts.put(key, oldValue + 1);
        }

        public Result plus(Result other) {
            Result sum = new Result(startingFrom);
            KIELIKOE_TUNNISTEET.forEach(tunniste -> {
                sum.applicationCounts.put(Pair.of(tunniste, hyvaksytty), trueCount(tunniste) + other.trueCount(tunniste));
                sum.applicationCounts.put(Pair.of(tunniste, hylatty), falseCount(tunniste) + other.falseCount(tunniste));
                sum.applicationCounts.put(Pair.of(tunniste, ei_osallistunut), eiOsallistunutCount(tunniste) + other.eiOsallistunutCount(tunniste));
            });
            return sum;
        }

        private Integer eiOsallistunutCount(String tunniste) {
            return applicationCounts.get(Pair.of(tunniste, ei_osallistunut));
        }

        private Integer falseCount(String tunniste) {
            return applicationCounts.get(Pair.of(tunniste, hylatty));
        }

        private Integer trueCount(String tunniste) {
            return applicationCounts.get(Pair.of(tunniste, hyvaksytty));
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
