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

public class PistesyotonTuonti5Test extends PistesyotonTuontiTestBase {
	@Test
	public void testaaOutput() throws Exception {
		List<ValintakoeOsallistuminenDTO> osallistumistiedot = lueOsallistumisTiedot("5/List_ValintakoeOsallistuminenDTO.json");
		List<ValintaperusteDTO> valintaperusteet = lueValintaperusteet("5/List_ValintaperusteDTO.json");
		List<ApplicationAdditionalDataDTO> pistetiedot = luePistetiedot("5/List_ApplicationAdditionalDataDTO.json");

        Collection<String> valintakoeTunnisteet = getValintakoeTunnisteet(valintaperusteet);

        List<Hakemus> hakemukset = Collections.emptyList();
        PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri = new PistesyottoDataRiviListAdapter();
        PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(
                "1.2.246.562.29.173465377510", "1.2.246.562.20.88759382968", null, "Haku",
                "hakukohdeNimi", "tarjoajaNimi", hakemukset,
                Collections.emptySet(),
                valintakoeTunnisteet, osallistumistiedot,
                valintaperusteet, pistetiedot,
                pistesyottoTuontiAdapteri);
        pistesyottoExcel.getExcel().tuoXlsx(new ClassPathResource("pistesyotto/5/muplattu.xlsx").getInputStream());
        Map<String, ApplicationAdditionalDataDTO> pistetiedotMapping = asMap(pistetiedot);

        muplaa(pistesyottoTuontiAdapteri, pistetiedotMapping);

        ApplicationAdditionalDataDTO dada = pistetiedot.stream().filter(h -> h.getLastName().equals("Aaltonen-Ruttunen")).findFirst().get();
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(dada));
        assertEquals("5,5", dada.getAdditionalData().get("f8523684-9274-fc59-12a6-a8fe79ec8b84"));
        assertEquals("OSALLISTUI", dada.getAdditionalData().get("f8523684-9274-fc59-12a6-a8fe79ec8b84-OSALLISTUMINEN"));
        assertEquals("2.00", dada.getAdditionalData().get("582c0bbc-c323-cbff-6aea-0fddbe26d0e6"));
        assertEquals("OSALLISTUI", dada.getAdditionalData().get("582c0bbc-c323-cbff-6aea-0fddbe26d0e6-OSALLISTUMINEN"));

        ApplicationAdditionalDataDTO dada2 = pistetiedot.stream().filter(h -> h.getLastName().equals("Peloton")).findFirst().get();
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(dada2));
        assertEquals("1", dada2.getAdditionalData().get("f8523684-9274-fc59-12a6-a8fe79ec8b84"));
        assertEquals("OSALLISTUI", dada2.getAdditionalData().get("f8523684-9274-fc59-12a6-a8fe79ec8b84-OSALLISTUMINEN"));
        assertEquals("", dada2.getAdditionalData().get("582c0bbc-c323-cbff-6aea-0fddbe26d0e6"));
        assertEquals("MERKITSEMATTA", dada2.getAdditionalData().get("582c0bbc-c323-cbff-6aea-0fddbe26d0e6-OSALLISTUMINEN"));
	}
}
