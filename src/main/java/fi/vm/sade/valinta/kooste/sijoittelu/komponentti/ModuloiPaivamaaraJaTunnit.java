package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import fi.vm.sade.valinta.kooste.util.Formatter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuloiPaivamaaraJaTunnit {
  private static final Logger LOG = LoggerFactory.getLogger(ModuloiPaivamaaraJaTunnit.class);

  public static DateTime moduloiSeuraava(DateTime start, DateTime now, int moduloitavaTuntiMaara) {
    // if (now.isBefore(start))
    if (now.isBefore(start.minusHours(1))) {
      LOG.error(
          "Yritettiin moduloida seuraavaa suoritusaikaa vaikka aloitusaika ei ole viela tapahtunut! Aloituspvm {} mutta nyt on {}",
          Formatter.paivamaara(start.toDate()),
          Formatter.paivamaara(now.toDate()));
      throw new RuntimeException(
          "Yritettiin moduloida seuraavaa suoritusaikaa vaikka aloitusaika ei ole viela tapahtunut!");
    }
    int tavoiteTunti =
        seuraavaAskel(start.getHourOfDay(), now.getHourOfDay(), moduloitavaTuntiMaara);
    if (now.getHourOfDay() == tavoiteTunti && now.getMinuteOfHour() > start.getMinuteOfHour()) {
      tavoiteTunti += moduloitavaTuntiMaara;
    }
    return new DateTime(
            now.getYear(),
            now.getMonthOfYear(),
            now.getDayOfMonth(),
            start.getHourOfDay(),
            start.getMinuteOfHour())
        .plusHours(tavoiteTunti - start.getHourOfDay());
  }

  public static int seuraavaAskel(int alkuPiste, int tavoiteAlue, int askel) {
    if (alkuPiste <= tavoiteAlue) {
      int i = alkuPiste;
      while (i < tavoiteAlue) {
        i += askel;
      }
      return i;
    } else {
      int i = alkuPiste;
      while (i >= tavoiteAlue) {
        i -= askel;
      }
      return i + askel;
    }
  }
}
