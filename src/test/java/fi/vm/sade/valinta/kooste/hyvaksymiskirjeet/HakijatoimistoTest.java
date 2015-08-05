package fi.vm.sade.valinta.kooste.hyvaksymiskirjeet;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.valinta.kooste.Integraatiopalvelimet;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.impl.OrganisaatioAsyncResourceImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static javax.ws.rs.HttpMethod.*;

/**
 * @author Jussi Jartamo
 */
public class HakijatoimistoTest {

    @After
    public void reset() {
        Integraatiopalvelimet.mockServer.reset();
    }

    @Test
    public void testaaHakijatoimistonValinnaisuus() throws Throwable{
        final String EI_LOYDY_ORGANISAATIO_ID = "ei_loydy";
        final String LOYTYY_ORGANISAATIO_ID = "loytyy";
        Integraatiopalvelimet.mockToNotFound(GET, "/organisaatio/v2/" + EI_LOYDY_ORGANISAATIO_ID + "/hakutoimisto");
        Integraatiopalvelimet.mockToReturnJson(GET, "/organisaatio/v2/" + LOYTYY_ORGANISAATIO_ID + "/hakutoimisto", new HakutoimistoDTO(
                ImmutableMap.of("jee","jee"), Collections.emptyMap()
        ));

        OrganisaatioAsyncResourceImpl o = new OrganisaatioAsyncResourceImpl(Integraatiopalvelimet.mockServer.getUrl());
        final CyclicBarrier barrier = new CyclicBarrier(3);
        final Consumer<Action0> breakBarrier = r -> {
            try {
            r.call();
                } catch(Throwable t0) {
                  t0.printStackTrace();
                }
            finally {
                try {
                    barrier.await();
                } catch (Throwable t) {t.printStackTrace();}
            }
        };
        final AtomicReference<Optional<HakutoimistoDTO>> notFoundWasPresent = new AtomicReference<>();
        final AtomicBoolean notFoundHadErrors = new AtomicBoolean(false);
        final AtomicReference<Optional<HakutoimistoDTO>> foundWasPresent = new AtomicReference<>();
        final AtomicBoolean foundHadErrors = new AtomicBoolean(false);
        Observable<Optional<HakutoimistoDTO>> hakutoimistoNotFound= o.haeHakutoimisto(EI_LOYDY_ORGANISAATIO_ID);
        hakutoimistoNotFound
                .subscribe(
                        h -> breakBarrier.accept(() -> notFoundWasPresent.set(h)),
                        e -> breakBarrier.accept(() -> {e.printStackTrace();notFoundHadErrors.set(true);})
                );
        Observable<Optional<HakutoimistoDTO>> hakutoimistoFound= o.haeHakutoimisto(LOYTYY_ORGANISAATIO_ID);
        hakutoimistoFound
                .subscribe(
                        h -> breakBarrier.accept(() -> foundWasPresent.set(h)),
                        e -> breakBarrier.accept(() -> {e.printStackTrace();foundHadErrors.set(true);})
                );
        barrier.await(5L, TimeUnit.SECONDS);

        Assert.assertFalse("Should not fail on 200 result", foundHadErrors.get());
        Assert.assertTrue("Should not be empty with 200", foundWasPresent.get().isPresent());

        Assert.assertFalse("Should not fail on 404 result",notFoundHadErrors.get());
        Assert.assertTrue("Should be empty with 404", !notFoundWasPresent.get().isPresent());
    }


}
