package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Laskenta {
	private final static String NIMI_FORMAT = "Laskenta hakuOid(%s) uuid(%s) hakukohteita(%s/%s)";
	private final String uuid;
	private final String hakuOid;
	private final boolean kaytaSeurantaa;
	private final int hakukohteita;
	private final AtomicBoolean lopetusehto;
	private final AtomicInteger tehty = new AtomicInteger(0);

	public Laskenta(String uuid, String hakuOid, int hakukohteita,
			AtomicBoolean lopetusehto) {
		this.uuid = uuid;
		this.hakuOid = hakuOid;
		this.kaytaSeurantaa = true;
		this.hakukohteita = hakukohteita;
		this.lopetusehto = lopetusehto;
	}

	public Laskenta(String uuid, String hakuOid, int hakukohteita,
			boolean kaytaSeurantaa, AtomicBoolean lopetusehto) {
		this.uuid = uuid;
		this.hakuOid = hakuOid;
		this.kaytaSeurantaa = kaytaSeurantaa;
		this.hakukohteita = hakukohteita;
		this.lopetusehto = lopetusehto;
	}

	public AtomicBoolean getLopetusehto() {
		return lopetusehto;
	}

	public boolean isKaytaSeurantaa() {
		return kaytaSeurantaa;
	}

	/**
	 * @return true jos laskenta valmis
	 */
	public boolean merkkaaHakukohdeTehdyksi() {
		return tehty.incrementAndGet() == hakukohteita;
	}

	public String getHakuOid() {
		return hakuOid;
	}

	public String getUuid() {
		return uuid;
	}

	public String toString() {
		return String.format(NIMI_FORMAT, hakuOid, uuid, tehty.get(),
				hakukohteita);
	}
}
