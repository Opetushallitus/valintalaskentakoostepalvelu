package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

public class PistesyotonTuonti7Test extends PistesyotonTuontiTestBase {
	@Test
	public void testaaOutput() throws Exception {
		List<ValintakoeOsallistuminenDTO> osallistumistiedot = lueOsallistumisTiedot("7/List_ValintakoeOsallistuminenDTO.json");
		List<ValintaperusteDTO> valintaperusteet = lueValintaperusteet("7/List_ValintaperusteDTO.json");
		List<ApplicationAdditionalDataDTO> pistetiedot = luePistetiedot("7/List_ApplicationAdditionalDataDTO.json");
        Collection<String> valintakoeTunnisteet = getValintakoeTunnisteet(valintaperusteet);

        List<Hakemus> hakemukset = Collections.emptyList();
        PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri = new PistesyottoDataRiviListAdapter();
        PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(
                "1.2.246.562.29.173465377510", "1.2.246.562.20.17162646719", null, "Haku",
                "hakukohdeNimi", "tarjoajaNimi", hakemukset,
                Collections.emptySet(),
                valintakoeTunnisteet, osallistumistiedot,
                valintaperusteet, pistetiedot,
                pistesyottoTuontiAdapteri);
        pistesyottoExcel.getExcel().tuoXlsx(new ClassPathResource("pistesyotto/7/muplattu.xlsx").getInputStream());
        Map<String, ApplicationAdditionalDataDTO> pistetiedotMapping = asMap(pistetiedot);

        muplaa(pistesyottoTuontiAdapteri, pistetiedotMapping);

        /*
        ApplicationAdditionalDataDTO dada = pistetiedot.stream().filter(h -> h.getLastName().equals("Andelin")).findFirst().get();
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(dada));
        */
	}
}
