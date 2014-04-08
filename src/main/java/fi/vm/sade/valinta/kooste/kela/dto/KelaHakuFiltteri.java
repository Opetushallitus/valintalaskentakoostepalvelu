package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.List;

public class KelaHakuFiltteri {

	private List<String> hakuOids;
	private String aineisto;

	public String getAineisto() {
		return aineisto;
	}

	public void setAineisto(String aineisto) {
		this.aineisto = aineisto;
	}

	public List<String> getHakuOids() {
		return hakuOids;
	}

	public void setHakuOids(List<String> hakuOids) {
		this.hakuOids = hakuOids;
	}
}
