package fi.vm.sade.valinta.kooste.valintalaskenta;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;
import fi.vm.sade.valinta.kooste.util.sure.ArvosanaToAvainArvoDTOConverter;
import fi.vm.sade.valinta.kooste.util.sure.ArvosanaToAvainArvoDTOConverter.OppiaineArvosana;
import fi.vm.sade.valinta.kooste.util.sure.YoToAvainSuoritustietoDTOConverter;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.AvainMetatiedotDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class SureKonvertointiTest {
	private static final Logger LOG = LoggerFactory
			.getLogger(SureKonvertointiTest.class);

	@Test
	public void testaaLukioKonvertointi() throws JsonSyntaxException,
			IOException {
		Oppija o = new Gson().fromJson(IOUtils
				.toString(new ClassPathResource("valintalaskenta/sure_pk_ja_yo.json")
						.getInputStream()), new TypeToken<Oppija>() {
		}.getType());
		SuoritusJaArvosanat pk = o.getSuoritukset().stream().filter(s -> new SuoritusJaArvosanatWrapper(s).isPerusopetus()).findFirst().get();
		SuoritusJaArvosanat yo = o.getSuoritukset().stream().filter(s -> new SuoritusJaArvosanatWrapper(s).isYoTutkinto()).findFirst().get();


		ParametritDTO pmetrit = new ParametritDTO();
		ParametriDTO ph_vls = new ParametriDTO();
		ph_vls.setDateStart(DateTime.now().minusDays(1).toDate()); // LASKENTA ALKANUT PAIVA SITTEN
		pmetrit.setPH_VLS(ph_vls);

		List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(o.getOppijanumero(), o.getSuoritukset(), new HakemusDTO(), pmetrit);

		String a1ESAinetunniste = aa.stream()
				.filter((AvainArvoDTO a) -> a.getAvain().startsWith("PK_A1") && "ES".equals(a.getArvo()))
				.findAny().get()
				.getAvain().replace("_OPPIAINE", "");
		Assert.assertEquals("5",
				aa.stream().filter(a -> a1ESAinetunniste.equals(a.getAvain())).findAny().get().getArvo());
		Assert.assertEquals("6",
				aa.stream().filter(a -> (a1ESAinetunniste + "_VAL1").equals(a.getAvain())).findAny().get().getArvo());

		Assert.assertEquals("HI:lla on lisäksi kaksi valinnaista", 3L,
				aa.stream().filter(a -> a.getAvain().startsWith("PK_HI")).count());

		Assert.assertTrue("PK_A12_VAL1 löytyy",
				aa.stream().filter(a -> a.getAvain().equals("PK_A12_VAL1")).count() == 1L);
	}

    @Test
    public void testaaSureKonvertointiKaikkiAIneet() throws JsonSyntaxException,
            IOException {
        List<Oppija> oppijat = new Gson().fromJson(IOUtils
                .toString(new ClassPathResource("sure_kaikki_aineet.json")
                        .getInputStream()), new TypeToken<List<Oppija>>() {
        }.getType());

        Oppija oppija = oppijat.get(0);

        Assert.assertTrue(oppija.getSuoritukset().get(0).getSuoritus().isVahvistettu());

        final List<AvainMetatiedotDTO> konvertoitu = YoToAvainSuoritustietoDTOConverter.convert(oppija);

        final List<String> aineet = Arrays.asList("UE", "UO", "ET", "FF", "PS",
                "HI", "FY", "KE", "BI", "GE", "TE", "YH",
                "REAALI", "AINEREAALI", "PITKA_KIELI", "KESKIPITKA_KIELI", "LYHYT_KIELI", "AIDINKIELI",
                "O", "O5", "A", "A5", "BA", "BB", "CA", "CB", "CC", "DC", "EA", "EB", "EC", "FA", "FB",
                "FC", "GA", "GB", "GC", "HA", "HB", "I", "W", "IC", "QC", "J", "KC", "L1", "L7", "M", "N",
                "PA", "PB", "PC", "RR", "RO", "RY", "SA", "SB", "SC", "S9", "TA", "TB", "TC", "VA", "VB",
                "VC", "Z", "UE", "UO", "ET", "FF", "PS", "HI", "FY", "KE", "BI", "GE", "TE", "YH");


        aineet.forEach(a -> {
            final List values = konvertoitu.stream().filter(k -> k.getAvain().equals(a)).collect(Collectors.toList());
            assertEquals("values for " + a + ": " + values, 1, values.size());
        });

        final List<Map<String,String>> ainereaali = konvertoitu.stream().filter(k -> k.getAvain().equals("AINEREAALI")).findFirst().get().getMetatiedot();
        assertEquals(12, ainereaali.size());

        final List<Map<String,String>>  reaali = konvertoitu.stream().filter(k -> k.getAvain().equals("REAALI")).findFirst().get().getMetatiedot();
        assertEquals(3, reaali.size());
        assertEquals("ET", reaali.stream().filter(m -> m.get("LISATIETO").equals("ET")).findFirst().get().get("ROOLI"));

        final List<Map<String,String>>  pitka_kieli = konvertoitu.stream().filter(k -> k.getAvain().equals("PITKA_KIELI")).findFirst().get().getMetatiedot();
        assertEquals(9, pitka_kieli.size());

        final List<Map<String,String>>  keskipitka_kieli = konvertoitu.stream().filter(k -> k.getAvain().equals("KESKIPITKA_KIELI")).findFirst().get().getMetatiedot();
        assertEquals(8, keskipitka_kieli.size());

        final List<Map<String,String>>  lyhyt_kieli = konvertoitu.stream().filter(k -> k.getAvain().equals("LYHYT_KIELI")).findFirst().get().getMetatiedot();
        assertEquals(10, lyhyt_kieli.size());

        final List<Map<String,String>>  aidinkieli = konvertoitu.stream().filter(k -> k.getAvain().equals("AIDINKIELI")).findFirst().get().getMetatiedot();
        assertEquals(7, aidinkieli.size());

    }

    @Test
    public void testaaSureKonvertointiKoetunnus() throws JsonSyntaxException,
            IOException {
        List<Oppija> oppijat = new Gson().fromJson(IOUtils
                .toString(new ClassPathResource("valintalaskenta/sure_yo_koetunnus.json")
                        .getInputStream()), new TypeToken<List<Oppija>>() {
        }.getType());

        Oppija oppija = oppijat.get(0);

        final List<AvainMetatiedotDTO> konvertoitu = YoToAvainSuoritustietoDTOConverter.convert(oppija);

        AvainMetatiedotDTO va = konvertoitu.stream().filter(k -> k.getAvain().equals("VA")).findFirst().get();
        assertNotNull(va);

        assertEquals("21", va.getMetatiedot().stream().filter(m -> m.containsKey("ROOLI")).findFirst().get().get("ROOLI"));

    }

	@Test
	public void testMontaKertaaSamaKielikoodiYhdistetaan() throws IOException {
		List<Oppija> oppijat = new Gson().fromJson(IOUtils
				.toString(new ClassPathResource("monta_kielta.json")
						.getInputStream()), new TypeToken<List<Oppija>>() {
		}.getType());

		Oppija o = oppijat.get(0);

		HakemusDTO hakemus = new HakemusDTO();
		hakemus.setHakemusoid("1.2.246.562.11.00000000001");
		List<AvainArvoDTO> arvot = OppijaToAvainArvoDTOConverter.convert(o.getOppijanumero(), o.getSuoritukset(), hakemus, null);
<<<<<<< HEAD

		assertEquals(ImmutableSet.of(
				new AvainArvoDTO("PK_B1", "10"), new AvainArvoDTO("PK_B1_OPPIAINE", "SV"),
				new AvainArvoDTO("PK_B13", "10"), new AvainArvoDTO("PK_B13_OPPIAINE", "EN")
		), ImmutableSet.copyOf(arvot));
=======

		Assert.assertEquals(4, arvot.size());
		String b1SVAinetunniste = arvot.stream()
				.filter(a -> a.getAvain().startsWith("PK_B1") && "SV".equals(a.getArvo()))
				.findAny().get()
				.getAvain().replace("_OPPIAINE", "");
		String b1ENAinetunniste = arvot.stream()
				.filter(a -> a.getAvain().startsWith("PK_B1") && "EN".equals(a.getArvo()))
				.findAny().get()
				.getAvain().replace("_OPPIAINE", "");
		assertEquals("9",
				arvot.stream().filter(a -> b1SVAinetunniste.equals(a.getAvain())).findAny().get().getArvo());
		assertEquals("10",
				arvot.stream().filter(a -> b1ENAinetunniste.equals(a.getAvain())).findAny().get().getArvo());
	}

	@Test
	public void testSuoritusmerkintaHuononpiKuinNumeerinen() {
		OppiaineArvosana a = ArvosanaToAvainArvoDTOConverter.parasArvosana(ImmutableList.of(
				new OppiaineArvosana("", "", false, 1, "4", "4-10"),
				new OppiaineArvosana("", "", false, 1, "S", "4-10")
		));
		assertEquals("4", a.arvosana);
	}

	@Test(expected = RuntimeException.class)
	public void testEriAsteikkojenArvosanojaEiVertailla() {
		OppiaineArvosana a = ArvosanaToAvainArvoDTOConverter.parasArvosana(ImmutableList.of(
				new OppiaineArvosana("", "", false, 1, "4", "4-10"),
				new OppiaineArvosana("", "", false, 1, "4", "0-5")
		));
	}

	@Test
	public void testParasArvosanaNumeerisestiIsoin() {
		OppiaineArvosana a = ArvosanaToAvainArvoDTOConverter.parasArvosana(ImmutableList.of(
				new OppiaineArvosana("", "", false, 1, "4", "4-10"),
				new OppiaineArvosana("", "", false, 1, "6", "4-10"),
				new OppiaineArvosana("", "", false, 1, "5", "4-10")
		));
		assertEquals("6", a.arvosana);
>>>>>>> 0cd2f2ce43a57adda5194d399a4f093f6e59e06a
	}

	@Test
	public void testOppiainenumeroKorvataanOppiaineenNimellä() {
		OppiaineArvosana a = new OppiaineArvosana(new Arvosana("testid", "testsuoritus", "B12", false, "testmyönnetty",
				"testsource", new HashMap<>(), new Arvio(), "EN"));
		OppiaineArvosana b = new OppiaineArvosana(new Arvosana("testid", "testsuoritus", "B1", false, "testmyönnetty",
				"testsource", new HashMap<>(), new Arvio(), "SV"));
		assertEquals("B1EN", a.aine);
		assertEquals("B1SV", b.aine);
	}

	@Test
	public void testOppiainenumeroinninPalautusAntaaSamanNumeronSamanAineenArvosanoille() {
		OppiaineArvosana a = new OppiaineArvosana(new Arvosana("testid", "testsuoritus", "B1", true, "testmyönnetty",
				"testsource", new HashMap<>(), new Arvio(), "EN"));
		OppiaineArvosana b = new OppiaineArvosana(new Arvosana("testid", "testsuoritus", "B12", true, "testmyönnetty",
				"testsource", new HashMap<>(), new Arvio(), "EN"));
		OppiaineArvosana c = new OppiaineArvosana(new Arvosana("testid", "testsuoritus", "B13", true, "testmyönnetty",
				"testsource", new HashMap<>(), new Arvio(), "SV"));
		OppiaineArvosana d = new OppiaineArvosana(new Arvosana("testid", "testsuoritus", "B14", true, "testmyönnetty",
				"testsource", new HashMap<>(), new Arvio(), "SV"));
		OppiaineArvosana e = new OppiaineArvosana(new Arvosana("testid", "testsuoritus", "A1", true, "testmyönnetty",
				"testsource", new HashMap<>(), new Arvio(), "EN"));
		List<OppiaineArvosana> r = ArvosanaToAvainArvoDTOConverter.palautaAinenumerointi(Stream.of(a, b, c, d, e))
				.collect(Collectors.toList());
		List<OppiaineArvosana> en = r.stream().filter(o -> o.aine.startsWith("B") && "EN".equals(o.lisatieto)).collect(Collectors.toList());
		List<OppiaineArvosana> sv = r.stream().filter(o -> o.aine.startsWith("B") && "SV".equals(o.lisatieto)).collect(Collectors.toList());
		List<OppiaineArvosana> aEn = r.stream().filter(o -> o.aine.startsWith("A") && "EN".equals(o.lisatieto)).collect(Collectors.toList());
		assertEquals(2, en.size());
		assertEquals(2, sv.size());
		assertEquals(1, aEn.size());
		String enAine = en.stream().findAny().get().aine;
		String svAine = sv.stream().findAny().get().aine;
		String aEnAine = aEn.stream().findAny().get().aine;
		assertFalse(enAine.equals(svAine));
		assertFalse(svAine.equals(aEnAine));
		assertFalse(enAine.equals(aEnAine));
		assertTrue(en.stream().allMatch(o -> enAine.equals(o.aine)));
		assertTrue(sv.stream().allMatch(o -> svAine.equals(o.aine)));
	}
}
