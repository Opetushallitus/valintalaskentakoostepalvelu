package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import java.util.List;

public class HakemusDTO {

    private String hakemusOid;
    private String henkiloOid;
    private List<ValintakoeDTO> valintakokeet;

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
