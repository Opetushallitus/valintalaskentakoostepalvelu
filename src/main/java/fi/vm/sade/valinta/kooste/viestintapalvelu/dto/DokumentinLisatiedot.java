package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Metadataa luotavan dokumentin mukauttamiseen
 */
public class DokumentinLisatiedot {

	private String tag;
	private String letterBodyText;
	private List<String> hakemusOids;

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

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
