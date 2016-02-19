package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import java.util.List;

public class UlkoinenResponseDTO {

    private String kasiteltyOk;
    private List<VirheDTO> virheet;

    public String getKasiteltyOk() {
        return kasiteltyOk;
    }

    public void setKasiteltyOk(String kasiteltyOk) {
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
