package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.http.HttpResourceBuilder;
import fi.vm.sade.valinta.http.HttpResourceImpl;
import fi.vm.sade.valinta.kooste.erillishaku.resource.dto.Prosessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;

import java.time.Duration;
import java.util.function.Function;

public class DokumenttiProsessiPoller {
    private static final Duration TIME_TO_WAIT_IN_TOTAL = Duration.ofSeconds(30);
    private static final Duration REQUEST_INTERVAL = Duration.ofMillis(10);

    public static Prosessi pollDokumenttiProsessi(String rootUrl, ProsessiId prosessiId, Function<Prosessi,Boolean> responseProcessor) {
        final HttpResource dokumenttiProsessiResource = new HttpResourceBuilder().address(rootUrl + "/dokumenttiprosessi/" + prosessiId.getId()).build();
        long pollStarted = System.currentTimeMillis();
        sleepOneInterval(); // give the server some time to get started
        while (System.currentTimeMillis() < pollStarted + TIME_TO_WAIT_IN_TOTAL.toMillis()) {
            Prosessi prosessiStatusResponse = dokumenttiProsessiResource.getWebClient().get(Prosessi.class);
            if (responseProcessor.apply(prosessiStatusResponse)) {
                return prosessiStatusResponse;
            }
            sleepOneInterval();
        }
        throw new RuntimeException("Did not complete within " + TIME_TO_WAIT_IN_TOTAL.toMillis() + " ms.");
    }

    private static void sleepOneInterval() {
        try {
            Thread.sleep(REQUEST_INTERVAL.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
