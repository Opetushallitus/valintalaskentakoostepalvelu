package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import java.util.Map;

public class Letter {
  private Osoite addressLabel;
  private String languageCode;
  private Map<String, Object> templateReplacements;
  private LetterContent letterContent;
  private String emailAddress;
  private String personOid;
  private String applicationOid;
  private boolean skipIPosti = false;
  private String emailAddressEPosti;

  public Letter() {}

  public Letter(Osoite addressLabel, Map<String, Object> customLetterContents) {
    this.addressLabel = addressLabel;
    this.templateReplacements = customLetterContents;
  }

  public Letter(
      Osoite addressLabel,
      String templateName,
      String languageCode,
      Map<String, Object> replacements) {
    this.addressLabel = addressLabel;
    this.templateReplacements = replacements;
  }

  public Letter(
      Osoite addressLabel,
      String templateName,
      String languageCode,
      Map<String, Object> replacements,
      String personOid,
      boolean skipIPosti,
      String emailAddressEPosti,
      String applicationOid) {
    this.addressLabel = addressLabel;
    this.templateReplacements = replacements;
    this.personOid = personOid;
    this.applicationOid = applicationOid;
    this.skipIPosti = skipIPosti;
    this.emailAddressEPosti = emailAddressEPosti;
  }

  public Letter(
      Osoite addressLabel,
      String templateName,
      String languageCode,
      Map<String, Object> replacements,
      String emailAddress) {
    this.addressLabel = addressLabel;
    this.templateReplacements = replacements;
    this.emailAddress = emailAddress;
  }

  public String getEmailAddress() {
    return emailAddress;
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

  public String getPersonOid() {
    return personOid;
  }

  public void setPersonOid(String personOid) {
    this.personOid = personOid;
  }

  public String getApplicationOid() {
    return applicationOid;
  }

  public void setApplicationOid(String applicationOid) {
    this.applicationOid = applicationOid;
  }

  public boolean isSkipIPosti() {
    return skipIPosti;
  }

  public void setSkipIPosti(boolean skipIPosti) {
    this.skipIPosti = skipIPosti;
  }

  @Override
  public String toString() {
    return "Letter [addressLabel="
        + addressLabel
        + ", templateReplacements="
        + templateReplacements
        + ", personOid="
        + personOid
        + ", applicationOid="
        + applicationOid
        + ", skipIPosti="
        + skipIPosti
        + "]";
  }
}
