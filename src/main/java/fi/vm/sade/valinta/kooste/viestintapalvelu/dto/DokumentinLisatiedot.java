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
	private List<String> valintakoeOids;

	public DokumentinLisatiedot() {}
	public DokumentinLisatiedot(String tag, String letterBodyText, String languageCode, List<String> hakemusOids, List<String> valintakoeOids) {
		this.tag = tag;
		this.letterBodyText = letterBodyText;
		this.languageCode = languageCode;
		this.hakemusOids = hakemusOids;
		this.valintakoeOids = valintakoeOids;
	}
	public String getTag() {
		return tag;
	}

	public void setValintakoeOids(List<String> valintakoeOids) {
		this.valintakoeOids = valintakoeOids;
	}

	public String getLanguageCode() {
		if (StringUtils.isBlank(languageCode)) {
			return StringUtils.EMPTY;
		}
		return KieliUtil.normalisoiKielikoodi(languageCode);
	}

	public List<String> getValintakoeOids() {
		return valintakoeOids;
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
