package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

public class PistesyotonVientiTuontiRoundtripTest extends PistesyotonTuontiTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(PistesyotonVientiTuontiRoundtripTest.class);

	@Test
	public void testaaOutput() throws FileNotFoundException, IOException,
			JsonIOException, JsonSyntaxException, Exception {
		List<ValintakoeOsallistuminenDTO> osallistumistiedot = lueOsallistumisTiedot("List_ValintakoeOsallistuminenDTO.json");
		List<ValintaperusteDTO> valintaperusteet = lueValintaperusteet("List_ValintaperusteDTO.json");
		List<ApplicationAdditionalDataDTO> pistetiedot = luePistetiedot("List_ApplicationAdditionalDataDTO.json");
		PistesyottoDataRiviKuuntelija kuuntelija = new PistesyottoDataRiviListAdapter();

		PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(
				"testioidi1",
				"1.2.246.562.5.85532589612",
				"jep",
				"",
				"",
				"",
				Collections.emptyList(),
				Collections.<String>emptySet(),
				Arrays.asList(
						"1_2_246_562_5_85532589612_urheilija_lisapiste",
						"Eläintenhoidon koulutusohjelma, pk (Maatalousalan perustutkinto), pääsy- ja soveltuvuuskoe",
						"kielikoe_fi"), osallistumistiedot, valintaperusteet,
				pistetiedot, kuuntelija);
		Excel excel = pistesyottoExcel.getExcel();

		excel.tuoXlsx(excel.vieXlsx());

		// tallenna(excel);
	}
}
