package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;
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

	private final int DEFAULT_WIDTH = 8500;

	@Test
	public void testaaOutput() throws FileNotFoundException, IOException {
		List<ValintakoeOsallistuminenDTO> osallistumistiedot = new Gson()
				.fromJson(
						new InputStreamReader(
								PistesyotonTuontiTest.class
										.getResourceAsStream("List_ValintakoeOsallistuminenDTO.json")),
						new TypeToken<ArrayList<ValintakoeOsallistuminenDTO>>() {
						}.getType());

		List<ValintaperusteDTO> valintaperusteet = new Gson().fromJson(
				new InputStreamReader(PistesyotonTuontiTest.class
						.getResourceAsStream("List_ValintaperusteDTO.json")),
				new TypeToken<ArrayList<ValintaperusteDTO>>() {
				}.getType());
		List<ApplicationAdditionalDataDTO> pistetiedot = new Gson()
				.fromJson(
						new InputStreamReader(
								PistesyotonTuontiTest.class
										.getResourceAsStream("List_ApplicationAdditionalDataDTO.json")),
						new TypeToken<ArrayList<ApplicationAdditionalDataDTO>>() {
						}.getType());

		PistesyottoDataRiviKuuntelija kuuntelija = new PistesyottoDataRiviKuuntelija() {
			@Override
			public void pistesyottoDataRiviTapahtuma(
					PistesyottoRivi pistesyottoRivi) {
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
		excel.tuoXlsx(this.getClass().getResourceAsStream("pistesyotto.xlsx"));

		// Arrays.asList(Sarake.PIILOTETTU));

		// !!ALUE ON TURHA ABSTRAKTIO!!
		// TEE RIVEJÄ/ KOOSTERIVEJÄ / DECORABLE RIVEJÄ
		// TOISTEISIA RIVEJÄ
		// excel.tuoXlsx(xlsx);

		// InputStream xlsx = excel.vieXlsx();
		// IOUtils.copy(excel.vieXlsx(), new FileOutputStream("02.xlsx"));
	}
}
