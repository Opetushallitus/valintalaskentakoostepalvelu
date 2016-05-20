package fi.vm.sade.valinta.kooste.valintalaskenta;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;
import fi.vm.sade.valinta.kooste.util.sure.YoToAvainSuoritustietoDTOConverter;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.AvainMetatiedotDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
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

		aa.stream().forEach(a -> {
			LOG.info("{}\t\t{}", a.getAvain(), a.getArvo());
		});

		Assert.assertTrue("Peruskoulutuksen A12 oppiaine on ES",
				aa.stream().filter(a -> "PK_A12_OPPIAINE".equals(a.getAvain()) && "ES".equals(a.getArvo())).count() == 1L);

		Assert.assertEquals("HI:lla on lisäksi kaksi valinnaista", 3L,
				aa.stream().filter(a -> a.getAvain().startsWith("PK_HI")).count());

		Assert.assertTrue("PK_A12_VAL1 löytyy",
				aa.stream().filter(a -> a.getAvain().equals("PK_A12_VAL1")).count() == 1L);
	}

	@Ignore
	@Test
	public void testaaSureKonvertointi() throws JsonSyntaxException,
			IOException {
		List<Oppija> oppijat = new Gson().fromJson(IOUtils
				.toString(new ClassPathResource("oppijat.json")
						.getInputStream()), new TypeToken<List<Oppija>>() {
		}.getType());
		for (Oppija o : oppijat) {
			if (!o.getSuoritukset().isEmpty()) {
				LOG.info("{}", new GsonBuilder().setPrettyPrinting().create()
						.toJson(o.getSuoritukset()));
				LOG.info(
						"###\r\n{}",
						new GsonBuilder()
								.setPrettyPrinting()
								.create()
								.toJson(OppijaToAvainArvoDTOConverter
										.convert(o.getOppijanumero(), o.getSuoritukset(), new HakemusDTO(),null)));

			}
		}
	}

    @Test
    public void testaaUseampiLisaopetusKonvertointi() throws JsonSyntaxException,
            IOException {
        List<Oppija> oppijat = new Gson().fromJson(IOUtils
                .toString(new ClassPathResource("monta_lisaopetusta.json")
                        .getInputStream()), new TypeToken<List<Oppija>>() {
        }.getType());
        for (Oppija o : oppijat) {
            if (!o.getSuoritukset().isEmpty()) {
                LOG.info("{}", new GsonBuilder().setPrettyPrinting().create()
                        .toJson(o.getSuoritukset()));
                LOG.info(
                        "###\r\n{}",
                        new GsonBuilder()
                                .setPrettyPrinting()
                                .create()
                                .toJson(OppijaToAvainArvoDTOConverter
                                        .convert(o.getOppijanumero(), o.getSuoritukset(), new HakemusDTO(),null)));

            }
        }
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

		assertEquals(ImmutableSet.of(
				new AvainArvoDTO("PK_B1", "10"), new AvainArvoDTO("PK_B1_OPPIAINE", "SV"),
				new AvainArvoDTO("PK_B13", "10"), new AvainArvoDTO("PK_B13_OPPIAINE", "EN")
		), ImmutableSet.copyOf(arvot));
	}

}
