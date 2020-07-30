package fi.vm.sade.valinta.kooste.kela.dto;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import java.util.Arrays;
import java.util.Collection;

public class KelaProsessi extends DokumenttiProsessi {
  public KelaProsessi(String toiminto, Collection<String> hakuOids) {
    super("Kela", toiminto, "", Arrays.asList("kela"));
  }
}
