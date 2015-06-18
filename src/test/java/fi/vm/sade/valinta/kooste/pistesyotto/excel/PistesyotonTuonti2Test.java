package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

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
		List<Hakemus> hakemukset = lueHakemukset("2/listfull.json");
		Assert.assertFalse(hakemukset.isEmpty());
        PistesyottoDataRiviKuuntelija kuuntelija = new PistesyottoDataRiviListAdapter();
		Collection<String> valintakoeTunnisteet = getValintakoeTunnisteet(valintaperusteet);
		PistesyottoExcel pistesyottoExcel = new PistesyottoExcel("testioidi1",
				"1.2.246.562.20.61064567623", "jep", "", "", "",
				hakemukset,Collections.<String>emptySet(), valintakoeTunnisteet,
				osallistumistiedot, valintaperusteet, pistetiedot, kuuntelija);

		//tallenna(pistesyottoExcel.getExcel());
	}
}
