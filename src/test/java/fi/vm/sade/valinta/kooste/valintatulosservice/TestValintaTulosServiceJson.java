package fi.vm.sade.valinta.kooste.valintatulosservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

public class TestValintaTulosServiceJson {

	private final static Logger LOG = LoggerFactory
			.getLogger(TestValintaTulosServiceJson.class);

	@Test
	public void testaaJson() throws JsonSyntaxException, IOException {
		Gson GSON = new Gson();
		Collection<ValintaTulosServiceDto> valintaTulosServiceDto = GSON
				.fromJson(
						IOUtils.toString(new ClassPathResource(
								"valintatulosservice/valintatuloservice_1.2.246.562.29.173465377510.json")
								.getInputStream()),
						new TypeToken<ArrayList<ValintaTulosServiceDto>>() {
						}.getType());
		valintaTulosServiceDto = valintaTulosServiceDto
				.stream()
				.filter(vts -> vts
						.getHakutoiveet()
						.stream()
						.anyMatch(
								hakutoive -> hakutoive.getVastaanottotila()
										.isVastaanottanut()

						)).collect(Collectors.toList());
		LOG.error("{}", valintaTulosServiceDto.size());
		LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(valintaTulosServiceDto));
	}
}
