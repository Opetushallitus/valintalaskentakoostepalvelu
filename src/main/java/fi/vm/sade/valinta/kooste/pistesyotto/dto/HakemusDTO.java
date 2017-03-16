package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import io.swagger.annotations.ApiModel;

import java.util.List;

@ApiModel(value = "valinta.kooste.pistesyotto.dto.HakemusDTO", description = "Yhden hakemuksen tiedot")
public class HakemusDTO {
    private String hakemusOid;
    private String henkiloOid;
    private List<ValintakoeDTO> valintakokeet;

    public HakemusDTO() {
    }

    public HakemusDTO(String hakemusOid, String henkiloOid, List<ValintakoeDTO> valintakokeet) {
        this.hakemusOid = hakemusOid;
        this.henkiloOid = henkiloOid;
        this.valintakokeet = valintakokeet;
    }

    public String getHakemusOid() {
        return hakemusOid;
    }

    public void setHakemusOid(String hakemusOid) {
        this.hakemusOid = hakemusOid;
    }

    public String getHenkiloOid() {
        return henkiloOid;
    }

    public void setHenkiloOid(String henkiloOid) {
        this.henkiloOid = henkiloOid;
    }

    public List<ValintakoeDTO> getValintakokeet() {
        return valintakokeet;
    }

    public void setValintakokeet(List<ValintakoeDTO> valintakokeet) {
        this.valintakokeet = valintakokeet;
    }

    @Override
    public String toString() {
        return "HakemusDTO{" +
                "hakemusOid='" + hakemusOid + '\'' +
                ", henkiloOid='" + henkiloOid + '\'' +
                ", valintakokeet=" + valintakokeet +
                '}';
    }
}
