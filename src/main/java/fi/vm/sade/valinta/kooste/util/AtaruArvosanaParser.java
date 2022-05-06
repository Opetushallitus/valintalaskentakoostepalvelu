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

  // FI 	Suomi äidinkielenä 	1
  // FI_2 	Suomi toisena kielenä 	1
  // FI_SE 	Suomi saamenkielisille 	1
  // FI_VK 	Suomi viittomakielisille 	1
  // RI 	Romani äidinkielenä 	1
  // SE 	Saame äidinkielenä 	1
  // SV 	Ruotsi äidinkielenä 	1
  // SV_2 	Ruotsi toisena kielenä 	1
  // SV_VK 	Ruotsi viittomakielisille 	1
  // VK 	Viittomakieli äidinkielenä 	1
  // XX 	Muu oppilaan äidinkieli 	1

  //  val aidinkieli = Map(
  //    "AI1" -> "FI",
  //    "AI2" -> "SV",
  //    "AI3" -> "SE",
  //    "AI4" -> "RI",
  //    "AI5" -> "VK",
  //    "AI6" -> "XX",
  //    "AI7" -> "FI_2",
  //    "AI8" -> "SV_2",
  //    "AI9" -> "FI_SE",
  //    "AI10" -> "XX",
  //    "AI11" -> "FI_VK",
  //    "AI12" -> "SV_VK",
  //    "AIAI" -> "XX"
  //  )

  // :options          [{:label (:suomi-aidinkielena texts/translation-mapping)
  //                           :value "suomi-aidinkielena"}
  //                          {:label (:suomi-toisena-kielena texts/translation-mapping)
  //                           :value "suomi-toisena-kielena"}
  //                          {:label (:suomi-viittomakielisille texts/translation-mapping)
  //                           :value "suomi-viittomakielisille"}
  //                          {:label (:suomi-saamenkielisille texts/translation-mapping)
  //                           :value "suomi-saamenkielisille"}
  //                          {:label (:ruotsi-aidinkielena texts/translation-mapping)
  //                           :value "ruotsi-aidinkielena"}
  //                          {:label (:ruotsi-toisena-kielena texts/translation-mapping)
  //                           :value "ruotsi-toisena-kielena"}
  //                          {:label (:ruotsi-viittomakielisille texts/translation-mapping)
  //                           :value "ruotsi-viittomakielisille"}
  //                          {:label (:saame-aidinkielena texts/translation-mapping)
  //                           :value "saame-aidinkielena"}
  //                          {:label (:romani-aidinkielena texts/translation-mapping)
  //                           :value "romani-aidinkielena"}
  //                          {:label (:viittomakieli-aidinkielena texts/translation-mapping)
  //                           :value "viittomakieli-aidinkielena"}
  //                          {:label (:muu-oppilaan-aidinkieli texts/translation-mapping)
  //                           :value "muu-oppilaan-aidinkieli"}]})

  public static String convertAtaruAidinkieliValue(String valueFromAtaru) {
    switch (valueFromAtaru) {
      case "suomi-aidinkielena":
        return "FI";
      case "suomi-toisena-kielena":
        return "FI_2";
      case "suomi-viittomakielisille":
        return "FI_VK";
      case "suomi-saamenkielisille":
        return "FI_SE";
      case "ruotsi-aidinkielena":
        return "SV";
      case "ruotsi-toisena-kielena":
        return "SV_2";
      case "ruotsi-viittomakielisille":
        return "SV_VK";
      case "saame-aidinkielena":
        return "SE";
      case "romani-aidinkielena":
        return "RI";
      case "viittomakieli-aidinkielena":
        return "VK";
      case "muu-oppilaan-aidinkieli":
        return "XX";
    }
    return "XX";
  }

  public static List<AvainArvoDTO> convertValinnaisetKielet(Map<String, AvainArvoDTO> keyValues) {

    List<AvainArvoDTO> r = new ArrayList<>();

    Map<String, List<AvainArvoDTO>> grouped =
        keyValues.values().stream()
            .filter(
                dto ->
                    dto.getAvain().contains("valinnainen-kieli") && !dto.getAvain().contains("-a-"))
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

        String valSuffix = "";
        if (!"0".equals(entry.getKey())) {
          valSuffix = "_VAL" + (Integer.parseInt(entry.getKey()));
        }

        if (!arvosana.isEmpty() && !kieli.isEmpty() && !aineKey.isEmpty()) {
          String arvosanaKey = prefix + aineKey + valSuffix;
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
          boolean isAidinkieli = StringUtils.startsWith(key, "oppimaara-a_group");
          String arvo = entry.getValue().getArvo();
          if (isAidinkieli) {
            kieliKey = "AI";
            arvo = convertAtaruAidinkieliValue(arvo);
          }
          // Kerätään oppimäärätieto vain kerran, "group0"-loppuiselta avaimelta.
          if (kieliKey != null && !arvo.isEmpty() && key.endsWith("0")) {
            String oppiaineKey = prefix + kieliKey + "_OPPIAINE";
            r.add(new AvainArvoDTO(oppiaineKey, arvo));
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
