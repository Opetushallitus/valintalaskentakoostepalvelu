package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

/** Esittää eri valintojen palveluista koostettuja hakukohde-kohtaisia tietoja. */
public class HakukohteenValintatiedot {
  public Boolean hasValintakoe;

  public HakukohteenValintatiedot(Boolean hasValintakoe) {
    this.hasValintakoe = hasValintakoe;
  }
}
