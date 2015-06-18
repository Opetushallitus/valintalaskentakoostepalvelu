package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

public class PistesyotonTuonti6Test extends PistesyotonTuontiTestBase {
	@Test
	public void testaaOutput() throws Exception {
		List<ValintakoeOsallistuminenDTO> osallistumistiedot = lueOsallistumisTiedot("6/List_ValintakoeOsallistuminenDTO.json");
		List<ValintaperusteDTO> valintaperusteet = lueValintaperusteet("6/List_ValintaperusteDTO.json");
		List<ApplicationAdditionalDataDTO> pistetiedot = luePistetiedot("6/List_ApplicationAdditionalDataDTO.json");
        Collection<String> valintakoeTunnisteet = getValintakoeTunnisteet(valintaperusteet);

        tuoExcel(osallistumistiedot, valintaperusteet, pistetiedot, "6/muplattu.xlsx", "1.2.246.562.29.173465377510", "1.2.246.562.20.27513650047");

        ApplicationAdditionalDataDTO alenderinPistetiedot = pistetiedot.stream().filter(h -> h.getLastName().equals("Alander")).findFirst().get();
        //System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(dada));
        assertEquals(alenderinPistetiedot.getAdditionalData().get("SOTE1_kaikkiosiot-OSALLISTUMINEN"), "EI_OSALLISTUNUT");
        assertEquals(alenderinPistetiedot.getAdditionalData().get("SOTEKOE_VK_RYHMA1-OSALLISTUMINEN"), "EI_OSALLISTUNUT");
	}
}
