package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

public class SijoittelultaEiSisaltoaPoikkeus extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public SijoittelultaEiSisaltoaPoikkeus(String viesti) {
    super(viesti);
  }
}
