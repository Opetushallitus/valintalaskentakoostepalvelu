package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.Arrays;
import java.util.Collection;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

public class KelaProsessi extends DokumenttiProsessi {
    public KelaProsessi(String toiminto, Collection<String> hakuOids) {
        super("Kela", toiminto, "", Arrays.asList("kela"));
    }
}
