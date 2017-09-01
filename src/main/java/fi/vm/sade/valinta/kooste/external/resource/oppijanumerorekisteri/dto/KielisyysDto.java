package fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto;

import java.io.Serializable;

public class KielisyysDto implements Serializable {
    private static final long serialVersionUID = 7217945009330980201L;

    private String kieliKoodi;
    private String kieliTyyppi;

    public String getKieliKoodi() {
        return kieliKoodi;
    }

    public void setKieliKoodi(String kieliKoodi) {
        this.kieliKoodi = kieliKoodi;
    }

    public String getKieliTyyppi() {
        return kieliTyyppi;
    }

    public void setKieliTyyppi(String kieliTyyppi) {
        this.kieliTyyppi = kieliTyyppi;
    }
}
