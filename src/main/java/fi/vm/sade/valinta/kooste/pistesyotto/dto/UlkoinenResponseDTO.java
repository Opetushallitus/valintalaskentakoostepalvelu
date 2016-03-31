package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import java.util.List;

public class UlkoinenResponseDTO {

    private Integer kasiteltyOk;
    private List<VirheDTO> virheet;

    public Integer getKasiteltyOk() {
        return kasiteltyOk;
    }

    public void setKasiteltyOk(Integer kasiteltyOk) {
        this.kasiteltyOk = kasiteltyOk;
    }

    public List<VirheDTO> getVirheet() {
        return virheet;
    }

    public void setVirheet(List<VirheDTO> virheet) {
        this.virheet = virheet;
    }

    @Override
    public String toString() {
        return "UlkoinenResponseDTO{" +
                "kasiteltyOk='" + kasiteltyOk + '\'' +
                ", virheet=" + virheet +
                '}';
    }

}
