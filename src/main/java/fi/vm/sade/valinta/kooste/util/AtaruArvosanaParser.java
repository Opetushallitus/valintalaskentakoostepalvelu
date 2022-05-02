package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtaruArvosanaParser {
  private static final Logger LOG = LoggerFactory.getLogger(AtaruArvosanaParser.class);

  public static List<AvainArvoDTO> convertAtaruArvosanas(Map<String, AvainArvoDTO> keyValues) {
    // Toisen asteen ataruhakemuksilta löytyy tällä hetkellä vain peruskoulun arvosanoja
    String prefix = "PK_";

    List<AvainArvoDTO> r = new ArrayList<>();
    for (Map.Entry<String, AvainArvoDTO> entry : keyValues.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("arvosana")) {
        try {
          String aineKey = StringUtils.substringBetween(key, "arvosana-", "_group");
          if (aineKey.equals("A")) {
            aineKey = "AI";
          }
          String valSuffix = "";
          if (!key.endsWith("0")) {
            valSuffix = "_VAL" + key.substring(key.length() - 1);
          }
          String aine = prefix + aineKey + valSuffix;
          String arvosana = StringUtils.substringAfterLast(entry.getValue().getArvo(), "-");
          LOG.debug("key " + key + ", result " + aineKey + valSuffix);
          Integer.parseInt(arvosana); // Just check that the arvosana correctly parses as Integer.
          r.add(new AvainArvoDTO(aine, arvosana));
        } catch (Exception e) {
          LOG.warn(
              "Virhe ({}) parsittaessa ataruarvosanaa {}. Jatketaan normaalisti, mutta tätä arvosanaa ei oteta huomioon.",
              e.getMessage(),
              entry);
        }
      } else if (key.startsWith("oppimaara")) {
        LOG.info("handling oppiaine: {}", entry);
        // oppimaara-kieli-B1_group0
        try {
          String kieliKey = StringUtils.substringBetween(key, "oppimaara-kieli-", "_group");
          String arvo = entry.getValue().getArvo();

          if (kieliKey != null && !arvo.isEmpty()) {
            String oppiaineKey = prefix + kieliKey + "_OPPIAINE";
            r.add(new AvainArvoDTO(oppiaineKey, entry.getValue().getArvo()));
          }
        } catch (Exception e) {
          LOG.warn(
              "Virhe ({}) parsittaessa ataruoppiainetta {}. Jatketaan normaalisti, mutta tätä oppiainetietoa ei oteta huomioon.",
              e.getMessage(),
              entry);
        }
      }
    }
    return r;
  }
}
