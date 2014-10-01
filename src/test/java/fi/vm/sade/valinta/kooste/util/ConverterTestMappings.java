package fi.vm.sade.valinta.kooste.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Maps;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Eligibility;

public class ConverterTestMappings {

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

}
