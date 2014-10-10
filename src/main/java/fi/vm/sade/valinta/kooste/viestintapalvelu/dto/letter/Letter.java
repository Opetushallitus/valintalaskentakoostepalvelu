package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

import java.util.Map;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Letter {

	private Osoite addressLabel;
	private String languageCode;
	private String henkilotunnus;
	private Osoite hakijapalveluidenOsoite;
	private Map<String, Object> templateReplacements;
	private LetterContent letterContent;
	private String emailAddress;

	public Letter() {
	}

	public Letter(Osoite addressLabel, Map<String, Object> customLetterContents) {
		this.addressLabel = addressLabel;
		this.templateReplacements = customLetterContents;
	}

	public Letter(Osoite addressLabel, String templateName,
			String languageCode, Map<String, Object> replacements) {
		this.addressLabel = addressLabel;
		this.templateReplacements = replacements;
	}

	public Letter(Osoite addressLabel, String templateName,
			String languageCode, Map<String, Object> replacements,
			String henkilotunnus, Osoite hakijapalveluidenOsoite,
			String emailAddress) {
		this.addressLabel = addressLabel;
		this.templateReplacements = replacements;
		this.hakijapalveluidenOsoite = hakijapalveluidenOsoite;
		this.henkilotunnus = henkilotunnus;
		this.emailAddress = emailAddress;
	}

	public Osoite getHakijapalveluidenOsoite() {
		return hakijapalveluidenOsoite;
	}

	public String getHenkilotunnus() {
		return henkilotunnus;
	}

	public Osoite getAddressLabel() {
		return addressLabel;
	}

	public Map<String, Object> getCustomLetterContents() {
		return this.templateReplacements;
	}

	public Map<String, Object> getTemplateReplacements() {
		return templateReplacements;
	}

	public void setTemplateReplacements(Map<String, Object> templateReplacements) {
		this.templateReplacements = templateReplacements;
	}

	public void setAddressLabel(Osoite addressLabel) {
		this.addressLabel = addressLabel;
	}

	public LetterContent getLetterContent() {
		return letterContent;
	}

	public String getLanguageCode() {
		return languageCode;
	}

	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}

	public void setLetterContent(LetterContent letterContent) {
		this.letterContent = letterContent;
	}

	@Override
	public String toString() {
		return "Letter [addressLabel=" + addressLabel
				+ ", templateReplacements=" + templateReplacements + "]";
	}
}
