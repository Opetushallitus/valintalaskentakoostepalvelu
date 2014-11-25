package fi.vm.sade.valinta.kooste.viestintapalvelu;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Organisaatio;
import fi.vm.sade.valinta.kooste.util.Kieli;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.LueHakijapalvelunOsoite;

public class HakijapalvelunOsoiteTest {

	@Ignore
	@Test
	public void testaaHakijapalvelunOsoitteenHaku3() throws JsonSyntaxException,
			JsonIOException, IOException {
		Organisaatio organisaatio = new Gson().fromJson(
				new InputStreamReader(new ClassPathResource(
						"organisaatio/osoite.json").getInputStream()),
						Organisaatio.class);
		HaeOsoiteKomponentti h = new HaeOsoiteKomponentti(null);
		System.err.println(new GsonBuilder().setPrettyPrinting().create().toJson(organisaatio));
		Osoite osoite = LueHakijapalvelunOsoite.lueHakijapalvelunOsoite(h, KieliUtil.SUOMI, organisaatio, new Teksti());
	}

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
