package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VastaanottoAikarajaMennytDTO {
    private String hakemusOid;
    private boolean mennyt;

    public String getHakemusOid() {
        return hakemusOid;
    }

    public void setHakemusOid(String hakemusOid) {
        this.hakemusOid = hakemusOid;
    }

    public boolean isMennyt() {
        return mennyt;
    }

    public void setMennyt(boolean mennyt) {
        this.mennyt = mennyt;
    }
}
