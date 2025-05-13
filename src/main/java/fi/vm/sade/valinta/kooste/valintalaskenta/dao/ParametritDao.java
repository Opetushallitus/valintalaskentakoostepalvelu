package fi.vm.sade.valinta.kooste.valintalaskenta.dao;

import java.util.Map;

public interface ParametritDao {

  /**
   * Lukee parametrit tietokannasta
   *
   * @return parametrien arvot
   */
  Map<String, String> lueParametrit();
}
