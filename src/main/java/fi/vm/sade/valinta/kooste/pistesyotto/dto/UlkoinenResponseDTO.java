package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import java.io.Serializable;
import java.util.List;

public class UlkoinenResponseDTO implements Serializable {

    private static final long serialVersionUID = 4370237271186029557L;

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
}
