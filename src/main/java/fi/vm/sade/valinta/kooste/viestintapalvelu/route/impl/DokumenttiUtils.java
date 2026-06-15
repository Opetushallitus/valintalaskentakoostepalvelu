package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

public class DokumenttiUtils {

  public static String generateId() {
    return UUID.randomUUID().toString();
  }

  public static Date defaultExpirationDate() {
    return Date.from(Instant.now().plus(168, ChronoUnit.HOURS));
  }
}
