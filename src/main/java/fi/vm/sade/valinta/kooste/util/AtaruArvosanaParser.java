package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtaruArvosanaParser {
  private static final Logger LOG = LoggerFactory.getLogger(AtaruArvosanaParser.class);

  // Toisen asteen ataruhakemuksilta löytyy tällä hetkellä vain peruskoulun arvosanoja
  private static final String prefix = "PK_";

  public static List<AvainArvoDTO> convertValinnaisetKielet(Map<String, AvainArvoDTO> keyValues) {

    List<AvainArvoDTO> r = new ArrayList<>();

    List<AvainArvoDTO> valinnaisetKielet =
        keyValues.values().stream()
            .filter(dto -> dto.getAvain().contains("valinnainen-kieli"))
            .collect(Collectors.toList());
    // Map<String, List<AvainArvoDTO>> grouped =
    // valinnaisetKielet.stream().collect(Collectors.groupingBy(dto ->
    // dto.getAvain().substring(dto.getAvain().length() -1)));
    Map<String, List<AvainArvoDTO>> grouped =
        keyValues.values().stream()
            .filter(dto -> dto.getAvain().contains("valinnainen-kieli"))
            .collect(
                Collectors.groupingBy(
                    dto -> dto.getAvain().substring(dto.getAvain().length() - 1)));
    for (Map.Entry<String, List<AvainArvoDTO>> entry : grouped.entrySet()) {
      try {
        LOG.info("Käsitellään valinnainen kieli: {}", entry);

        // B2
        if (entry.getValue().size() == 3) {
          LOG.info("Sopivasti avain-arvoja!");
        } else {
          LOG.warn("Liian vähän avain-arvoja! {}", entry);
        }
        String aineKey =
            entry.getValue().stream()
                .filter(dto -> dto.getAvain().contains("oppiaine"))
                .findFirst()
                .map(dto -> StringUtils.substringAfterLast(dto.getArvo(), "-"))
                .orElse("")
                .toUpperCase();
        String kieli =
            entry.getValue().stream()
                .filter(dto -> dto.getAvain().startsWith("oppimaara"))
                .findFirst()
                .map(AvainArvoDTO::getArvo)
                .orElse("")
                .toUpperCase();
        String arvosana =
            entry.getValue().stream()
                .filter(dto -> dto.getAvain().startsWith("arvosana"))
                .findFirst()
                .map(dto -> StringUtils.substringAfterLast(dto.getArvo(), "-"))
                .orElse("")
                .toUpperCase();

        if (!arvosana.isEmpty() && !kieli.isEmpty() && !aineKey.isEmpty()) {
          String arvosanaKey = prefix + aineKey;
          r.add(new AvainArvoDTO(arvosanaKey, arvosana));
          if ("0".equals(entry.getKey())) {
            String oppiaineKey = arvosanaKey + "_OPPIAINE";
            r.add(
                new AvainArvoDTO(
                    oppiaineKey,
                    kieli)); // fixme vältetään duplikaattiavainongelma, mutta tästä saattaa olla
            // muuta harmia.
          }
        } else {
          throw new RuntimeException("Tyhjä arvo!");
        }
      } catch (Exception e) {
        LOG.error("Valinnaisen parsiminen ei onnistunut: {}", entry, e);
      }
    }

    return r;
  }

  public static List<AvainArvoDTO> convertAtaruArvosanas(Map<String, AvainArvoDTO> keyValues) {
    List<AvainArvoDTO> r = new ArrayList<>();
    r.addAll(convertValinnaisetKielet(keyValues));
    for (Map.Entry<String, AvainArvoDTO> entry : keyValues.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("arvosana") && !key.contains("valinnainen-kieli")) {
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
      } else if (key.startsWith("oppimaara") && !key.contains("valinnainen-kieli")) {
        LOG.info("handling oppiaine: {}", entry);
        // oppimaara-kieli-B1_group0
        try {
          String kieliKey = StringUtils.substringBetween(key, "oppimaara-kieli-", "_group");
          String arvo = entry.getValue().getArvo();

          // Kerätään oppimäärätieto vain kerran, "group0"-loppuiselta avaimelta.
          if (kieliKey != null && !arvo.isEmpty() && key.endsWith("0")) {
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
