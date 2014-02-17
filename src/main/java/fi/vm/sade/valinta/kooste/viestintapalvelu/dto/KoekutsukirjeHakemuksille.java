package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.util.List;

public class KoekutsukirjeHakemuksille {

	private String letterBodyText;
	private List<String> hakemusOid;

	public List<String> getHakemusOid() {
		return hakemusOid;
	}

	public String getLetterBodyText() {
		return letterBodyText;
	}

	public void setHakemusOid(List<String> hakemusOid) {
		this.hakemusOid = hakemusOid;
	}

	public void setLetterBodyText(String letterBodyText) {
		this.letterBodyText = letterBodyText;
	}
}
