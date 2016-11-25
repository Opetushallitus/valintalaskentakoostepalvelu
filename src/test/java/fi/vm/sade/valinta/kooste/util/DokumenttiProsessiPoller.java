package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.erillishaku.resource.dto.Prosessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;

import java.time.Duration;
import java.util.function.Function;

public class DokumenttiProsessiPoller {
    private static final Duration TIME_TO_WAIT_IN_TOTAL = Duration.ofSeconds(30);
    private static final Duration REQUEST_INTERVAL = Duration.ofMillis(10);

    public static Prosessi pollDokumenttiProsessi(String rootUrl, ProsessiId prosessiId, Function<Prosessi,Boolean> responseProcessor) {
        final HttpResource dokumenttiProsessiResource = new HttpResource(rootUrl + "/dokumenttiprosessi/" + prosessiId.getId());
        long pollStarted = System.currentTimeMillis();
        while (System.currentTimeMillis() < pollStarted + TIME_TO_WAIT_IN_TOTAL.toMillis()) {
            Prosessi prosessiStatusResponse = dokumenttiProsessiResource.getWebClient().get(Prosessi.class);
            if (responseProcessor.apply(prosessiStatusResponse)) {
                return prosessiStatusResponse;
            }
            try {
                Thread.sleep(REQUEST_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Did not complete within " + TIME_TO_WAIT_IN_TOTAL.toMillis() + " ms.");
    }
}
