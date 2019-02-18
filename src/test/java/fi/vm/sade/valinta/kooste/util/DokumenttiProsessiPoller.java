package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.erillishaku.resource.dto.Prosessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Function;

public class DokumenttiProsessiPoller {
    private static final Duration TIME_TO_WAIT_IN_TOTAL = Duration.ofSeconds(150);
    private static final Duration REQUEST_INTERVAL = Duration.ofMillis(100);
    private static Logger LOG = LoggerFactory.getLogger(DokumenttiProsessiPoller.class);

    public static Prosessi pollDokumenttiProsessi(String rootUrl, ProsessiId prosessiId, Function<Prosessi,Boolean> responseProcessor) {
        final HttpResourceBuilder.WebClientExposingHttpResource dokumenttiProsessiResource = new HttpResourceBuilder()
            .address(rootUrl + "/dokumenttiprosessi/" + prosessiId.getId())
            .buildExposingWebClientDangerously();
        long pollStarted = System.currentTimeMillis();
        sleep(REQUEST_INTERVAL.multipliedBy(8)); // give the server some time to get started
        while (System.currentTimeMillis() < pollStarted + TIME_TO_WAIT_IN_TOTAL.toMillis()) {
            Prosessi prosessiStatusResponse = dokumenttiProsessiResource.getWebClient().get(Prosessi.class);
            LOG.info("prosessiStatusResponse = " + prosessiStatusResponse);
            if (responseProcessor.apply(prosessiStatusResponse)) {
                return prosessiStatusResponse;
            }
            sleepOneInterval();
        }
        throw new RuntimeException("Did not complete within " + TIME_TO_WAIT_IN_TOTAL.toMillis() + " ms (" + TIME_TO_WAIT_IN_TOTAL + ").");
    }

    private static void sleepOneInterval() {
        sleep(REQUEST_INTERVAL);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
