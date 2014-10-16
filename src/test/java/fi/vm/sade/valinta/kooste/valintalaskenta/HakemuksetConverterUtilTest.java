package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;

public class HakemuksetConverterUtilTest {
	private static final Logger LOG = LoggerFactory
			.getLogger(HakemuksetConverterUtilTest.class);

	@Ignore
	@Test
	public void testaaH() {
		String hakukohdeOid = "hakukohdeOid";
		List<Hakemus> hakemukset = Lists.newArrayList();
		Hakemus h0 = new Hakemus();
		h0.setOid("");
		// h0.setPersonOid("");
		hakemukset.add(h0);
		Map<String, String> hakemusOidToPersonOid;
		try {
			hakemusOidToPersonOid = hakemukset.stream()
			//
					.filter(Objects::nonNull)
					//
					.collect(Collectors.toMap(h -> h.getOid(), h -> {
						String personOid = h.getPersonOid();
						if (personOid == null) {
							// virheellisetHakemukset.add(h.getOid());
							return null;
						}
						return personOid;
					}));
		} catch (Exception e) {
			LOG.error(
					"Hakemukset to personOid mappauksessa virhe hakukohteelle {}. Syy {}!",
					hakukohdeOid, e.getMessage());
			throw e;
		}
	}

	@Ignore
	@Test
	public void testaaHakemuksetConverterUtil() throws JsonSyntaxException,
			IOException {
		List<Hakemus> obj = new Gson().fromJson(IOUtils
				.toString(new ClassPathResource("hakemus/listfull.json")
						.getInputStream()), new TypeToken<List<Hakemus>>() {
		}.getType());

		for (Hakemus h0 : obj) {
			if (h0.getPersonOid() == null) {
				// System.err.println(new GsonBuilder().setPrettyPrinting()
				// .create().toJson(h0));
				// System.err.println(h0.getOid());
			}
		}
		// System.err.println(obj.size());
		// List<Hakemus> hakemukset = Lists.newArrayList();
		// HakemuksetConverterUtil.muodostaHakemuksetDTO("hakukohdeOid", null,
		// Collections.emptyList());
	}

}
