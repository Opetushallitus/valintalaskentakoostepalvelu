package fi.vm.sade.valinta.kooste.pistesyotto.service;

import fi.vm.sade.valinta.kooste.pistesyotto.dto.TuontiErrorDTO;

import java.util.List;

public class PistesyotonTuontivirhe extends RuntimeException {
    public final List<TuontiErrorDTO> virheet;

    public PistesyotonTuontivirhe(List<TuontiErrorDTO> virheet) {
        this.virheet = virheet;
    }
}
