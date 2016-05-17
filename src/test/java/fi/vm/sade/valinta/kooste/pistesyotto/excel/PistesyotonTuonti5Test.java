package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.google.gson.GsonBuilder;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

public class PistesyotonTuonti5Test extends PistesyotonTuontiTestBase {
	@Test
	public void testaaOutput() throws Exception {
		List<ValintakoeOsallistuminenDTO> osallistumistiedot = lueOsallistumisTiedot("5/List_ValintakoeOsallistuminenDTO.json");
		List<ValintaperusteDTO> valintaperusteet = lueValintaperusteet("5/List_ValintaperusteDTO.json");
		List<ApplicationAdditionalDataDTO> pistetiedot = luePistetiedot("5/List_ApplicationAdditionalDataDTO.json");

        tuoExcel(osallistumistiedot, valintaperusteet, pistetiedot, "5/muplattu.xlsx", "1.2.246.562.29.173465377510", "1.2.246.562.20.88759382968");

        ApplicationAdditionalDataDTO aaltosenPistetiedot = pistetiedot.stream().filter(h -> h.getLastName().equals("Aaltonen-Ruttunen")).findFirst().get();
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(aaltosenPistetiedot));
        assertEquals("", aaltosenPistetiedot.getAdditionalData().get("f8523684-9274-fc59-12a6-a8fe79ec8b84"));
        assertEquals("MERKITSEMATTA", aaltosenPistetiedot.getAdditionalData().get("f8523684-9274-fc59-12a6-a8fe79ec8b84-OSALLISTUMINEN"));
        assertEquals("2.0", aaltosenPistetiedot.getAdditionalData().get("582c0bbc-c323-cbff-6aea-0fddbe26d0e6"));
        assertEquals("OSALLISTUI", aaltosenPistetiedot.getAdditionalData().get("582c0bbc-c323-cbff-6aea-0fddbe26d0e6-OSALLISTUMINEN"));

        ApplicationAdditionalDataDTO pelottomanPistetiedot = pistetiedot.stream().filter(h -> h.getLastName().equals("Peloton")).findFirst().get();
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(pelottomanPistetiedot));
        assertEquals("", pelottomanPistetiedot.getAdditionalData().get("f8523684-9274-fc59-12a6-a8fe79ec8b84"));
        assertEquals("MERKITSEMATTA", pelottomanPistetiedot.getAdditionalData().get("f8523684-9274-fc59-12a6-a8fe79ec8b84-OSALLISTUMINEN"));
        assertEquals("", pelottomanPistetiedot.getAdditionalData().get("582c0bbc-c323-cbff-6aea-0fddbe26d0e6"));
        assertEquals("MERKITSEMATTA", pelottomanPistetiedot.getAdditionalData().get("582c0bbc-c323-cbff-6aea-0fddbe26d0e6-OSALLISTUMINEN"));
	}
}
