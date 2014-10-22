package fi.vm.sade.valinta.kooste.viestintapalvelu;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;

public class HakijapalvelunOsoiteTest {

	@Test
	public void testaaHakijapalvelunOsoitteenHaku() throws JsonSyntaxException,
			JsonIOException, IOException {
		OrganisaatioRDTO organisaatio = new Gson().fromJson(
				new InputStreamReader(new ClassPathResource(
						"organisaatio/organisaatiodto.json").getInputStream()),
				OrganisaatioRDTO.class);
		for (Map<String, String> yhteystiedot : organisaatio.getMetadata()
				.getYhteystiedot()) {

		}
	}
}
