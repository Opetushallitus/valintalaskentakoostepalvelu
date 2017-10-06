package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import java.util.Map;

public class HenkiloValilehtiDTO {
    private String lastmodified;
    private Map<String, HakemuksenKoetulosYhteenveto> hakukohteittain;

    public HenkiloValilehtiDTO() {

    }
    public HenkiloValilehtiDTO(String lastmodified, Map<String, HakemuksenKoetulosYhteenveto> hakukohteittain) {
        this.lastmodified = lastmodified;
        this.hakukohteittain = hakukohteittain;
    }
    public String getLastmodified() {
        return lastmodified;
    }

    public Map<String, HakemuksenKoetulosYhteenveto> getHakukohteittain() {
        return hakukohteittain;
    }
}
