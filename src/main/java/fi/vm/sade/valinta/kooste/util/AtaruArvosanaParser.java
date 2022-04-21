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

    LOG.info("convertAtaruArvosanas: {}", keyValues);
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
          Integer.parseInt(arvosana);
          r.add(new AvainArvoDTO(aine, arvosana));
        } catch (Exception e) {
          LOG.error(
              "Virhe ({}) parsittaessa ataruarvosanaa {}. Jatketaan normaalisti, mutta tätä arvosanaa ei oteta huomioon.",
              e.getMessage(),
              entry);
        }
      }
    }
    return r;
  }
}
