package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintatapajonoTuontiTest {

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

	@Ignore
	@Test
	public void testaaValintatapajonoTuonti() throws JsonSyntaxException,
			IOException {
		// fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.ValinnanvaiheDTO;
		List<ValintatietoValinnanvaiheDTO> valinnanvaihe = new GsonBuilder()
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

		ValintatapajonoExcel valintatapajonoExcel = new ValintatapajonoExcel(
				"hakuOid", "hakukohdeOid", "14017934785463582418268204255542",
				"Haun Nimi", "Hakukohteen Nimi", valinnanvaihe, hakemukset);
		Excel excel = valintatapajonoExcel.getExcel();
		if (false) {
			IOUtils.copy(excel.vieXlsx(), new FileOutputStream(
					"valintatapajono.xlsx"));
		}

	}

	private static String resurssi(String resurssi) throws IOException {
		InputStream i;
		String s = IOUtils.toString(i = new ClassPathResource(
				"valintatapajono/" + resurssi).getInputStream(), "UTF-8");
		IOUtils.closeQuietly(i);
		return s;
	}
}
