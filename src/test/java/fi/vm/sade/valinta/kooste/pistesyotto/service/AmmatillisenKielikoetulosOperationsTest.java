package fi.vm.sade.valinta.kooste.pistesyotto.service;

import static fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hylatty;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hyvaksytty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.pistesyotto.service.AbstractPistesyottoKoosteService.SingleKielikoeTulos;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana;
import fi.vm.sade.valinta.kooste.valintalaskenta.spec.SuoritusrekisteriSpec;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            .suoritus().setId("123-123-123-1").setMyontaja(SOURCE_OID_1).setHenkiloOid(PERSON_OID_1).setKomo(AMMATILLISEN_KIELIKOE).setValmistuminen("2015-09-01")
                .arvosana().setAine(KIELIKOE_AINE).setLisatieto("FI").setArvosana(hylatty.name()).setMyonnetty("2015-09-01").build()
                .build()
            .suoritus().setId("123-123-123-2").setMyontaja(SOURCE_OID_2).setHenkiloOid(PERSON_OID_1).setKomo(AMMATILLISEN_KIELIKOE).setValmistuminen("2016-04-06")
                .arvosana().setAine(KIELIKOE_AINE).setLisatieto("FI").setArvosana(hyvaksytty.name()).setMyonnetty("2016-04-06").build()
                .build()
            .suoritus().setId("123-123-123-3").setMyontaja(SOURCE_OID_2).setHenkiloOid(PERSON_OID_1).setKomo("1.2.3.4.5.6").setValmistuminen("2015-01-05")
                .arvosana().setAine("FY").setArvosana("7.0").setMyonnetty("2015-01-05").build()
                .build()
            .build(),
        new SuoritusrekisteriSpec.OppijaBuilder().setOppijanumero(PERSON_OID_2)
            .suoritus().setId("123-123-123-4").setMyontaja(SOURCE_OID_1).setHenkiloOid(PERSON_OID_2).setKomo(AMMATILLISEN_KIELIKOE).setValmistuminen("2015-09-01")
                .arvosana().setAine(KIELIKOE_AINE).setLisatieto("SV").setArvosana(hylatty.name()).setMyonnetty("2015-09-01").build()
                .build()
            .suoritus().setId("123-123-123-5").setMyontaja(SOURCE_OID_2).setHenkiloOid(PERSON_OID_2).setKomo(AMMATILLISEN_KIELIKOE).setValmistuminen("2016-04-06")
                .arvosana().setAine(KIELIKOE_AINE).setLisatieto("SV").setArvosana(hylatty.name()).setMyonnetty("2016-04-06").build()
                .build()
            .build());

    private final Map<String,List<SingleKielikoeTulos>> syotetytTulokset = new HashMap<>();
    private final Map<String,String> hakijaOidByHakemusOid = new HashMap<>();
    private Function<String, String> findPersonOidByHakemusOid = hakijaOidByHakemusOid::get;

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
        assertThat(source2Updates.getResultsToSendToSure().entrySet(), hasSize(1));

        assertThat(source2Updates.getResultsToSendToSure(), hasKey(HAKEMUS_OID_2));
        List<SingleKielikoeTulos> personOid2Updates = source2Updates.getResultsToSendToSure().get(HAKEMUS_OID_2);
        assertThat(personOid2Updates, hasSize(2));
        assertThat(personOid2Updates, hasItem(withValues("kielikoe_fi", hyvaksytty, new LocalDate(2016, 10, 26))));
        assertThat(personOid2Updates, hasItem(withValues("kielikoe_sv", hyvaksytty, new LocalDate(2016, 10, 26))));
    }

    @Test
    public void existingResultsWithDifferentArvosanaRetainGivenValmistuminen() {
        AmmatillisenKielikoetulosOperations source1Updates = new AmmatillisenKielikoetulosOperations(SOURCE_OID_1, oppijatSuresta, syotetytTulokset, findPersonOidByHakemusOid);
        assertThat(source1Updates.getResultsToSendToSure().entrySet(), hasSize(2));

        assertThat(source1Updates.getResultsToSendToSure(), hasKey(HAKEMUS_OID_1));
        assertThat(source1Updates.getResultsToSendToSure(), hasKey(HAKEMUS_OID_2));

        List<SingleKielikoeTulos> personOid1Updates = source1Updates.getResultsToSendToSure().get(HAKEMUS_OID_1);
        assertThat(personOid1Updates, hasSize(1));
        assertThat(personOid1Updates, hasItem(withValues("kielikoe_fi", hyvaksytty, new LocalDate(2016, 10, 26))));

        List<SingleKielikoeTulos> personOid2Updates = source1Updates.getResultsToSendToSure().get(HAKEMUS_OID_2);
        assertThat(personOid2Updates, hasSize(2));
        assertThat(personOid2Updates, hasItem(withValues("kielikoe_fi", hyvaksytty, new LocalDate(2016, 10, 26))));
        assertThat(personOid2Updates, hasItem(withValues("kielikoe_sv", hyvaksytty, new LocalDate(2016, 10, 26))));
    }

    @Test
    public void newResultsRetainGivenValmistuminen() {
        AmmatillisenKielikoetulosOperations source3Updates = new AmmatillisenKielikoetulosOperations(SOURCE_OID_3, oppijatSuresta, syotetytTulokset, findPersonOidByHakemusOid);
        assertThat(source3Updates.getResultsToSendToSure().entrySet(), hasSize(2));
        assertEquals(syotetytTulokset, source3Updates.getResultsToSendToSure());
    }

    private Matcher<SingleKielikoeTulos> withValues(String kokeenTunnus, SureHyvaksyttyArvosana arvosana, LocalDate valmistuminen) {
        return new BaseMatcher<SingleKielikoeTulos>() {
            @Override
            public boolean matches(Object item) {
                if (!(item instanceof SingleKielikoeTulos)) {
                    return false;
                }
                SingleKielikoeTulos kielikoeTulos = (SingleKielikoeTulos) item;
                return kokeenTunnus.equals(kielikoeTulos.kokeenTunnus)
                    && arvosana.equals(kielikoeTulos.arvioArvosana)
                    && valmistuminen.toDate().equals(kielikoeTulos.valmistuminen);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with kokeenTunnus == " + kokeenTunnus + " , arvosana == " + arvosana + " , valmistuminen == " + valmistuminen);
            }
        };
    }
}
