package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import fi.vm.sade.valinta.kooste.util.KieliUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Metadataa luotavan dokumentin mukauttamiseen
 */
public class DokumentinLisatiedot {

	private String tag;
	private String letterBodyText;
	private String languageCode;
	private List<String> hakemusOids;
	private List<String> valintakoeTunnisteet;

	public DokumentinLisatiedot() {}
	public DokumentinLisatiedot(List<String> valintakoeTunnisteet, String tag, String letterBodyText, String languageCode, List<String> hakemusOids) {
		this.tag = tag;
		this.letterBodyText = letterBodyText;
		this.languageCode = languageCode;
		this.hakemusOids = hakemusOids;
		this.valintakoeTunnisteet = valintakoeTunnisteet;
	}
	public String getTag() {
		return tag;
	}

	public void setValintakoeTunnisteet(List<String> valintakoeTunnisteet) {
		this.valintakoeTunnisteet = valintakoeTunnisteet;
	}

	public String getLanguageCode() {
		if (StringUtils.isBlank(languageCode)) {
			return StringUtils.EMPTY;
		}
		return KieliUtil.normalisoiKielikoodi(languageCode);
	}

	public List<String> getValintakoeTunnisteet() {
		return valintakoeTunnisteet;
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
