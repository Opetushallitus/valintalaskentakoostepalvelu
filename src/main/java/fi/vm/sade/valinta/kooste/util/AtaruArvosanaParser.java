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

  public static List<AvainArvoDTO> convertAtaruArvosanas(
      Map<String, AvainArvoDTO> keyValues, boolean isLukio) {
    String prefix = "PK_";
    if (isLukio) {
      prefix = "LK_";
    }
    LOG.info("convertAtaruArvosanas: {}", keyValues);
    List<AvainArvoDTO> r = new ArrayList<>();
    for (Map.Entry<String, AvainArvoDTO> entry : keyValues.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("arvosana")) {
        try {
          // String aineKey = key.split("-")[1].split("_")[0];
          String aineKey = StringUtils.substringBetween(key, "arvosana-", "_group");
          if (aineKey.equals("A")) {
            aineKey = "AI";
          }
          String jarj = "";
          if (!key.endsWith("0")) {
            jarj = "_VAL" + key.substring(key.length() - 1);
          }
          String aine = prefix + aineKey + jarj;
          String arvosana = StringUtils.substringAfterLast(entry.getValue().getArvo(), "-");
          LOG.debug("key " + key + ", result " + aineKey + jarj);
          r.add(new AvainArvoDTO(aine, arvosana));

        } catch (Exception e) {
          LOG.error("Vikaan meni: " + e.getMessage());
        }
      }
    }
    LOG.info("result: {}", r);

    return r;
  }
}
