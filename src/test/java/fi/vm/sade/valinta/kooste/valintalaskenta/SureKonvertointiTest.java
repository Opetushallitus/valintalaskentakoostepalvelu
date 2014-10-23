package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
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

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvio;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class SureKonvertointiTest {
	private static final Logger LOG = LoggerFactory
			.getLogger(SureKonvertointiTest.class);

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
										.convert(o)));

			}
		}
	}

	@Test
	public void testaaArvosanaMax() {
		String ARVOSANA = "L";
		Arvosana a = new Arvosana(null, null, null, null, null, null,
				new Arvio(ARVOSANA, null, null), null);
		Assert.assertTrue(ARVOSANA.equals(OppijaToAvainArvoDTOConverter
				.max(Arrays.asList(a)).getArvio().getArvosana()));
	}

	@Test
	public void testaaArvosanaMaxNollalla() {
		Assert.assertTrue(OppijaToAvainArvoDTOConverter.max(Arrays.asList()) == null);
	}

	@Test
	public void testaaArvosanaMaxKahdella() {
		String ARVOSANA = "L";
		String ARVOSANA2 = "B";
		Arvosana a = new Arvosana(null, null, null, null, null, null,
				new Arvio(ARVOSANA, null, null), null);
		Arvosana b = new Arvosana(null, null, null, null, null, null,
				new Arvio(ARVOSANA2, null, null), null);
		Assert.assertTrue(ARVOSANA.equals(OppijaToAvainArvoDTOConverter
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
		Assert.assertTrue(ARVOSANA3.equals(OppijaToAvainArvoDTOConverter
				.max(Arrays.asList(a, b, c)).getArvio().getArvosana()));
	}
}
