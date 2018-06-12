package fi.vm.sade.valinta.kooste.hyvaksymiskirjeet;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.valinta.kooste.Integraatiopalvelimet;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl.ApplicationAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.impl.OrganisaatioAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static javax.ws.rs.HttpMethod.GET;

/**
 * @author Jussi Jartamo
 */
public class HakijatoimistoTest {

    @Before
    public void init() {
        startShared();
    }

    @Test
    public void testaaHyvaksymiskirjeetServicenLapi() {
        final String hakuOid = "haku";
        final String hakukohdeOid = "hakukohde";
        final String tarjoajaOid = "tarjoajaOid";
        Integraatiopalvelimet.mockToReturnJson(GET, "/sijoittelu-service/resources/sijoittelu/haku/hyvaksytyt/hakukohde/hakukohde", new HakijaPaginationObject());
        Integraatiopalvelimet.mockToReturnJson(GET, "/haku-app/applications/listfull", Arrays.asList());
        Integraatiopalvelimet.mockToNotFound(GET, "/organisaatio-service/rest/organisaatio/v2/" + tarjoajaOid + "/hakutoimisto");

        OrganisaatioAsyncResourceImpl o = new OrganisaatioAsyncResourceImpl();
        final String host= Integraatiopalvelimet.mockServer.getUrl();
        ApplicationAsyncResourceImpl a = new ApplicationAsyncResourceImpl(null);
        Observable<List<HakemusWrapper>> hakemuksetObservable = a.getApplicationsByOid(hakuOid, hakukohdeOid);
        //Observable<HakijaPaginationObject> hakijatFuture = s.getKoulutuspaikkalliset(hakuOid, hakukohdeOid);
        Observable<Optional<HakutoimistoDTO>> hakutoimistoObservable = o.haeHakutoimisto(tarjoajaOid);
        final Semaphore counter = new Semaphore(0);
        final AtomicReference<Optional<HakutoimistoDTO>> option = new AtomicReference<>();
        Observable.zip(
                hakemuksetObservable,
                //hakijatFuture,
                hakutoimistoObservable,
                (hakemukset, /*hakijat,*/ hakutoimisto) -> hakutoimisto
        ).subscribe(
                hakutoimisto -> {
                    option.set(hakutoimisto);
                    counter.release();
                },
                Throwable::printStackTrace
        );
        try {
            Assert.assertTrue(counter.tryAcquire(1, 10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Assert.fail();
        }
        Assert.assertEquals(Optional.empty(), option.get());
    }

    @Test
    public void testaaHakijatoimistonValinnaisuus() {
        final String EI_LOYDY_ORGANISAATIO_ID = "ei_loydy";
        final String HAKUKOHDE_OID = "hakukohdeOid";
        final String LOYTYY_ORGANISAATIO_ID = "loytyy";
        Integraatiopalvelimet.mockToNotFound(GET, "/organisaatio-service/rest/organisaatio/v2/" + EI_LOYDY_ORGANISAATIO_ID + "/hakutoimisto");
        Integraatiopalvelimet.mockToReturnJson(GET, "/organisaatio-service/rest/organisaatio/v2/" + LOYTYY_ORGANISAATIO_ID + "/hakutoimisto", new HakutoimistoDTO(
                ImmutableMap.of("jee","jee"), Collections.emptyMap()
        ));

        OrganisaatioAsyncResourceImpl o = new OrganisaatioAsyncResourceImpl();
        final Semaphore counter = new Semaphore(0);

        final AtomicReference<Optional<HakutoimistoDTO>> notFoundWasPresent = new AtomicReference<>();
        final AtomicBoolean notFoundHadErrors = new AtomicBoolean(false);
        final AtomicReference<Optional<HakutoimistoDTO>> foundWasPresent = new AtomicReference<>();
        final AtomicBoolean foundHadErrors = new AtomicBoolean(false);

        final AtomicReference<Map<String, Optional<Osoite>>> osoitePresent = new AtomicReference<>();
        final AtomicBoolean osoiteHadErrors = new AtomicBoolean(false);
        Observable<Optional<HakutoimistoDTO>> hakutoimistoNotFound= o.haeHakutoimisto(EI_LOYDY_ORGANISAATIO_ID);
        hakutoimistoNotFound
                .subscribe(
                        h -> {
                            counter.release();
                            notFoundWasPresent.set(h);
                        },
                        e -> notFoundHadErrors.set(true)
                );
        Observable<Optional<HakutoimistoDTO>> hakutoimistoFound= o.haeHakutoimisto(LOYTYY_ORGANISAATIO_ID);
        hakutoimistoFound
                .subscribe(
                        h -> {
                            counter.release();
                            foundWasPresent.set(h);
                        },
                        e -> foundHadErrors.set(true)
                );

        ViestintapalveluObservables.hakukohteenOsoite(
                HAKUKOHDE_OID,
                EI_LOYDY_ORGANISAATIO_ID,
                ImmutableMap.of(HAKUKOHDE_OID, new MetaHakukohde(EI_LOYDY_ORGANISAATIO_ID, new Teksti(HAKUKOHDE_OID),new Teksti(EI_LOYDY_ORGANISAATIO_ID))),
                o::haeHakutoimisto)
                .subscribe(
                        h -> {
                            counter.release();
                            osoitePresent.set(h);
                        },
                        e -> osoiteHadErrors.set(true)
                );

        try {
            Assert.assertTrue(counter.tryAcquire(3, 20, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Assert.fail();
        }

        Assert.assertFalse("Should not fail on 200 result", foundHadErrors.get());
        Assert.assertTrue("Should not be empty with 200", foundWasPresent.get().isPresent());

        Assert.assertFalse("Should not fail on 404 result",notFoundHadErrors.get());
        Assert.assertTrue("Should be empty with 404", !notFoundWasPresent.get().isPresent());
        Assert.assertEquals("Should be empty with 404", Optional.empty(),
                notFoundWasPresent.get().map(p -> Optional.ofNullable("WRONG")).orElse(Optional.empty()));

        Assert.assertTrue("Should not throw", !osoiteHadErrors.get());
        Assert.assertTrue("Should not have 'Osoite'", !osoitePresent.get().get(EI_LOYDY_ORGANISAATIO_ID).isPresent());


    }


}
