package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.GsonBuilder;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

public class PistesyotonTuonti6Test extends PistesyotonTuontiTestBase {
	@Test
	public void testaaOutput() throws Exception {
		List<ValintakoeOsallistuminenDTO> osallistumistiedot = lueOsallistumisTiedot("6/List_ValintakoeOsallistuminenDTO.json");
		List<ValintaperusteDTO> valintaperusteet = lueValintaperusteet("6/List_ValintaperusteDTO.json");
		List<ApplicationAdditionalDataDTO> pistetiedot = luePistetiedot("6/List_ApplicationAdditionalDataDTO.json");
        Collection<String> valintakoeTunnisteet = getValintakoeTunnisteet(valintaperusteet);
        List<Hakemus> hakemukset = Collections.emptyList();
        PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri = new PistesyottoDataRiviListAdapter();
        PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(
                "1.2.246.562.29.173465377510", "1.2.246.562.20.27513650047", null, "Haku",
                "hakukohdeNimi", "tarjoajaNimi", hakemukset,
                Collections.emptySet(),
                valintakoeTunnisteet, osallistumistiedot,
                valintaperusteet, pistetiedot,
                pistesyottoTuontiAdapteri);
        pistesyottoExcel.getExcel().tuoXlsx(new ClassPathResource("pistesyotto/6/muplattu.xlsx").getInputStream());
        Map<String, ApplicationAdditionalDataDTO> pistetiedotMapping = asMap(pistetiedot);

        muplaa(pistesyottoTuontiAdapteri, pistetiedotMapping);

        ApplicationAdditionalDataDTO dada = pistetiedot.stream().filter(h -> h.getLastName().equals("Alander")).findFirst().get();
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(dada));
        assertEquals(dada.getAdditionalData().get("SOTE1_kaikkiosiot-OSALLISTUMINEN"), "EI_OSALLISTUNUT");
        assertEquals(dada.getAdditionalData().get("SOTEKOE_VK_RYHMA1-OSALLISTUMINEN"), "EI_OSALLISTUNUT");
	}
}
