package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.Collection;

import org.apache.camel.Body;
import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class KelaLuonti {
	private final String uuid;
	private final Collection<String> hakuOids;
	private final String aineistonNimi;
	private final String organisaationNimi;

	public KelaLuonti() {
		this.hakuOids = null;
		this.aineistonNimi = null;
		this.organisaationNimi = null;
		this.uuid = null;
	}

	public KelaLuonti(String uuid, Collection<String> hakuOids,
			String aineistonNimi, String organisaationNimi) {
		this.uuid = uuid;
		this.hakuOids = hakuOids;
		this.aineistonNimi = aineistonNimi;
		this.organisaationNimi = organisaationNimi;
	}

	public String getUuid() {
		return uuid;
	}

	public String getAineistonNimi() {
		return aineistonNimi;
	}

	public Collection<String> getHakuOids() {
		return hakuOids;
	}

	public String getOrganisaationNimi() {
		return organisaationNimi;
	}
}
