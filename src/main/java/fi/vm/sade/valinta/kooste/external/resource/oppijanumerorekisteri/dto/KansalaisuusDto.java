package fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto;

import java.io.Serializable;

public class KansalaisuusDto implements Serializable {
    private static final long serialVersionUID = -1616181528688301217L;

    private String kansalaisuusKoodi;

    public String getKansalaisuusKoodi() {
        return kansalaisuusKoodi;
    }

    public void setKansalaisuusKoodi(String kansalaisuusKoodi) {
        this.kansalaisuusKoodi = kansalaisuusKoodi;
    }
}
