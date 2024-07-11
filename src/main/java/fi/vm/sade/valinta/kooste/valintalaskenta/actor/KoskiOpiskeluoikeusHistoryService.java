package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static fi.vm.sade.service.valintaperusteet.dto.model.Funktionimi.ITEROIAMMATILLISETTUTKINNOT;
import static fi.vm.sade.service.valintaperusteet.dto.model.Funktionimi.ITEROIAMMATILLISETTUTKINNOT_LEIKKURIPVM_PARAMETRI;
import static java.io.File.separator;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import fi.vm.sade.service.valintaperusteet.dto.SyoteparametriDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetFunktiokutsuDTO;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KoskiOpiskeluoikeusHistoryService {
  private static final Logger LOG =
      LoggerFactory.getLogger(KoskiOpiskeluoikeusHistoryService.class);

  private static final DateTimeFormatter FINNISH_DATE_FORMAT =
      DateTimeFormatter.ofPattern("d.M.yyyy");
  private final Map<String, JsonElement> ohittavatOpiskeluoikeudetOideittain;
  private static final Gson GSON = new Gson();

  public KoskiOpiskeluoikeusHistoryService() {
    this.ohittavatOpiskeluoikeudetOideittain = lueOhitettavatOpiskeluoikeudetKonfiguraatiosta();
    LOG.info(
        String.format(
            "Saatiin %d ohitettavaa opiskeluoikeutta: %s",
            ohittavatOpiskeluoikeudetOideittain.size(),
            ohittavatOpiskeluoikeudetOideittain.keySet()));
  }

  LocalDate etsiKoskiDatanLeikkuriPvm(
      List<ValintaperusteetDTO> valintaperusteetDTOS, String hakukohdeOid) {
    List<String> leikkuriPvmMerkkijonot =
        etsiTutkintojenIterointiFunktioKutsut(valintaperusteetDTOS)
            .flatMap(
                tutkintojenIterointiFunktio ->
                    tutkintojenIterointiFunktio.getSyoteparametrit().stream()
                        .filter(
                            parametri ->
                                ITEROIAMMATILLISETTUTKINNOT_LEIKKURIPVM_PARAMETRI.equals(
                                        parametri.getAvain())
                                    && StringUtils.isNotBlank(parametri.getArvo()))
                        .map(SyoteparametriDTO::getArvo))
            .collect(Collectors.toList());
    LocalDate kaytettavaLeikkuriPvm =
        leikkuriPvmMerkkijonot.stream()
            .map(pvm -> LocalDate.parse(pvm, FINNISH_DATE_FORMAT))
            .max(Comparator.naturalOrder())
            .orElse(LocalDate.now());
    LOG.info(
        String.format(
            "Saatiin hakukohteen %s valintaperusteista Koski-datan leikkuripäivämäärät %s. Käytetään leikkuripäivämääränä arvoa %s.",
            hakukohdeOid,
            leikkuriPvmMerkkijonot,
            FINNISH_DATE_FORMAT.format(kaytettavaLeikkuriPvm)));
    return kaytettavaLeikkuriPvm;
  }

  static Stream<ValintaperusteetFunktiokutsuDTO> etsiTutkintojenIterointiFunktioKutsut(
      List<ValintaperusteetDTO> valintaperusteetDTOS) {
    return valintaperusteetDTOS.stream()
        .flatMap(
            valintaperusteetDTO ->
                valintaperusteetDTO.getValinnanVaihe().getValintatapajono().stream())
        .flatMap(jono -> jono.getJarjestyskriteerit().stream())
        .flatMap(
            kriteeri ->
                etsiFunktiokutsutRekursiivisesti(
                    kriteeri.getFunktiokutsu(),
                    fk -> ITEROIAMMATILLISETTUTKINNOT.equals(fk.getFunktionimi()))
                    .stream());
  }

  private static List<ValintaperusteetFunktiokutsuDTO> etsiFunktiokutsutRekursiivisesti(
      ValintaperusteetFunktiokutsuDTO juuriFunktioKutsu,
      Predicate<ValintaperusteetFunktiokutsuDTO> predikaatti) {
    List<ValintaperusteetFunktiokutsuDTO> tulokset = new LinkedList<>();
    if (predikaatti.test(juuriFunktioKutsu)) {
      tulokset.add(juuriFunktioKutsu);
    }
    tulokset.addAll(
        juuriFunktioKutsu.getFunktioargumentit().stream()
            .flatMap(
                argumentti ->
                    etsiFunktiokutsutRekursiivisesti(argumentti.getFunktiokutsu(), predikaatti)
                        .stream())
            .collect(Collectors.toSet()));
    return tulokset;
  }

  private static Map<String, JsonElement> lueOhitettavatOpiskeluoikeudetKonfiguraatiosta() {
    try {
      String kotihakemisto = System.getProperty("user.home");
      File ohitusTiedostojenHakemisto =
          new File(
              String.format(
                  "%s%soph-configuration%svalintalaskentakoostepalvelu%skoski-ohitukset",
                  kotihakemisto, separator, separator, separator));
      if (!ohitusTiedostojenHakemisto.exists()) {
        LOG.info(
            String.format(
                "Ei löydy ohitettavien opiskeluoikeuksien hakemistoa polusta '%s', "
                    + "joten käytetään Koski-data sellaisena kuin se tulee.",
                ohitusTiedostojenHakemisto.getAbsolutePath()));
        return Collections.emptyMap();
      }
      final File[] ohitustiedostot = ohitusTiedostojenHakemisto.listFiles();
      if (ohitustiedostot == null || ohitustiedostot.length == 0) {
        LOG.info(
            String.format(
                "Ohitettavien opiskeluoikeuksien hakemistoa polussa '%s' on tyhjä, "
                    + "joten käytetään Koski-data sellaisena kuin se tulee.",
                ohitusTiedostojenHakemisto.getAbsolutePath()));
        return Collections.emptyMap();
      }

      return Arrays.stream(ohitustiedostot)
          .collect(
              Collectors.toMap(
                  t -> t.getName().replace(".json", ""),
                  t -> {
                    LOG.info(String.format("Luetaan tiedosto '%s'", t.getAbsolutePath()));
                    return lueJsoniksi(t);
                  }));
    } catch (Exception e) {
      LOG.error(
          "Ongelma luettaessa JSON-ohitustiedostoja. Käytetään Koski-data sellaisena kuin se tulee");
      return Collections.emptyMap();
    }
  }

  private static JsonElement lueJsoniksi(File tiedosto) {
    try {
      return GSON.fromJson(FileUtils.readFileToString(tiedosto, UTF_8), JsonElement.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
