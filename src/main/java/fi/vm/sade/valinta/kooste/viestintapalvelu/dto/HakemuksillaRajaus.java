package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.util.List;

public class HakemuksillaRajaus {

	private String letterBodyText;
	private List<String> hakemusOids;

	public List<String> getHakemusOids() {
		if (hakemusOids == null || hakemusOids.isEmpty()) {
			return null;
		}
		return hakemusOids;
	}

	public String getLetterBodyText() {
		return letterBodyText;
	}

	public void setHakemusOids(List<String> hakemusOids) {
		this.hakemusOids = hakemusOids;
	}

	public void setLetterBodyText(String letterBodyText) {
		this.letterBodyText = letterBodyText;
	}
}
