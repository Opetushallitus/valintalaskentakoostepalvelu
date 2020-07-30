package fi.vm.sade.valinta.kooste.sijoittelu.dto;

import fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatuloksenTila;
import java.util.List;

public class Valintatulos {
  private String valintatapajonoOid;
  private String hakemusOid;
  private String hakukohdeOid;
  private String hakijaOid;
  private String hakuOid;
  private int hakutoive;
  private ValintatuloksenTila tila = ValintatuloksenTila.KESKEN;
  private IlmoittautumisTila ilmoittautumisTila = IlmoittautumisTila.EI_TEHTY;

  private String ehdollisenHyvaksymisenEhtoKoodi;
  private String ehdollisenHyvaksymisenEhtoFI;
  private String ehdollisenHyvaksymisenEhtoSV;
  private String ehdollisenHyvaksymisenEhtoEN;

  private List<LogEntry> logEntries;

  public IlmoittautumisTila getIlmoittautumisTila() {
    return ilmoittautumisTila;
  }

  public int getHakutoive() {
    return hakutoive;
  }

  public void setHakutoive(int hakutoive) {
    this.hakutoive = hakutoive;
  }

  public String getHakukohdeOid() {
    return hakukohdeOid;
  }

  public void setHakukohdeOid(String hakukohdeOid) {
    this.hakukohdeOid = hakukohdeOid;
  }

  public String getHakemusOid() {
    return hakemusOid;
  }

  public void setHakemusOid(String hakemusOid) {
    this.hakemusOid = hakemusOid;
  }

  public String getHakijaOid() {
    return hakijaOid;
  }

  public void setHakijaOid(String hakijaOid) {
    this.hakijaOid = hakijaOid;
  }

  public ValintatuloksenTila getTila() {
    return tila;
  }

  public void setTila(ValintatuloksenTila tila) {
    this.tila = tila;
  }

  public String getValintatapajonoOid() {
    return valintatapajonoOid;
  }

  public void setValintatapajonoOid(String valintatapajonoOid) {
    this.valintatapajonoOid = valintatapajonoOid;
  }

  public String getHakuOid() {
    return hakuOid;
  }

  public void setHakuOid(String hakuOid) {
    this.hakuOid = hakuOid;
  }

  public List<LogEntry> getLogEntries() {
    return logEntries;
  }

  public void setLogEntries(List<LogEntry> logEntries) {
    this.logEntries = logEntries;
  }

  public String getEhdollisenHyvaksymisenEhtoKoodi() {
    return ehdollisenHyvaksymisenEhtoKoodi;
  }

  public void setEhdollisenHyvaksymisenEhtoKoodi(String ehdollisenHyvaksymisenEhtoKoodi) {
    this.ehdollisenHyvaksymisenEhtoKoodi = ehdollisenHyvaksymisenEhtoKoodi;
  }

  public String getEhdollisenHyvaksymisenEhtoFI() {
    return ehdollisenHyvaksymisenEhtoFI;
  }

  public void setEhdollisenHyvaksymisenEhtoFI(String ehdollisenHyvaksymisenEhtoFI) {
    this.ehdollisenHyvaksymisenEhtoFI = ehdollisenHyvaksymisenEhtoFI;
  }

  public String getEhdollisenHyvaksymisenEhtoSV() {
    return ehdollisenHyvaksymisenEhtoSV;
  }

  public void setEhdollisenHyvaksymisenEhtoSV(String ehdollisenHyvaksymisenEhtoSV) {
    this.ehdollisenHyvaksymisenEhtoSV = ehdollisenHyvaksymisenEhtoSV;
  }

  public String getEhdollisenHyvaksymisenEhtoEN() {
    return ehdollisenHyvaksymisenEhtoEN;
  }

  public void setEhdollisenHyvaksymisenEhtoEN(String ehdollisenHyvaksymisenEhtoEN) {
    this.ehdollisenHyvaksymisenEhtoEN = ehdollisenHyvaksymisenEhtoEN;
  }
}
