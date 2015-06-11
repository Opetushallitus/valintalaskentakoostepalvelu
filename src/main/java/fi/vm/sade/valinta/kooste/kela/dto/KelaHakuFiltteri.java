package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.Date;
import java.util.List;

public class KelaHakuFiltteri {

    private List<String> hakuOids;
    private String aineisto;
    private Date alkupvm;
    private Date loppupvm;

    public String getAineisto() {
        return aineisto;
    }

    public void setAineisto(String aineisto) {
        this.aineisto = aineisto;
    }

    public List<String> getHakuOids() {
        return hakuOids;
    }

    public void setHakuOids(List<String> hakuOids) {
        this.hakuOids = hakuOids;
    }

    public Date getAlkupvm() {
        return alkupvm;
    }

    public void setAlkupvm(Date alkupvm) {
        this.alkupvm = alkupvm;
    }

    public Date getLoppupvm() {
        return loppupvm;
    }

    public void setLoppupvm(Date loppupvm) {
        this.loppupvm = loppupvm;
    }
}

