package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Pistesyoton tuonti XLSX-tiedostolla
 */
public class PistesyotonTuontiTest {

	private static final Logger LOG = LoggerFactory
			.getLogger(PistesyotonTuontiTest.class);
	private final int DEFAULT_WIDTH = 8500;

	private InputStream pistesyottoResurssi(String resurssi) throws IOException {
		return new ClassPathResource("pistesyotto/" + resurssi)
				.getInputStream();
	}

	@Test
	public void testaaOutput() throws FileNotFoundException, IOException,
			JsonIOException, JsonSyntaxException {
		List<ValintakoeOsallistuminenDTO> osallistumistiedot = new Gson()
				.fromJson(
						new InputStreamReader(
								pistesyottoResurssi("List_ValintakoeOsallistuminenDTO.json")),
						new TypeToken<ArrayList<ValintakoeOsallistuminenDTO>>() {
						}.getType());

		List<ValintaperusteDTO> valintaperusteet = new Gson().fromJson(
				new InputStreamReader(
						pistesyottoResurssi("List_ValintaperusteDTO.json")),
				new TypeToken<ArrayList<ValintaperusteDTO>>() {
				}.getType());
		List<ApplicationAdditionalDataDTO> pistetiedot = new Gson()
				.fromJson(
						new InputStreamReader(
								pistesyottoResurssi("List_ApplicationAdditionalDataDTO.json")),
						new TypeToken<ArrayList<ApplicationAdditionalDataDTO>>() {
						}.getType());

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
				}
				// LOG.error("{}", new
				// GsonBuilder().setPrettyPrinting().create()
				// .toJson(pistesyottoRivi));
				// System.exit(0);
			}
		};

		PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(
				"testioidi1",
				"1.2.246.562.5.85532589612",
				"jep",
				"",
				"",
				"",
				Arrays.asList(
						"1_2_246_562_5_85532589612_urheilija_lisapiste",
						"Eläintenhoidon koulutusohjelma, pk (Maatalousalan perustutkinto), pääsy- ja soveltuvuuskoe",
						"kielikoe_fi"), osallistumistiedot, valintaperusteet,
				pistetiedot, kuuntelija);
		Excel excel = pistesyottoExcel.getExcel();
		// new FileInputStream("02.xlsx"));//
		excel.tuoXlsx(pistesyottoResurssi("pistesyotto.xlsx"));

		// Arrays.asList(Sarake.PIILOTETTU));

		// !!ALUE ON TURHA ABSTRAKTIO!!
		// TEE RIVEJÄ/ KOOSTERIVEJÄ / DECORABLE RIVEJÄ
		// TOISTEISIA RIVEJÄ
		// excel.tuoXlsx(xlsx);
		if (false) {
			InputStream xlsx = excel.vieXlsx();
			IOUtils.copy(excel.vieXlsx(), new FileOutputStream(
					"pistesyotto.xlsx"));
		}
	}
}
