package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Sets;

import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoittelu;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class JatkuvasijoitteluDelayerTest {

	@Test
	public void testJatkuvasijoitteluDelayerNow() {
		DelayQueue<DelayedSijoittelu> jatkuvaSijoitteluDelayQueue = new DelayQueue<DelayedSijoittelu>();
		jatkuvaSijoitteluDelayQueue.add(new DelayedSijoittelu("hk", DateTime
				.now().getMillis()));
		Assert.assertNotNull(jatkuvaSijoitteluDelayQueue.poll());
	}

	@Test
	public void testJatkuvasijoitteluDelayerPlusSecond() {
		DelayQueue<DelayedSijoittelu> jatkuvaSijoitteluDelayQueue = new DelayQueue<DelayedSijoittelu>();
		jatkuvaSijoitteluDelayQueue.add(new DelayedSijoittelu("hk", DateTime
				.now().getMillis() + TimeUnit.SECONDS.toMillis(1L)));
		Assert.assertNull(jatkuvaSijoitteluDelayQueue.poll());
	}

	@Test
	public void testJatkuvasijoitteluDelayerMultiplePlusMinusSecond() {
		String COMING = "hk1";
		String LATE = "hk2";
		DelayQueue<DelayedSijoittelu> jatkuvaSijoitteluDelayQueue = new DelayQueue<DelayedSijoittelu>();
		jatkuvaSijoitteluDelayQueue.add(new DelayedSijoittelu(COMING, DateTime
				.now().getMillis() + TimeUnit.SECONDS.toMillis(1L)));
		jatkuvaSijoitteluDelayQueue.add(new DelayedSijoittelu(LATE, DateTime
				.now().getMillis() + TimeUnit.SECONDS.toMillis(-1L)));
		Assert.assertTrue(jatkuvaSijoitteluDelayQueue.poll().getHakuOid()
				.equals(LATE));
		jatkuvaSijoitteluDelayQueue.clear();
		jatkuvaSijoitteluDelayQueue.add(new DelayedSijoittelu(LATE, DateTime
				.now().getMillis() + TimeUnit.SECONDS.toMillis(-1L)));
		jatkuvaSijoitteluDelayQueue.add(new DelayedSijoittelu(COMING, DateTime
				.now().getMillis() + TimeUnit.SECONDS.toMillis(1L)));
		Assert.assertTrue(jatkuvaSijoitteluDelayQueue.poll().getHakuOid()
				.equals(LATE));
	}

	@Test
	public void testJatkuvasijoitteluDelayerRemoving() {
		String NOW1 = "hk1";
		String NOW2 = "hk2";
		DelayedSijoittelu DNOW1 = new DelayedSijoittelu(NOW1, DateTime.now()
				.getMillis() + TimeUnit.SECONDS.toMillis(0L));
		DelayedSijoittelu DNOW2 = new DelayedSijoittelu(NOW2, DateTime.now()
				.getMillis() + TimeUnit.SECONDS.toMillis(0L));
		DelayQueue<DelayedSijoittelu> jatkuvaSijoitteluDelayQueue = new DelayQueue<DelayedSijoittelu>();
		jatkuvaSijoitteluDelayQueue.add(DNOW1);
		jatkuvaSijoitteluDelayQueue.add(DNOW2);
		jatkuvaSijoitteluDelayQueue.remove(DNOW2);
		Assert.assertTrue(jatkuvaSijoitteluDelayQueue.poll().getHakuOid()
				.equals(NOW1));
		jatkuvaSijoitteluDelayQueue.clear();
		jatkuvaSijoitteluDelayQueue.add(DNOW1);
		jatkuvaSijoitteluDelayQueue.add(DNOW2);
		jatkuvaSijoitteluDelayQueue.remove(DNOW1);
		Assert.assertTrue(jatkuvaSijoitteluDelayQueue.poll().getHakuOid()
				.equals(NOW2));
	}

	@Test
	public void testJatkuvasijoitteluDelayerTakingMultiple() {
		String NOW1 = "hk1";
		String NOW2 = "hk2";
		String NOW3 = "hk3";
		DelayedSijoittelu DNOW1 = new DelayedSijoittelu(NOW1, DateTime.now()
				.getMillis() + TimeUnit.SECONDS.toMillis(0L));
		DelayedSijoittelu DNOW2 = new DelayedSijoittelu(NOW2, DateTime.now()
				.getMillis() + TimeUnit.SECONDS.toMillis(0L));
		DelayedSijoittelu DNOW3 = new DelayedSijoittelu(NOW3, DateTime.now()
				.getMillis() + TimeUnit.SECONDS.toMillis(0L));
		DelayQueue<DelayedSijoittelu> jatkuvaSijoitteluDelayQueue = new DelayQueue<DelayedSijoittelu>();
		jatkuvaSijoitteluDelayQueue.add(DNOW1);
		jatkuvaSijoitteluDelayQueue.add(DNOW2);
		jatkuvaSijoitteluDelayQueue.add(DNOW3);

		DelayedSijoittelu DNOWX = jatkuvaSijoitteluDelayQueue.poll();
		DelayedSijoittelu DNOWY = jatkuvaSijoitteluDelayQueue.poll();
		DelayedSijoittelu DNOWZ = jatkuvaSijoitteluDelayQueue.poll();

		Set<String> oids = Sets.newHashSet(NOW1, NOW2, NOW3);
		oids.removeAll(Arrays.asList(DNOWX.getHakuOid(), DNOWY.getHakuOid(),
				DNOWZ.getHakuOid()));
		Assert.assertTrue(oids.isEmpty());
	}
}
