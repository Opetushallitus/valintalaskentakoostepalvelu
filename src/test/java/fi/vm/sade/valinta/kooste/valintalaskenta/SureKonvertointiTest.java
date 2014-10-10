package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;

public class SureKonvertointiTest {
	private static final Logger LOG = LoggerFactory
			.getLogger(SureKonvertointiTest.class);

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
}
