package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.util.List;

import org.junit.Test;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

public class PistesyotonTuonti7Test extends PistesyotonTuontiTestBase {
	@Test
	public void testaaOutput() throws Exception {
		List<ValintakoeOsallistuminenDTO> osallistumistiedot = lueOsallistumisTiedot("7/List_ValintakoeOsallistuminenDTO.json");
		List<ValintaperusteDTO> valintaperusteet = lueValintaperusteet("7/List_ValintaperusteDTO.json");
		List<ApplicationAdditionalDataDTO> pistetiedot = luePistetiedot("7/List_ApplicationAdditionalDataDTO.json");

        tuoExcel(osallistumistiedot, valintaperusteet, pistetiedot, "7/muplattu.xlsx", "1.2.246.562.29.173465377510", "1.2.246.562.20.17162646719");

        /*
        ApplicationAdditionalDataDTO dada = pistetiedot.stream().filter(h -> h.getLastName().equals("Andelin")).findFirst().get();
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(dada));
        */
	}
}
