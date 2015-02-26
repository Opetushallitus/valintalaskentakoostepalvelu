package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.util.sure.ArvosanaToAvainArvoDTOConverter;
import fi.vm.sade.valinta.kooste.util.sure.YoToAvainArvoDTOConverter;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
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
	public void testaaLukioYoKonvertointi() throws JsonSyntaxException,
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

		Assert.assertTrue("EA:n arvo on E",
				aa.stream().filter(a -> "EA".equals(a.getAvain()) && "E".equals(a.getArvo())).count() == 1L);

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
	public void testaaArvosanaMax() {
		String ARVOSANA = "L";
		Arvosana a = new Arvosana(null, null, null, null, null, null,
				new Arvio(ARVOSANA, null, null), null);
		Assert.assertTrue(ARVOSANA.equals(YoToAvainArvoDTOConverter
				.max(Arrays.asList(a)).getArvio().getArvosana()));
	}

	@Test
	public void testaaArvosanaMaxNollalla() {
		Assert.assertTrue(YoToAvainArvoDTOConverter.max(Arrays.asList()) == null);
	}

	@Test
	public void testaaArvosanaMaxKahdella() {
		String ARVOSANA = "L";
		String ARVOSANA2 = "B";
		Arvosana a = new Arvosana(null, null, null, null, null, null,
				new Arvio(ARVOSANA, null, null), null);
		Arvosana b = new Arvosana(null, null, null, null, null, null,
				new Arvio(ARVOSANA2, null, null), null);
		Assert.assertTrue(ARVOSANA.equals(YoToAvainArvoDTOConverter
				.max(Arrays.asList(a, b)).getArvio().getArvosana()));
	}

	@Test
	public void testaaArvosanaMaxKolmella() {
		String ARVOSANA = "A";
		String ARVOSANA2 = "B";
		String ARVOSANA3 = "C";
		Arvosana a = new Arvosana(null, null, null, null, null, null,
				new Arvio(ARVOSANA, null, null), null);
		Arvosana b = new Arvosana(null, null, null, null, null, null,
				new Arvio(ARVOSANA2, null, null), null);
		Arvosana c = new Arvosana(null, null, null, null, null, null,
				new Arvio(ARVOSANA3, null, null), null);
		Assert.assertTrue(ARVOSANA3.equals(YoToAvainArvoDTOConverter
				.max(Arrays.asList(a, b, c)).getArvio().getArvosana()));
	}
}
