package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PistesyotonTuonti2Test extends PistesyotonTuontiTestBase {

	@Test
	public void testaaOutput() throws FileNotFoundException, IOException,
			JsonIOException, JsonSyntaxException, Exception {
		List<ValintakoeOsallistuminenDTO> osallistumistiedot = lueOsallistumisTiedot("2/valintakoe.json");
		Assert.assertFalse(osallistumistiedot.isEmpty());
		List<ValintaperusteDTO> valintaperusteet = lueValintaperusteet("2/avaimet.json");
		Assert.assertFalse(valintaperusteet.isEmpty());
		List<ApplicationAdditionalDataDTO> pistetiedot = luePistetiedot("2/add_data.json");
		Assert.assertFalse(pistetiedot.isEmpty());
		List<HakemusWrapper> hakemukset = lueHakemukset("2/listfull.json").stream()
				.map(HakuappHakemusWrapper::new).collect(Collectors.toList());
		Assert.assertFalse(hakemukset.isEmpty());
		Collection<String> valintakoeTunnisteet = getValintakoeTunnisteet(valintaperusteet);
		PistesyottoExcel pistesyottoExcel = new PistesyottoExcel("testioidi1",
				"1.2.246.562.20.61064567623", "jep", "", "", "",
				Optional.empty(),
				hakemukset,Collections.<String>emptySet(), valintakoeTunnisteet,
				osallistumistiedot, valintaperusteet, pistetiedot,
				Collections.singletonList(new PistesyottoDataRiviListAdapter()));

		//tallenna(pistesyottoExcel.getExcel());
	}
}
