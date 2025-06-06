package fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto;

public class ParametritDTO {

  private String target;

  // Valintatulokset siirrettävä sijoitteluun viimeistään
  private ParametriDTO PH_VTSSV;

  // Varasijasäännöt astuvat voimaan
  private ParametriDTO PH_VSSAV;

  // Valintatulosten julkistaminen hakijoille
  private ParametriDTO PH_VTJH;

  // Ehdolliset valinnat raukeavat
  private ParametriDTO PH_EVR;

  // Opiskelijan paikan vastaanotto päättyy
  private ParametriDTO PH_OPVP;

  // Hakijakohtaisen paikan vastaanottoaika
  private ParametriDTO PH_HPVOA;

  // Ilmoittautuminen päättyy
  private ParametriDTO PH_IP;

  // Tarjonnan julkaisun takaraja
  private ParametriDTO PH_TJT;

  // Hakukohteiden lisäämisen ja poistamisen takaraja
  private ParametriDTO PH_HKLPT;

  // Hakukohteiden muokkaamisen takaraja
  private ParametriDTO PH_HKMT;

  // Koekutsujen muodostaminen
  private ParametriDTO PH_KKM;

  // Harkinnanvaraisen valinnan päätösten tallentaminen päättyy
  private ParametriDTO PH_HVVPTP;

  // Koetulosten tallentaminen
  private ParametriDTO PH_KTT;

  // Oppilaitosten virkailijoiden valintapalvelun käyttö estetty
  private ParametriDTO PH_OLVVPKE;

  // Valintalaskennan suorittaminen
  private ParametriDTO PH_VLS;

  // Sijoittelun suorittaminen
  private ParametriDTO PH_SS;

  // Jälkiohjauskirjeen lähettäminen iPostiin
  private ParametriDTO PH_JKLIP;

  // Hakukierros päättyy
  private ParametriDTO PH_HKP;

  // Varasijatäyttö päättyy
  private ParametriDTO PH_VSTP;

  private Boolean synteettisetHakemukset;

  public void setPH_EVR(ParametriDTO PH_EVR) {
    this.PH_EVR = PH_EVR;
  }

  public void setPH_HKLPT(ParametriDTO PH_HKLPT) {
    this.PH_HKLPT = PH_HKLPT;
  }

  public void setPH_HKMT(ParametriDTO PH_HKMT) {
    this.PH_HKMT = PH_HKMT;
  }

  public void setPH_HKP(ParametriDTO PH_HKP) {
    this.PH_HKP = PH_HKP;
  }

  public void setPH_HPVOA(ParametriDTO PH_HPVOA) {
    this.PH_HPVOA = PH_HPVOA;
  }

  public void setPH_HVVPTP(ParametriDTO PH_HVVPTP) {
    this.PH_HVVPTP = PH_HVVPTP;
  }

  public void setPH_IP(ParametriDTO PH_IP) {
    this.PH_IP = PH_IP;
  }

  public void setPH_JKLIP(ParametriDTO PH_JKLIP) {
    this.PH_JKLIP = PH_JKLIP;
  }

  public void setPH_KKM(ParametriDTO PH_KKM) {
    this.PH_KKM = PH_KKM;
  }

  public void setPH_KTT(ParametriDTO PH_KTT) {
    this.PH_KTT = PH_KTT;
  }

  public void setPH_OLVVPKE(ParametriDTO PH_OLVVPKE) {
    this.PH_OLVVPKE = PH_OLVVPKE;
  }

  public void setPH_OPVP(ParametriDTO PH_OPVP) {
    this.PH_OPVP = PH_OPVP;
  }

  public void setPH_SS(ParametriDTO PH_SS) {
    this.PH_SS = PH_SS;
  }

  public void setPH_TJT(ParametriDTO PH_TJT) {
    this.PH_TJT = PH_TJT;
  }

  public void setPH_VLS(ParametriDTO PH_VLS) {
    this.PH_VLS = PH_VLS;
  }

  public void setPH_VSSAV(ParametriDTO PH_VSSAV) {
    this.PH_VSSAV = PH_VSSAV;
  }

  public void setPH_VTJH(ParametriDTO PH_VTJH) {
    this.PH_VTJH = PH_VTJH;
  }

  public void setPH_VTSSV(ParametriDTO PH_VTSSV) {
    this.PH_VTSSV = PH_VTSSV;
  }

  public void setPH_VSTP(ParametriDTO PH_VSTP) {
    this.PH_VSTP = PH_VSTP;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public void setSynteettisetHakemukset(Boolean synteettisetHakemukset) {
    this.synteettisetHakemukset = synteettisetHakemukset;
  }

  public ParametriDTO getPH_EVR() {
    return PH_EVR;
  }

  public ParametriDTO getPH_HPVOA() {
    return PH_HPVOA;
  }

  public ParametriDTO getPH_IP() {
    return PH_IP;
  }

  public ParametriDTO getPH_OPVP() {
    return PH_OPVP;
  }

  public ParametriDTO getPH_VSSAV() {
    return PH_VSSAV;
  }

  public ParametriDTO getPH_VTJH() {
    return PH_VTJH;
  }

  public ParametriDTO getPH_VTSSV() {
    return PH_VTSSV;
  }

  public ParametriDTO getPH_HKLPT() {
    return PH_HKLPT;
  }

  public ParametriDTO getPH_HKMT() {
    return PH_HKMT;
  }

  public ParametriDTO getPH_HKP() {
    return PH_HKP;
  }

  public ParametriDTO getPH_HVVPTP() {
    return PH_HVVPTP;
  }

  public ParametriDTO getPH_JKLIP() {
    return PH_JKLIP;
  }

  public ParametriDTO getPH_KKM() {
    return PH_KKM;
  }

  public ParametriDTO getPH_KTT() {
    return PH_KTT;
  }

  public ParametriDTO getPH_OLVVPKE() {
    return PH_OLVVPKE;
  }

  public ParametriDTO getPH_SS() {
    return PH_SS;
  }

  public ParametriDTO getPH_TJT() {
    return PH_TJT;
  }

  public ParametriDTO getPH_VLS() {
    return PH_VLS;
  }

  public ParametriDTO getPH_VSTP() {
    return PH_VSTP;
  }

  public String getTarget() {
    return target;
  }

  public Boolean getSynteettisetHakemukset() {
    return synteettisetHakemukset;
  }
}
