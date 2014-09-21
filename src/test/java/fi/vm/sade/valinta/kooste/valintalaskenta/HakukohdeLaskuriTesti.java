package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.HakukohdeLaskuri;

/**
 * 
 * @author Jussi Jartamo
 * 
 * 
 */
public class HakukohdeLaskuriTesti {
	private final static Logger LOG = LoggerFactory
			.getLogger(HakukohdeLaskuriTesti.class);

	@Test
	public void testaaHakukohdeLaskuri() throws JsonSyntaxException,
			IOException {
		final Collection<String> oids = new Gson().fromJson(IOUtils
				.toString(new ClassPathResource("oids.json").getInputStream()),
				new TypeToken<List<String>>() {
				}.getType());

		LOG.info("Aloitetaan oidien merkitseminen. Oidien maara on {} ja {}",
				oids.size());

		HakukohdeLaskuri hakukohdeLaskuri = new HakukohdeLaskuri(oids.size());

		oids.forEach(o -> hakukohdeLaskuri.done(o));

		Assert.assertTrue("Hakukohde laskurin tulisi olla valmis!",
				hakukohdeLaskuri.isDone());
		Assert.assertFalse(
				"Hakukohde laskurin ei saisi olla ylikierroksilla eli enemman merkintoja kuin tehtavia!",
				hakukohdeLaskuri.isOverDone());
	}

}
