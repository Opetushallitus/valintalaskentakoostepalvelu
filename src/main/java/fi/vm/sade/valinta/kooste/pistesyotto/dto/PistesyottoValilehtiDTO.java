package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import java.util.List;

public class PistesyottoValilehtiDTO {
    private String lastmodified;
    private List<HakemuksenKoetulosYhteenveto> valintapisteet;

    public PistesyottoValilehtiDTO() {
    }
    public PistesyottoValilehtiDTO(String lastmodified, List<HakemuksenKoetulosYhteenveto> valintapisteet) {
        this.lastmodified = lastmodified;
        this.valintapisteet = valintapisteet;
    }
    public List<HakemuksenKoetulosYhteenveto> getValintapisteet() {
        return valintapisteet;
    }

    public String getLastmodified() {
        return lastmodified;
    }
}
