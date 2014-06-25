package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintatapajonoTuontiTest {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintatapajonoTuontiTest.class);

	@Ignore
	@Test
	public void testaaTallennuksenYhtenevyysKayttoliittymanTuottamanJsoninKanssa()
			throws JsonSyntaxException, IOException {
		ValinnanvaiheDTO valinnanvaiheKali = new GsonBuilder()
				.registerTypeAdapter(Date.class, new JsonDeserializer() {
					@Override
					public Object deserialize(JsonElement json, Type typeOfT,
							JsonDeserializationContext context)
							throws JsonParseException {
						// TODO Auto-generated method stub
						return new Date(json.getAsJsonPrimitive().getAsLong());
					}

				})
				.create()
				.fromJson(resurssi("valinnanvaihe2.json"),
						new TypeToken<ArrayList<ValinnanvaiheDTO>>() {
						}.getType());
		ValinnanvaiheDTO valinnanvaiheExcel = null;

		Assert.assertTrue(
				"Excelista saatu valinnanvaihe ei tasmaa kayttoliittyman tallennuksessa tuottamaa valinnanvaihetta.",
				EqualsBuilder.reflectionEquals(valinnanvaiheKali,
						valinnanvaiheExcel));
	}

	// @Ignore
	@Test
	public void testaaValintatapajonoTuonti() throws JsonSyntaxException,
			IOException {
		// fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.ValinnanvaiheDTO;
		List<ValinnanvaiheDTO> valinnanvaihe = new GsonBuilder()
				.registerTypeAdapter(Date.class, new JsonDeserializer() {
					@Override
					public Object deserialize(JsonElement json, Type typeOfT,
							JsonDeserializationContext context)
							throws JsonParseException {
						// TODO Auto-generated method stub
						return new Date(json.getAsJsonPrimitive().getAsLong());
					}

				})
				.create()
				.fromJson(resurssi("valinnanvaihe.json"),
						new TypeToken<ArrayList<ValinnanvaiheDTO>>() {
						}.getType());

		// List<ValinnanVaiheJonoillaDTO> valinnanvaiheJonoillaDto = new
		// GsonBuilder()
		// .registerTypeAdapter(Date.class, new JsonDeserializer() {
		// @Override
		// public Object deserialize(JsonElement json, Type typeOfT,
		// JsonDeserializationContext context)
		// throws JsonParseException {
		// // TODO Auto-generated method stub
		// return new Date(json.getAsJsonPrimitive().getAsLong());
		// }
		//
		// })
		// .create()
		// .fromJson(resurssi("ilmanlaskentaa.json"),
		// new TypeToken<ArrayList<ValinnanVaiheJonoillaDTO>>() {
		// }.getType());

		List<Hakemus> hakemukset = new GsonBuilder()
				.registerTypeAdapter(Date.class, new JsonDeserializer() {
					@Override
					public Object deserialize(JsonElement json, Type typeOfT,
							JsonDeserializationContext context)
							throws JsonParseException {
						// TODO Auto-generated method stub
						return new Date(json.getAsJsonPrimitive().getAsLong());
					}

				})
				.create()
				.fromJson(resurssi("listfull.json"),
						new TypeToken<ArrayList<Hakemus>>() {
						}.getType());
		ValintatapajonoDataRiviListAdapter listaus = new ValintatapajonoDataRiviListAdapter();
		ValintatapajonoExcel valintatapajonoExcel = new ValintatapajonoExcel(
				"1.2.246.562.5.2013080813081926341927",
				"1.2.246.562.14.2013082110450143806511",
				"14017934785463582418268204255542", "Haun Nimi",
				"Hakukohteen Nimi", valinnanvaihe, hakemukset,
				Arrays.asList(listaus));
		valintatapajonoExcel.getExcel().tuoXlsx(
				new ClassPathResource("valintatapajono/valintatapajono.xlsx")
						.getInputStream());
		for (ValintatapajonoRivi r : listaus.getRivit()) {
			LOG.error("{} {}", r.getJonosija(), r.getNimi());

		}
		// Excel excel = valintatapajonoExcel.getExcel();
		// if (false) {
		// IOUtils.copy(excel.vieXlsx(), new FileOutputStream(
		// "valintatapajono.xlsx"));
		// }

	}

	private static String resurssi(String resurssi) throws IOException {
		InputStream i;
		String s = IOUtils.toString(i = new ClassPathResource(
				"valintatapajono/" + resurssi).getInputStream(), "UTF-8");
		IOUtils.closeQuietly(i);
		return s;
	}
}
