package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintatapajonoTuontiTest {

	@Test
	public void testaaValintatapajonoTuonti() throws JsonSyntaxException,
			IOException {
		// fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.ValinnanvaiheDTO;
		List<ValinnanvaiheDTO> v = new GsonBuilder()
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
		ValintatapajonoExcel excel = new ValintatapajonoExcel("hakuOid",
				"hakukohdeOid", "1400827524125-5286600445600454965",
				"Haun Nimi", "Hakukohteen Nimi", v);

	}

	private static String resurssi(String resurssi) throws IOException {
		InputStream i;
		String s = IOUtils.toString(i = new ClassPathResource(
				"valintatapajono/" + resurssi).getInputStream(), "UTF-8");
		IOUtils.closeQuietly(i);
		return s;
	}
}
