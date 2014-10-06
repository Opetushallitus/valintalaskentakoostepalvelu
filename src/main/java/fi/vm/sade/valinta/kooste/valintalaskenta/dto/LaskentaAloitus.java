package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class LaskentaAloitus implements LaskentaInfo {
	private final static Logger LOG = LoggerFactory
			.getLogger(LaskentaAloitus.class);
	private final static String NIMI_FORMAT = "Laskenta hakuOid(%s) uuid(%s) hakukohteita(%s/%s)";
	private final String uuid;
	private final String hakuOid;
	private final boolean osittainenLaskenta; // eli ei koko haku, eli esim
												// yksittainen hakukohde tai
												// osajoukko haun hakukohteista
	private final Integer valinnanvaihe;
	private final Boolean valintakoelaskenta;
	private final boolean valintaryhmalaskenta;
	private final Collection<HakukohdeJaOrganisaatio> hakukohdeDtos;
	private final LaskentaTyyppi tyyppi;

	public LaskentaAloitus(String uuid, String hakuOid, Integer valinnanvaihe,
			Boolean valintakoelaskenta,
			Collection<HakukohdeJaOrganisaatio> hakukohdeDtos,
			LaskentaTyyppi tyyppi) {
		this.uuid = uuid;
		this.hakuOid = hakuOid;
		this.osittainenLaskenta = false;
		this.valinnanvaihe = valinnanvaihe;
		this.valintakoelaskenta = valintakoelaskenta;
		this.hakukohdeDtos = hakukohdeDtos;
		this.valintaryhmalaskenta = false;
		this.tyyppi = tyyppi;
	}

	public LaskentaAloitus(String uuid, String hakuOid,
			boolean osittainenLaskenta, boolean valintaryhmalaskenta,
			Integer valinnanvaihe, Boolean valintakoelaskenta,
			Collection<HakukohdeJaOrganisaatio> hakukohdeDtos,
			LaskentaTyyppi tyyppi) {
		this.uuid = uuid;
		this.hakuOid = hakuOid;
		this.osittainenLaskenta = osittainenLaskenta;
		this.valintaryhmalaskenta = valintaryhmalaskenta;
		this.valinnanvaihe = valinnanvaihe;
		this.valintakoelaskenta = valintakoelaskenta;
		this.hakukohdeDtos = hakukohdeDtos;
		this.tyyppi = tyyppi;
	}

	public LaskentaTyyppi getTyyppi() {
		return tyyppi;
	}

	public Collection<HakukohdeJaOrganisaatio> getHakukohdeDtos() {
		return hakukohdeDtos;
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

	public boolean isValintaryhmaLaskenta() {
		return valintaryhmalaskenta;
	}

	public String getHakuOid() {
		return hakuOid;
	}

	public String getUuid() {
		return uuid;
	}

	public String toString() {
		return String.format(NIMI_FORMAT, hakuOid, uuid);
	}

}
