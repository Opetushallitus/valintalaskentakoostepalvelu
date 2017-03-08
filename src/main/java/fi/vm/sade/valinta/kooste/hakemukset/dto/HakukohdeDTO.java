package fi.vm.sade.valinta.kooste.hakemukset.dto;

import io.swagger.annotations.ApiModel;

import java.util.List;

@ApiModel(value = "valinta.kooste.hakemukset.dto.Hakukohde", description = "Hakukohde")
public class HakukohdeDTO {

    private String hakukohdeOid;
    private List<ValintakoeDTO> valintakokeet;

    public HakukohdeDTO(String hakukohdeOid, List<ValintakoeDTO> valintakokeet) {
        this.hakukohdeOid = hakukohdeOid;
        this.valintakokeet = valintakokeet;
    }

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
