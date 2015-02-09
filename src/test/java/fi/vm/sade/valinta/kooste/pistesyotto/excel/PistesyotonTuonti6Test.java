package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Pistesyoton tuonti XLSX-tiedostolla
 */
// @Ignore
public class PistesyotonTuonti6Test {

	private static final Logger LOG = LoggerFactory
			.getLogger(PistesyotonTuonti6Test.class);

	private String pistesyottoResurssi(String resurssi) throws IOException {
		InputStream i;
		String s = IOUtils.toString(i = new ClassPathResource("pistesyotto/6" +
                "/"
				+ resurssi).getInputStream(), "UTF-8");
		IOUtils.closeQuietly(i);
		return s;
	}

	@Test
	public void testaaOutput() throws Exception {
		List<ValintakoeOsallistuminenDTO> osallistumistiedot;
		String s = null;
		try {
			s = pistesyottoResurssi("List_ValintakoeOsallistuminenDTO.json");
			osallistumistiedot = new Gson().fromJson(

			s, new TypeToken<ArrayList<ValintakoeOsallistuminenDTO>>() {
			}.getType());
		} catch (Exception e) {
			LOG.error("\r\n{}\r\n", s);
			throw e;
		}
		List<ValintaperusteDTO> valintaperusteet = new Gson().fromJson(

		pistesyottoResurssi("List_ValintaperusteDTO.json"),
				new TypeToken<ArrayList<ValintaperusteDTO>>() {
				}.getType());
		List<ApplicationAdditionalDataDTO> pistetiedot = new Gson().fromJson(

		pistesyottoResurssi("List_ApplicationAdditionalDataDTO.json"),
				new TypeToken<ArrayList<ApplicationAdditionalDataDTO>>() {
				}.getType());

        Collection<String> valintakoeTunnisteet = FluentIterable
                .from(valintaperusteet)
                .transform(
                        new Function<ValintaperusteDTO, String>() {
                            @Override
                            public String apply(
                                    ValintaperusteDTO input) {
                                return input.getTunniste();
                            }
                        }).toList();

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

        List<ApplicationAdditionalDataDTO> uudetPistetiedot = Lists
                .newArrayList();
        for (PistesyottoRivi rivi : pistesyottoTuontiAdapteri
                .getRivit()) {
            ApplicationAdditionalDataDTO additionalData = pistetiedotMapping
                    .get(rivi.getOid());
            Map<String, String> originalPistetiedot = additionalData
                    .getAdditionalData();

            Map<String, String> newPistetiedot = rivi
                    .asAdditionalData();
            if (originalPistetiedot.equals(newPistetiedot)) {
                LOG.error("Ei muutoksia riville({},{})",
                        rivi.getOid(), rivi.getNimi());
            } else {
                if (rivi.isValidi()) {
                    LOG.error("Rivi on muuttunut ja eheä. Tehdään päivitys hakupalveluun");
                    Map<String, String> uudetTiedot = Maps
                            .newHashMap(originalPistetiedot);
                    uudetTiedot.putAll(newPistetiedot);
                    additionalData
                            .setAdditionalData(uudetTiedot);
                    uudetPistetiedot.add(additionalData);
                } else {
                    LOG.error("Rivi on muuttunut mutta viallinen joten ilmoitetaan virheestä!");

                    for (PistesyottoArvo arvo : rivi.getArvot()) {
                        if (!arvo.isValidi()) {
                            String virheIlmoitus = new StringBuffer()
                                    .append("Henkilöllä ")
                                    .append(rivi.getNimi())
                                            //
                                    .append(" (")
                                    .append(rivi.getOid())
                                    .append(")")
                                            //
                                    .append(" oli virheellinen arvo '")
                                    .append(arvo.getArvo())
                                    .append("'")
                                    .append(" kohdassa ")
                                    .append(arvo.getTunniste())
                                    .toString();

                            throw new RuntimeException(
                                    virheIlmoitus);
                        }
                    }

                }

            }
        }


        ApplicationAdditionalDataDTO dada = pistetiedot.stream().filter(h -> h.getLastName().equals("Alander")).findFirst().get();
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(dada));
        assertEquals(dada.getAdditionalData().get("SOTE1_kaikkiosiot-OSALLISTUMINEN"), "EI_OSALLISTUNUT");
        assertEquals(dada.getAdditionalData().get("SOTEKOE_VK_RYHMA1-OSALLISTUMINEN"), "EI_OSALLISTUNUT");

	}

    private Map<String, ApplicationAdditionalDataDTO> asMap(
            Collection<ApplicationAdditionalDataDTO> datas) {
        Map<String, ApplicationAdditionalDataDTO> mapping = Maps.newHashMap();
        for (ApplicationAdditionalDataDTO data : datas) {
            mapping.put(data.getOid(), data);
        }
        return mapping;
    }
}
