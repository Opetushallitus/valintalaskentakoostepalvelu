package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.HakukohdeLaskuri;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.PalvelukutsuLaskuri;

/**
 * 
 * @author Jussi Jartamo
 * 
 * 
 */
public class LaskuriTest {
	private final static Logger LOG = LoggerFactory
			.getLogger(LaskuriTest.class);

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
		final AtomicBoolean valmis = new AtomicBoolean(false);
		oids.forEach(o -> {
			if (hakukohdeLaskuri.done(o, "")) {
				if (!valmis.compareAndSet(false, true)) {
					Assert.fail("Laskuri vaitti tyon valmistuneen kahdesti!");
				}
			}
		});
		Assert.assertTrue("Laskurin piti valmistua!", valmis.get());
		Assert.assertTrue("Hakukohde laskurin tulisi olla valmis!",
				hakukohdeLaskuri.isDone());
		Assert.assertFalse(
				"Hakukohde laskurin ei saisi olla ylikierroksilla eli enemman merkintoja kuin tehtavia!",
				hakukohdeLaskuri.isOverDone());
	}

	@Test
	public void testaaPalvelukutsuLaskuri() throws JsonSyntaxException,
			IOException {
		Collection<Object> pks = Arrays.asList(1, 2, 3, 4, 5);
		PalvelukutsuLaskuri pkl = new PalvelukutsuLaskuri(pks.size());

		final AtomicBoolean valmis = new AtomicBoolean(false);
		pks.forEach(o -> {
			if (pkl.isDone(pkl.palvelukutsuSaapui())) {
				if (!valmis.compareAndSet(false, true)) {
					Assert.fail("Laskuri vaitti tyon valmistuneen kahdesti!");
				}
			}
		});
		Assert.assertTrue("Palvelulaskurin piti valmistua!", valmis.get());
		Assert.assertTrue("Palvelukutsulaskurin tulisi olla valmis!",
				pkl.isDone());
		Assert.assertFalse(
				"Palvelukutsulaskurin ei saisi olla ylikierroksilla eli enemman merkintoja kuin tehtavia!",
				pkl.isOverDone());
	}

}
