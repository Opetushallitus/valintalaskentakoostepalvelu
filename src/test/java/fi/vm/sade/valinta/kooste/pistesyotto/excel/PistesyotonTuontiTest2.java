package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Pistesyoton tuonti XLSX-tiedostolla
 */
public class PistesyotonTuontiTest2 {
	private static final Logger LOG = LoggerFactory
			.getLogger(PistesyotonTuontiTest2.class);

	private String pistesyottoResurssi(String resurssi) throws IOException {
		InputStream i;
		String s = IOUtils.toString(i = new ClassPathResource("pistesyotto/2/"
				+ resurssi).getInputStream(), "UTF-8");
		IOUtils.closeQuietly(i);
		return s;
	}

	@Test
	public void testaaOutput() throws FileNotFoundException, IOException,
			JsonIOException, JsonSyntaxException, Exception {
		List<ValintakoeOsallistuminenDTO> osallistumistiedot;
		String s = null;
		try {
			s = pistesyottoResurssi("valintakoe.json");
			osallistumistiedot = new GsonBuilder()
					.registerTypeAdapter(Date.class, new JsonDeserializer() {
						@Override
						public Object deserialize(JsonElement json,
								Type typeOfT, JsonDeserializationContext context)
								throws JsonParseException {
							// TODO Auto-generated method stub
							return new Date(json.getAsJsonPrimitive()
									.getAsLong());
						}

					})
					.create()
					.fromJson(
							s,
							new TypeToken<ArrayList<ValintakoeOsallistuminenDTO>>() {
							}.getType());
		} catch (Exception e) {
			LOG.error("\r\n{}\r\n", s);
			throw e;
		}
		Assert.assertFalse(osallistumistiedot.isEmpty());
		List<ValintaperusteDTO> valintaperusteet = new Gson().fromJson(
				pistesyottoResurssi("avaimet.json"),
				new TypeToken<ArrayList<ValintaperusteDTO>>() {
				}.getType());
		Assert.assertFalse(valintaperusteet.isEmpty());
		List<ApplicationAdditionalDataDTO> pistetiedot = new Gson().fromJson(
				pistesyottoResurssi("add_data.json"),
				new TypeToken<ArrayList<ApplicationAdditionalDataDTO>>() {
				}.getType());
		Assert.assertFalse(pistetiedot.isEmpty());
		List<Hakemus> hakemukset = new Gson().fromJson(
				pistesyottoResurssi("listfull.json"),
				new TypeToken<ArrayList<Hakemus>>() {
				}.getType());
		Assert.assertFalse(hakemukset.isEmpty());
		PistesyottoDataRiviKuuntelija kuuntelija = new PistesyottoDataRiviKuuntelija() {
			@Override
			public void pistesyottoDataRiviTapahtuma(
					PistesyottoRivi pistesyottoRivi) {
				if (!pistesyottoRivi.isValidi()) {
					for (PistesyottoArvo arvo : pistesyottoRivi.getArvot()) {
						if (!arvo.isValidi()) {
							String virheIlmoitus = new StringBuffer()
									.append("Henkilöllä ")
									.append(pistesyottoRivi.getNimi())
									//
									.append(" (")
									.append(pistesyottoRivi.getOid())
									.append(")")
									//
									.append(" oli virheellinen arvo '")
									.append(arvo.getArvo()).append("'")
									.append(" kohdassa ")
									.append(arvo.getTunniste()).toString();

							LOG.error("{}", virheIlmoitus);
						}
					}
				} else {
				LOG.error("{}", pistesyottoRivi);
				}
				// LOG.error("{}", new
				// GsonBuilder().setPrettyPrinting().create()
				// .toJson(pistesyottoRivi));
				// System.exit(0);
			}
		};
		Collection<String> valintakoeTunnisteet = null;
		try {
			valintakoeTunnisteet = FluentIterable.from(valintaperusteet)
					.transform(new Function<ValintaperusteDTO, String>() {
						@Override
						public String apply(ValintaperusteDTO input) {
							return input.getTunniste();
						}
					}).toList();
		} catch (Exception e) {
		}
		//pistetiedot.forEach(t -> LOG.error("{}", t));
		PistesyottoExcel pistesyottoExcel = new PistesyottoExcel("testioidi1",
				"1.2.246.562.20.61064567623", "jep", "", "", "",
				hakemukset, valintakoeTunnisteet,
				osallistumistiedot, valintaperusteet, pistetiedot, kuuntelija);
		Excel excel = pistesyottoExcel.getExcel();
		// new FileInputStream("02.xlsx"));//

		//excel.tuoXlsx(new ClassPathResource("pistesyotto/2/pistesyotto.xlsx").getInputStream());

		// Arrays.asList(Sarake.PIILOTETTU));

		// !!ALUE ON TURHA ABSTRAKTIO!!
		// TEE RIVEJÄ/ KOOSTERIVEJÄ / DECORABLE RIVEJÄ
		// TOISTEISIA RIVEJÄ
		// excel.tuoXlsx(xlsx);
		if (false) {
			IOUtils.copy(excel.vieXlsx(), new FileOutputStream(
					"pistesyotto.xlsx"));
		}
	}
}
