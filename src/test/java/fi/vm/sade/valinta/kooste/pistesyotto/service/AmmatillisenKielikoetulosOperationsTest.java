package fi.vm.sade.valinta.kooste.pistesyotto.service;

import static fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hylatty;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hyvaksytty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import com.github.npathai.hamcrestopt.OptionalMatchers;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvio;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.mocks.MockSuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.service.AbstractPistesyottoKoosteService.SingleKielikoeTulos;
import fi.vm.sade.valinta.kooste.pistesyotto.service.AmmatillisenKielikoetulosOperations.CompositeCommand;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana;
import fi.vm.sade.valinta.kooste.valintalaskenta.spec.SuoritusrekisteriSpec;
import org.hamcrest.Matchers;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class AmmatillisenKielikoetulosOperationsTest {
    private static final String HAKEMUS_OID_1 = "1.2.246.562.11.00000000001";
    private static final String HAKEMUS_OID_2 = "1.2.246.562.11.00000000002";
    private final String PERSON_OID_1 = "1.2.3.4.111";
    private final String PERSON_OID_2 = "1.2.3.4.222";
    private static final String KIELIKOE_AINE = "kielikoe";
    private static final String SOURCE_OID_1 = "1.2.246.562.10.45698499377";
    private static final String SOURCE_OID_2 = "1.2.246.562.10.45698499378";
    private static final String SOURCE_OID_3 = "1.2.246.562.10.45698499379";

    private final List<Oppija> oppijatSuresta = Arrays.asList(
        new SuoritusrekisteriSpec.OppijaBuilder().setOppijanumero(PERSON_OID_1)
            .suoritus().setId("123-123-123-2").setMyontaja(HAKEMUS_OID_1).setHenkiloOid(PERSON_OID_1).setKomo(AMMATILLISEN_KIELIKOE).setValmistuminen("6.4.2016")
                .arvosana().setAine(KIELIKOE_AINE).setLisatieto("FI").setArvosana(hyvaksytty.name()).setMyonnetty("6.4.2016").setSource(SOURCE_OID_1).build()
                .build()
            .suoritus().setId("123-123-123-3").setMyontaja(SOURCE_OID_2).setHenkiloOid(PERSON_OID_1).setKomo("1.2.3.4.5.6").setValmistuminen("5.1.2015")
                .arvosana().setAine("FY").setArvosana("7.0").setMyonnetty("5.1.2015").setSource(SOURCE_OID_2).build()
                .build()
            .build(),
        new SuoritusrekisteriSpec.OppijaBuilder().setOppijanumero(PERSON_OID_2)
            .suoritus().setId("123-123-123-4").setMyontaja(HAKEMUS_OID_2).setHenkiloOid(PERSON_OID_2).setKomo(AMMATILLISEN_KIELIKOE).setValmistuminen("1.9.2015")
                .arvosana().setAine(KIELIKOE_AINE).setLisatieto("SV").setArvosana(hylatty.name()).setMyonnetty("1.9.2015").setSource(SOURCE_OID_2).build()
                .build()
            .build());

    private final Map<String,List<SingleKielikoeTulos>> syotetytTulokset = new HashMap<>();
    private final Map<String,String> hakijaOidByHakemusOid = new HashMap<>();
    private Function<String, String> findPersonOidByHakemusOid = hakijaOidByHakemusOid::get;
    private List<Suoritus> postedSuoritukset = new LinkedList<>();
    private SuoritusSavingMockSuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = new SuoritusSavingMockSuoritusrekisteriAsyncResource();
    private final TestSubscriber<Arvosana> testSubscriber = new TestSubscriber<>();

    @Before
    public void populateTestData() {
        hakijaOidByHakemusOid.put(HAKEMUS_OID_1, PERSON_OID_1);
        hakijaOidByHakemusOid.put(HAKEMUS_OID_2, PERSON_OID_2);
        syotetytTulokset.put(HAKEMUS_OID_1, Collections.singletonList(new SingleKielikoeTulos("kielikoe_fi", hyvaksytty, new LocalDate(2016, 10, 26).toDate())));
        syotetytTulokset.put(HAKEMUS_OID_2, Arrays.asList(
            new SingleKielikoeTulos("kielikoe_fi", hyvaksytty, new LocalDate(2016, 10, 26).toDate()),
            new SingleKielikoeTulos("kielikoe_sv", hyvaksytty, new LocalDate(2016, 10, 26).toDate())
        ));
    }

    @Test
    public void existingResultsWithEqualArvosanaAreFilteredOut() {
        AmmatillisenKielikoetulosOperations source2Updates = new AmmatillisenKielikoetulosOperations(SOURCE_OID_2, oppijatSuresta, syotetytTulokset, findPersonOidByHakemusOid);
        assertThat(source2Updates.getResultsToSendToSure().entrySet(), hasSize(2));
        assertThat(source2Updates.getResultsToSendToSure(), hasKey(HAKEMUS_OID_2));
        assertThat(source2Updates.getResultsToSendToSure(), hasKey(HAKEMUS_OID_1));

        assertThat(source2Updates.getResultsToSendToSure().get(HAKEMUS_OID_1), OptionalMatchers.isEmpty());
        assertThat(source2Updates.getResultsToSendToSure().get(HAKEMUS_OID_2), OptionalMatchers.isPresent());
        Optional<CompositeCommand> personOid2Updates = source2Updates.getResultsToSendToSure().get(HAKEMUS_OID_2);

        personOid2Updates.get().createSureOperation(suoritusrekisteriAsyncResource).doOnError(Throwable::printStackTrace).subscribe(testSubscriber);

        testSubscriber.assertValueCount(2);
        List<Arvosana> receivedArvosanas = testSubscriber.getOnNextEvents();
        Assert.assertThat(receivedArvosanas, Matchers.hasSize(2));

        assertEquals(receivedArvosanas.get(0), (createArvosana("100", "kielikoe", "26.10.2016", SOURCE_OID_2, hyvaksytty, "FI")));

        assertEquals(receivedArvosanas.get(1), (createArvosana("100", "kielikoe", "1.9.2015", SOURCE_OID_2, hyvaksytty, "SV")));

        assertThat(postedSuoritukset, hasSize(1));
        Suoritus postedSuoritus = postedSuoritukset.get(0);
        assertNotEquals(HAKEMUS_OID_1, postedSuoritus.getMyontaja());
        assertEquals(HAKEMUS_OID_2, postedSuoritus.getMyontaja());
        assertEquals("100", postedSuoritus.getId());
        assertEquals(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE, postedSuoritus.getKomo());
        assertEquals(PERSON_OID_2, postedSuoritus.getHenkiloOid());
        assertEquals("26.10.2016", postedSuoritus.getValmistuminen());
    }

    @Test
    public void existingResultsWithDifferentArvosanaRetainGivenValmistuminen() {
        AmmatillisenKielikoetulosOperations source1Updates = new AmmatillisenKielikoetulosOperations(SOURCE_OID_1, oppijatSuresta, syotetytTulokset, findPersonOidByHakemusOid);
        assertThat(source1Updates.getResultsToSendToSure().entrySet(), hasSize(2));

        assertThat(source1Updates.getResultsToSendToSure(), hasKey(HAKEMUS_OID_1));
        assertThat(source1Updates.getResultsToSendToSure(), hasKey(HAKEMUS_OID_2));

        Optional<CompositeCommand> personOid2Updates = source1Updates.getResultsToSendToSure().get(HAKEMUS_OID_2);

        personOid2Updates.get().createSureOperation(suoritusrekisteriAsyncResource).doOnError(Throwable::printStackTrace).subscribe(testSubscriber);

        testSubscriber.assertValueCount(2);
        List<Arvosana> receivedArvosanas = testSubscriber.getOnNextEvents();


        assertEquals(receivedArvosanas.get(0), createArvosana("101", "kielikoe", "26.10.2016", SOURCE_OID_1, hyvaksytty, "FI"));
        assertEquals(receivedArvosanas.get(1), createArvosana("101", "kielikoe", "1.9.2015", SOURCE_OID_2, hyvaksytty, "SV"));

        assertThat(postedSuoritukset, hasSize(1));
        Suoritus postedSuoritus = postedSuoritukset.get(0);
        assertNotEquals(HAKEMUS_OID_1, postedSuoritus.getMyontaja());
        assertEquals(HAKEMUS_OID_2, postedSuoritus.getMyontaja());
        assertEquals("101", postedSuoritus.getId());
        assertEquals(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE, postedSuoritus.getKomo());
        assertEquals(PERSON_OID_2, postedSuoritus.getHenkiloOid());
        assertEquals("26.10.2016", postedSuoritus.getValmistuminen());
    }

    @Test
    public void newResultsRetainGivenValmistuminen() {
        AmmatillisenKielikoetulosOperations source3Updates = new AmmatillisenKielikoetulosOperations(SOURCE_OID_3, oppijatSuresta, syotetytTulokset, findPersonOidByHakemusOid);
        assertThat(source3Updates.getResultsToSendToSure().entrySet(), hasSize(2));

        assertThat(source3Updates.getResultsToSendToSure(), hasKey(HAKEMUS_OID_1));
        assertThat(source3Updates.getResultsToSendToSure(), hasKey(HAKEMUS_OID_2));

        Optional<CompositeCommand> personOid2Updates = source3Updates.getResultsToSendToSure().get(HAKEMUS_OID_2);

        personOid2Updates.get().createSureOperation(suoritusrekisteriAsyncResource).doOnError(Throwable::printStackTrace).subscribe(testSubscriber);

        testSubscriber.assertValueCount(2);
        List<Arvosana> receivedArvosanas = testSubscriber.getOnNextEvents();

        assertEquals(receivedArvosanas.get(0), createArvosana("102", "kielikoe", "26.10.2016", SOURCE_OID_3, hyvaksytty, "FI"));
        assertEquals(receivedArvosanas.get(1), createArvosana("102", "kielikoe", "1.9.2015", SOURCE_OID_2, hyvaksytty, "SV"));

        assertThat(postedSuoritukset, hasSize(1));
        Suoritus postedSuoritus = postedSuoritukset.get(0);
        assertNotEquals(HAKEMUS_OID_1, postedSuoritus.getMyontaja());
        assertEquals(HAKEMUS_OID_2, postedSuoritus.getMyontaja());
        assertEquals("102", postedSuoritus.getId());
        assertEquals(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE, postedSuoritus.getKomo());
        assertEquals(PERSON_OID_2, postedSuoritus.getHenkiloOid());
        assertEquals("26.10.2016", postedSuoritus.getValmistuminen());
    }

    private Arvosana createArvosana(String suoritusId, String aine, String myonnetty, String sourceOid, SureHyvaksyttyArvosana arvosana, String lisatieto) {
        Arvio arvio = new Arvio();
        arvio.setAsteikko("HYVAKSYTTY");
        arvio.setArvosana(arvosana.name());
        return new Arvosana(null, suoritusId, aine, false, myonnetty, sourceOid, Collections.emptyMap(), arvio, lisatieto);
    }

    private class SuoritusSavingMockSuoritusrekisteriAsyncResource extends MockSuoritusrekisteriAsyncResource {
        @Override
        public Observable<Suoritus> postSuoritus(Suoritus suoritus) {
            postedSuoritukset.add(suoritus);
            return super.postSuoritus(suoritus);
        }
    }
}