package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.util.sure.YoToAvainSuoritustietoDTOConverter;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.AvainMetatiedotDTO;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;

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

		List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(o, pmetrit);

		aa.stream().forEach(a -> {
			LOG.info("{}\t\t{}", a.getAvain(), a.getArvo());
		});

		Assert.assertTrue("Ensikertalaisuus on datalla tosi",
				aa.stream().filter(a -> "ensikertalainen".equals(a.getAvain()) && "true".equals(a.getArvo())).count() == 1L);

		Assert.assertTrue("Peruskoulutuksen A12 oppiaine on ES",
				aa.stream().filter(a -> "PK_A12_OPPIAINE".equals(a.getAvain()) && "ES".equals(a.getArvo())).count() == 1L);

		Assert.assertTrue("HI:lla on lisäksi kaksi valinnaista",
				aa.stream().filter(a -> a.getAvain().startsWith("PK_HI")).count() ==3L);

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
										.convert(o,null)));

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
            final long count = konvertoitu.stream().filter(k -> k.getAvain().equals(a)).count();
            Assert.assertEquals(count, 1);
        });

        final int ainereaali = konvertoitu.stream().filter(k -> k.getAvain().equals("AINEREAALI")).findFirst().get().getMetatiedot().size();
        final int reaali = konvertoitu.stream().filter(k -> k.getAvain().equals("REAALI")).findFirst().get().getMetatiedot().size();
        final int pitka_kieli = konvertoitu.stream().filter(k -> k.getAvain().equals("PITKA_KIELI")).findFirst().get().getMetatiedot().size();
        final int keskipitka_kieli = konvertoitu.stream().filter(k -> k.getAvain().equals("KESKIPITKA_KIELI")).findFirst().get().getMetatiedot().size();
        final int lyhyt_kieli = konvertoitu.stream().filter(k -> k.getAvain().equals("LYHYT_KIELI")).findFirst().get().getMetatiedot().size();
        final int aidinkieli = konvertoitu.stream().filter(k -> k.getAvain().equals("AIDINKIELI")).findFirst().get().getMetatiedot().size();

        Assert.assertEquals(12, ainereaali);
        Assert.assertEquals(3, reaali);
        Assert.assertEquals(9, pitka_kieli);
        Assert.assertEquals(8, keskipitka_kieli);
        Assert.assertEquals(10, lyhyt_kieli);
        Assert.assertEquals(7, aidinkieli);


    }

}
