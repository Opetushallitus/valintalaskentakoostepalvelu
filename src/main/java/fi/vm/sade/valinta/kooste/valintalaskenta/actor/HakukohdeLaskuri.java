package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class HakukohdeLaskuri {
	private static final Logger LOG = LoggerFactory
			.getLogger(HakukohdeLaskuri.class);
	private final BloomFilter<CharSequence> duplicateChecker;
	private final AtomicInteger laskuri;
	private final int yhteensa;

	public HakukohdeLaskuri(int hakukohteita) {
		this.yhteensa = hakukohteita;
		this.laskuri = new AtomicInteger(hakukohteita);
		this.duplicateChecker = BloomFilter.create(
				Funnels.stringFunnel(Charset.forName("UTF-8")), hakukohteita);
	}

	public int getYhteensa() {
		return yhteensa;
	}

	public boolean done(String hakukohdeOid) {
		try {
			if (!duplicateChecker.put(hakukohdeOid)) {
				LOG.error("Hakukohde {} saattoi olla jo merkittyna valmiiksi!",
						hakukohdeOid);
			}
		} catch (Exception e) {
			LOG.error("Bloomfilterin kutsu epaonnistui! {}", e.getMessage());
		}
		int l = laskuri.getAndDecrement();
		if (l < 0) {
			LOG.error(
					"Hakukohteita merkitty valmiiksi odotettua enemman! {}/{} eli ylimaaraisia merkintoja on {}",
					(-l) + yhteensa, yhteensa, -l);
			return false;
		}
		return l == 0;
	}

	public boolean isDone() {
		return laskuri.get() <= 0;
	}

	public boolean isOverDone() {
		return laskuri.get() < 0;
	}
}
