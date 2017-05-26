package fi.vm.sade.valinta.kooste.valintatulosservice;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Change;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Muutoshistoria;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
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

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;

public class TestValintaTulosServiceJson {

	private final static Logger LOG = LoggerFactory
			.getLogger(TestValintaTulosServiceJson.class);

	@Test
	public void testaaMuutoshistoriaJson() throws JsonSyntaxException, IOException {
		Gson GSON = new Gson();
		List<Muutoshistoria> muutoshistoriat = GSON
				.fromJson(
						IOUtils.toString(new ClassPathResource(
								"valintatulosservice/muutoshistoria.json")
								.getInputStream()),
						new TypeToken<List<Muutoshistoria>>() {
						}.getType());

		final Predicate<Change> isVastaanottoChange = (change) -> "vastaanottotila".equals(change.getField());
		final Predicate<Map.Entry<String, Date>> isVastaanotto = entry -> asList("VASTAANOTTANUT_SITOVASTI", "VASTAANOTTANUT", "EHDOLLISESTI_VASTAANOTTANUT").contains(entry.getKey());

		Comparator<Object> reversed = comparing(entry -> ((Map.Entry<String, Date>) entry).getValue()).reversed();


		Optional<Map.Entry<String, Date>> newestVastaanottoFieldStatus = muutoshistoriat.stream()
				.flatMap(m -> m.getChanges().stream().filter(isVastaanottoChange)
						.map(c -> Maps.immutableEntry(c.getTo(), m.getTimestamp())))
				.sorted(Comparator.<Map.Entry<String, Date>, Date>comparing(Map.Entry::getValue).reversed()).findFirst();

		Map.Entry<String, Date> newestVastaanotto = newestVastaanottoFieldStatus.get();

		Assert.assertEquals(newestVastaanotto.getKey(), "EHDOLLISESTI_VASTAANOTTANUT");
	}
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
