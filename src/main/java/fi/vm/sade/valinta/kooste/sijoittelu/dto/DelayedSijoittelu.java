package fi.vm.sade.valinta.kooste.sijoittelu.dto;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.util.Formatter;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class DelayedSijoittelu implements Delayed {
	private final Logger LOG = LoggerFactory.getLogger(DelayedSijoittelu.class);
	private final long when;
	private final String hakuOid;

	public DelayedSijoittelu(String hakuOid, long when) {
		this.when = when;
		this.hakuOid = hakuOid;
	}

	public DelayedSijoittelu(String hakuOid, DateTime when) {
		this.when = when.getMillis();
		this.hakuOid = hakuOid;
	}

	public String getHakuOid() {
		return hakuOid;
	}

	public long getWhen() {
		return when;
	}

	public String getWhenAsFormattedString() {
		return Formatter.PVMFORMATTER.format(when);
	}

	public int compareTo(Delayed o) {
		return new Long(getDelay(TimeUnit.MILLISECONDS)).compareTo(new Long(o
				.getDelay(TimeUnit.MILLISECONDS)));
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(when - System.currentTimeMillis(),
				TimeUnit.MILLISECONDS);
	}

}
