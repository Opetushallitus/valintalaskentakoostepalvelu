package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;

public class LaskentaActorWrapper implements Laskenta {

	private final String uuid;
	private final String hakuOid;
	private final LaskentaActor laskentaActor;

	public LaskentaActorWrapper(String uuid, String hakuOid,
			LaskentaActor laskentaActor) {
		this.uuid = uuid;
		this.hakuOid = hakuOid;
		this.laskentaActor = laskentaActor;
	}

	public String getHakuOid() {
		return hakuOid;
	}

	public String getUuid() {
		return uuid;
	}

	public boolean isOsittainenLaskenta() {
		return false;
	}

	public boolean isValmis() {
		return false;
	}

	public void lopeta() {
		laskentaActor.lopeta();
	}
}
