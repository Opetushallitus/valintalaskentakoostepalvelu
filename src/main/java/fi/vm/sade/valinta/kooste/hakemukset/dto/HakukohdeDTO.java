package fi.vm.sade.valinta.kooste.hakemukset.dto;

import java.io.Serializable;
import java.util.List;

public class HakukohdeDTO implements Serializable {

    private static final long serialVersionUID = 3384740333940585758L;

    private String hakukohdeOid;

    private List<ValintakoeDTO> valintakokeet;

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public void setHakukohdeOid(String hakukohdeOid) {
        this.hakukohdeOid = hakukohdeOid;
    }

    public List<ValintakoeDTO> getValintakokeet() {
        return valintakokeet;
    }

    public void setValintakokeet(List<ValintakoeDTO> valintakokeet) {
        this.valintakokeet = valintakokeet;
    }

    @Override
    public String toString() {
        return "HakukohdeDTO{" +
                "hakukohdeOid='" + hakukohdeOid + '\'' +
                ", valintakokeet=" + valintakokeet +
                '}';
    }
}
