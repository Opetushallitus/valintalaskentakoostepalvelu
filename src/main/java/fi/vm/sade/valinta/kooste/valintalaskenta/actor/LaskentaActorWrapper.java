package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class LaskentaActorWrapper implements Laskenta {

	private final String uuid;
	private final String hakuOid;
	private final boolean osittainen;
	private final LaskentaActor laskentaActor;

	public LaskentaActorWrapper(String uuid, String hakuOid, boolean osittainen, LaskentaActor laskentaActor) {
		this.uuid = uuid;
		this.hakuOid = hakuOid;
		this.osittainen = osittainen;
		this.laskentaActor = laskentaActor;
	}

	public LaskentaActor laskentaActor() {
		return laskentaActor;
	}

	public String getHakuOid() {
		return hakuOid;
	}

	public String getUuid() {
		return uuid;
	}

	public boolean isOsittainenLaskenta() {
		return osittainen;
	}

	public boolean isValmis() {
		return false;
	}

	public void lopeta() {
		laskentaActor.lopeta();
	}
}
