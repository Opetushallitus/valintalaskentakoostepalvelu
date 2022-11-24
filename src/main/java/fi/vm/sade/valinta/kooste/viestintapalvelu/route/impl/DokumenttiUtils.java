package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.util.Date;
import java.util.UUID;
import org.joda.time.DateTime;

public class DokumenttiUtils {

  public static String generateId() {
    return UUID.randomUUID().toString();
  }

  public static Date defaultExpirationDate() {
    return DateTime.now().plusHours(168).toDate();
  }
}
