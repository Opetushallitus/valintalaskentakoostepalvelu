package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Laskenta {
	private final static Logger LOG = LoggerFactory.getLogger(Laskenta.class);
	private final static String NIMI_FORMAT = "Laskenta hakuOid(%s) uuid(%s) hakukohteita(%s/%s)";
	private final String uuid;
	private final String hakuOid;
	private final boolean osittainenLaskenta; // eli ei koko haku, eli esim
												// yksittainen hakukohde tai
												// osajoukko haun hakukohteista
	private final int hakukohteita;
	private final AtomicBoolean lopetusehto;
	private final AtomicInteger tehty = new AtomicInteger(0);
	private final Integer valinnanvaihe;
	private final Boolean valintakoelaskenta;

	public Laskenta(String uuid, String hakuOid, int hakukohteita,
			AtomicBoolean lopetusehto, Integer valinnanvaihe,
			Boolean valintakoelaskenta) {
		this.uuid = uuid;
		this.hakuOid = hakuOid;
		this.hakukohteita = hakukohteita;
		this.lopetusehto = lopetusehto;
		this.osittainenLaskenta = false;
		this.valinnanvaihe = valinnanvaihe;
		this.valintakoelaskenta = valintakoelaskenta;
	}

	public Laskenta(String uuid, String hakuOid, int hakukohteita,
			AtomicBoolean lopetusehto, boolean osittainenLaskenta,
			Integer valinnanvaihe, Boolean valintakoelaskenta) {
		this.uuid = uuid;
		this.hakuOid = hakuOid;
		this.hakukohteita = hakukohteita;
		this.lopetusehto = lopetusehto;
		this.osittainenLaskenta = osittainenLaskenta;
		this.valinnanvaihe = valinnanvaihe;
		this.valintakoelaskenta = valintakoelaskenta;
	}

	public Integer getValinnanvaihe() {
		return valinnanvaihe;
	}

	public Boolean getValintakoelaskenta() {
		return valintakoelaskenta;
	}

	public boolean isOsittainenLaskenta() {
		return osittainenLaskenta;
	}

	public AtomicBoolean getLopetusehto() {
		return lopetusehto;
	}

	/**
	 * @return true jos laskenta valmis
	 */
	public boolean merkkaaHakukohdeTehdyksi() {
		int nyt = tehty.incrementAndGet();
		if (nyt > hakukohteita) {
			LOG.error("Tehdyt tyot ylittaa maariteltyjen toiden maaran laskenta reitilla!");
		}
		return nyt >= hakukohteita;
	}

	public boolean isValmis() {
		return tehty.get() >= hakukohteita || lopetusehto.get();
	}

	public int getHakukohteita() {
		return hakukohteita;
	}

	public String getHakuOid() {
		return hakuOid;
	}

	public String getUuid() {
		return uuid;
	}

	public AtomicInteger getTehty() {
		return tehty;
	}

	public String toString() {
		return String.format(NIMI_FORMAT, hakuOid, uuid, tehty.get(),
				hakukohteita);
	}
}
