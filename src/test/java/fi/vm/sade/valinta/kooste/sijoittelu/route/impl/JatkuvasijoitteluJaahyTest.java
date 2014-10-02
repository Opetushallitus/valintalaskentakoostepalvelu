package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.util.Formatter;

public class JatkuvasijoitteluJaahyTest {
	private final static Logger LOG = LoggerFactory
			.getLogger(JatkuvasijoitteluJaahyTest.class);

	@Test
	public void testaaJaahyEiVanhene() {
		final long jaahy = TimeUnit.HOURS.toMillis(2L);
		ConcurrentHashMap<String, Long> ajossaHakuOids = new ConcurrentHashMap<>();
		ajossaHakuOids.put("hk1", System.currentTimeMillis());
		ajossaHakuOids.forEach((hakuOid, activationTime) -> {
			DateTime activated = new DateTime(activationTime);
			DateTime expires = new DateTime(activationTime)
					.plusMillis((int) jaahy);
			boolean vanheneekoNyt = expires.isBeforeNow()
					|| expires.isEqualNow();
			LOG.info("Aktivoitu {} ja vanhenee {} vanheneeko nyt {}",
					Formatter.paivamaara(activated.toDate()),
					Formatter.paivamaara(expires.toDate()), vanheneekoNyt);
			//
				if (vanheneekoNyt) {
					LOG.info("Jaahy haulle {} vanhentui", hakuOid);
					ajossaHakuOids.remove(hakuOid);
				}
			});
	}
}
