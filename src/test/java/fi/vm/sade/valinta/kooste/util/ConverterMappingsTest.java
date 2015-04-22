package fi.vm.sade.valinta.kooste.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Eligibility;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;

public class ConverterMappingsTest {
	private static final Logger LOG = LoggerFactory
			.getLogger(ConverterMappingsTest.class);

	@Test
	public void testaaEligibilitiesOikeallaDatalla()
			throws JsonSyntaxException, IOException {
		List<Hakemus> hakemukset = new Gson().fromJson(IOUtils
				.toString(new ClassPathResource("listfull2_eligibilities.json")
						.getInputStream()), new TypeToken<List<Hakemus>>() {
		}.getType());
		Hakemus hakemus = hakemukset.stream()
				.filter(h -> "1.2.246.562.11.00000977230".equals(h.getOid()))
				.distinct().iterator().next();
		HakemusDTO dto = Converter.hakemusToHakemusDTO(hakemus);
		// LOG.error("\r\n{}", new GsonBuilder().setPrettyPrinting().create()
		// .toJson(dto));
		Assert.assertTrue(dto
				.getAvaimet()
				.stream()
				.filter(pari -> "preference1-Koulutus-id-eligibility"
						.equals(pari.getAvain())
						&& "NOT_CHECKED".equals(pari.getArvo())).distinct()
				.iterator().hasNext());

	}

	@Test
	public void testaaEligibilitiesMappaustaNullArvoilla() {
		Assert.assertTrue(Collections.emptyMap().equals(
				Converter.mapEligibilityAndStatus(null, null)));
		Assert.assertTrue(Converter.mapEligibilityAndStatus(
				Arrays.asList(new Eligibility("", "", "")), null).isEmpty());
		Map<String, String> m = Maps.newHashMap();
		m.put("preference1-Koulutus-id", "hk1");
		Assert.assertTrue(Converter.mapEligibilityAndStatus(null, m).isEmpty());
	}

	@Test
	public void testaaEligibilitiesMappaustaEiMatsaa() {
		Map<String, String> m = Maps.newHashMap();
		m.put("preference1-Koulutus-id", "hk1");
		Assert.assertTrue(Converter.mapEligibilityAndStatus(
				Arrays.asList(new Eligibility("", "", "")), m).isEmpty());
	}

	@Test
	public void testaaEligibilitiesMappaustaMatsaa() {
		Map<String, String> m = Maps.newHashMap();
		m.put("preference1-Koulutus-id", "hk1");
		Map<String, String> ans = Converter.mapEligibilityAndStatus(
				Arrays.asList(new Eligibility("hk1", "status1", "")), m);
		Assert.assertFalse(ans.isEmpty());
		Assert.assertTrue(ans.size() == 1);
	}

	@Test
	public void testaaEligibilitiesParsintaa() {
		Map<String, String> m = Maps.newHashMap();
		m.put("preference1-Koulutus-id", "hk1");
		Map<String, String> ans = Converter.mapEligibilityAndStatus(
				Arrays.asList(new Eligibility("hk1", "AUTOMATICALLY_CHECKED_ELIGIBLE", "")), m);
		Assert.assertFalse(ans.isEmpty());
		Assert.assertTrue(ans.size() == 1);
		Assert.assertTrue(ans.entrySet().iterator().next().getValue().equals("ELIGIBLE"));
	}

	@Test
	public void testaaEligibilitiesMappaustaMatsaakoKunYlimaaraisiaAvaimia() {
		Map<String, String> m = Maps.newHashMap();
		m.put("preference1-Koulutus-id", "hk1");
		m.put("preference2-Koulutus-id", "hk2");
		Map<String, String> ans = Converter.mapEligibilityAndStatus(
				Arrays.asList(new Eligibility("hk1", "status1", "")), m);
		Assert.assertFalse(ans.isEmpty());
		Assert.assertTrue(ans.size() == 1);
		Assert.assertTrue(ans.entrySet().iterator().next().getKey()
				.equals("preference1-Koulutus-id-eligibility"));
	}

	@Test
	public void testaaEligibilitiesMappaustaMatsaakoKunYlimaaraisiaEligibilityja() {
		Map<String, String> m = Maps.newHashMap();
		m.put("preference1-Koulutus-id", "hk1");
		Map<String, String> ans = Converter.mapEligibilityAndStatus(Arrays
				.asList(new Eligibility("hk1", "status1", ""), new Eligibility(
						"hk2", "status2", "")), m);
		Assert.assertFalse(ans.isEmpty());
		Assert.assertTrue(ans.size() == 1);
		Assert.assertTrue(ans.entrySet().iterator().next().getValue()
				.equals("status1"));
	}

    @Test
    public void testaaArvosanaFiletrointi()
            throws JsonSyntaxException, IOException {
        List<Hakemus> hakemukset = new Gson().fromJson(IOUtils
                .toString(new ClassPathResource("osaaminen_ilman_suorituksia.json")
                        .getInputStream()), new TypeToken<List<Hakemus>>() {
        }.getType());
        Hakemus hakemus = hakemukset.stream()
                .filter(h -> "1.2.246.562.11.00003000803".equals(h.getOid()))
                .distinct().iterator().next();
        HakemusDTO dto = Converter.hakemusToHakemusDTO(hakemus);

        final int yksi = dto.getAvaimet().stream()
                .filter(a -> a.getAvain().startsWith("PK_") || a.getAvain().startsWith("LK_"))
                .collect(Collectors.toList())
                .size();

        // PK_PAATTOTODISTUSVUOSI
        Assert.assertEquals(1, yksi);

        final AvainArvoDTO yleinen_kielitutkinto_sv = dto.getAvaimet().stream()
                .filter(a -> a.getAvain().equals("yleinen_kielitutkinto_sv"))
                .findFirst().get();

        Assert.assertNotNull(yleinen_kielitutkinto_sv);


    }

}
