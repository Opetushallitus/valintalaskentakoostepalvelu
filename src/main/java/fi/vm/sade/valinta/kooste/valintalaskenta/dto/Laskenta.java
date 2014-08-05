package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.Util;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaKerrallaRouteImpl;

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
	private final AtomicInteger hakukohteita = new AtomicInteger(0);
	private final AtomicInteger tehty = new AtomicInteger(0);

	public Laskenta(String uuid, String hakuOid) {
		this.uuid = uuid;
		this.hakuOid = hakuOid;
		this.kaytaSeurantaa = true;
	}

	public Laskenta(String uuid, String hakuOid, boolean kaytaSeurantaa) {
		this.uuid = uuid;
		this.hakuOid = hakuOid;
		this.kaytaSeurantaa = kaytaSeurantaa;
	}

	public boolean isKaytaSeurantaa() {
		return kaytaSeurantaa;
	}

	public void setLaskettavienHakukohteidenMaara(int maara) {
		hakukohteita.set(maara);
	}

	/**
	 * @return true jos laskenta valmis
	 */
	public boolean merkkaaHakukohdeTehdyksi() {
		return tehty.incrementAndGet() == hakukohteita.get();
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
