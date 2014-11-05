package fi.vm.sade.valinta.kooste.viestintapalvelu;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;

public class HakijapalvelunOsoiteTest {

	@Ignore
	@Test
	public void testaaHakijapalvelunOsoitteenHaku() throws JsonSyntaxException,
			JsonIOException, IOException {
		OrganisaatioRDTO organisaatio = new Gson().fromJson(
				new InputStreamReader(new ClassPathResource(
						"organisaatio/organisaatiodto.json").getInputStream()),
				OrganisaatioRDTO.class);
	}
	@Ignore
	@Test
	public void testaaHakijapalvelunOsoitteenHaku2() throws JsonSyntaxException,
			JsonIOException, IOException {
		com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider provider = new com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider();
		fi.vm.sade.valinta.kooste.ObjectMapperProvider mapper = new fi.vm.sade.valinta.kooste.ObjectMapperProvider();
		
		String json = IOUtils.toString(new ClassPathResource(
						"organisaatio/org.json").getInputStream());
		mapper.getContext(OrganisaatioRDTO.class).readValue(json, OrganisaatioRDTO.class);
	}
}
