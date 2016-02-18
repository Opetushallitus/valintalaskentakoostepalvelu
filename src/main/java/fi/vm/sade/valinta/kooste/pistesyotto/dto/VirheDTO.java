package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import java.io.Serializable;

public class VirheDTO implements Serializable {

    private static final long serialVersionUID = 6591935646238664674L;

    private String hakemusOid;
    private String virhe;

    public String getHakemusOid() {
        return hakemusOid;
    }

    public void setHakemusOid(String hakemusOid) {
        this.hakemusOid = hakemusOid;
    }

    public String getVirhe() {
        return virhe;
    }

    public void setVirhe(String virhe) {
        this.virhe = virhe;
    }

    @Override
    public String toString() {
        return "VirheDTO{" +
                "hakemusOid='" + hakemusOid + '\'' +
                ", virhe='" + virhe + '\'' +
                '}';
    }
}
