package fi.vm.sade.valinta.kooste.pistesyotto.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class KielikoepistetietojenTarkistusTest {

  private static ApplicationAdditionalDataDTO pistetiedot(String... avaimetJaArvot) {
    Map<String, String> additionalData = new HashMap<>();
    for (int i = 0; i < avaimetJaArvot.length; i += 2) {
      additionalData.put(avaimetJaArvot[i], avaimetJaArvot[i + 1]);
    }
    return new ApplicationAdditionalDataDTO(
        "hakemusOid", "personOid", "Etunimi", "Sukunimi", additionalData);
  }

  @Test
  public void hyvaksyyOsallistuneenTotuusarvoisenTuloksen() {
    assertDoesNotThrow(
        () ->
            AbstractPistesyottoKoosteService.tarkistaKielikoepistetiedot(
                pistetiedot(
                    "kielikoe_fi", "true",
                    "kielikoe_fi-OSALLISTUMINEN", "OSALLISTUI",
                    "kielikoe_sv", "false",
                    "kielikoe_sv-OSALLISTUMINEN", "OSALLISTUI")));
  }

  @Test
  public void hyvaksyyTyhjanTuloksenKunEiOsallistunut() {
    assertDoesNotThrow(
        () ->
            AbstractPistesyottoKoosteService.tarkistaKielikoepistetiedot(
                pistetiedot(
                    "kielikoe_fi", "",
                    "kielikoe_fi-OSALLISTUMINEN", "MERKITSEMATTA",
                    "kielikoe_sv", "",
                    "kielikoe_sv-OSALLISTUMINEN", "EI_OSALLISTUNUT",
                    "kielikoe_en", "",
                    "kielikoe_en-OSALLISTUMINEN", "EI_VAADITA")));
  }

  @Test
  public void eiTarkistaMuitaKuinKielikoeavaimia() {
    assertDoesNotThrow(
        () ->
            AbstractPistesyottoKoosteService.tarkistaKielikoepistetiedot(
                pistetiedot(
                    "paasykoe", "12,5",
                    "paasykoe-OSALLISTUMINEN", "OSALLISTUI",
                    "kielikoe_fi-OSALLISTUMINEN", "OSALLISTUI")));
  }

  @Test
  public void hylkaaOsallistuneenMuunKuinTotuusarvoisenTuloksen() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                AbstractPistesyottoKoosteService.tarkistaKielikoepistetiedot(
                    pistetiedot(
                        "kielikoe_fi", "hyvaksytty",
                        "kielikoe_fi-OSALLISTUMINEN", "OSALLISTUI")));
    assertEquals(
        "Huono kielikokeen kielikoe_fi tulos 'hyvaksytty' hakemuksella hakemusOid", e.getMessage());
  }

  @Test
  public void hylkaaOsallistuneenTyhjanTuloksen() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AbstractPistesyottoKoosteService.tarkistaKielikoepistetiedot(
                pistetiedot("kielikoe_fi", "", "kielikoe_fi-OSALLISTUMINEN", "OSALLISTUI")));
  }

  @Test
  public void hylkaaTuntemattomanOsallistumistiedon() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                AbstractPistesyottoKoosteService.tarkistaKielikoepistetiedot(
                    pistetiedot(
                        "kielikoe_fi", "true",
                        "kielikoe_fi-OSALLISTUMINEN", "OSALLISTUU")));
    assertEquals(
        "Huono kielikokeen kielikoe_fi osallistumistieto 'OSALLISTUU' hakemuksella hakemusOid",
        e.getMessage());
  }

  @Test
  public void hylkaaPuuttuvanOsallistumistiedon() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AbstractPistesyottoKoosteService.tarkistaKielikoepistetiedot(
                pistetiedot("kielikoe_fi", "true")));
  }
}
