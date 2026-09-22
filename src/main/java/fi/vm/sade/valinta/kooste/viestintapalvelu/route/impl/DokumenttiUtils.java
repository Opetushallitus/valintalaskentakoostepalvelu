package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.util.UUID;

public class DokumenttiUtils {

  public static String generateId() {
    return UUID.randomUUID().toString();
  }
}
