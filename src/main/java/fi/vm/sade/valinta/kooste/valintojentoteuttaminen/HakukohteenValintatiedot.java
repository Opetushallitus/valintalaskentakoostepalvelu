package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

/** Esittää eri palveluista koostettuja hakukohteen valintatietoja. */
public class HakukohteenValintatiedot {
  public String hakukohdeOid;
  public Boolean hasValintakoe;

  public HakukohteenValintatiedot(String hakukohdeOid, Boolean hasValintakoe) {
    this.hakukohdeOid = hakukohdeOid;
    this.hasValintakoe = hasValintakoe;
  }
}
