package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SijoitteluajoDTO implements Serializable {

    private Long sijoitteluajoId;
    private String hakuOid;
    private Long startMils;
    private Long endMils;
    private List<HakukohdeDTO> hakukohteet = new ArrayList<HakukohdeDTO>();

    public Long getSijoitteluajoId() {
        return sijoitteluajoId;
    }

    public void setSijoitteluajoId(Long sijoitteluajoId) {
        this.sijoitteluajoId = sijoitteluajoId;
    }

    public String getHakuOid() {
        return hakuOid;
    }

    public void setHakuOid(String hakuOid) {
        this.hakuOid = hakuOid;
    }

    public Long getStartMils() {
        return startMils;
    }

    public void setStartMils(Long startMils) {
        this.startMils = startMils;
    }

    public Long getEndMils() {
        return endMils;
    }

    public void setEndMils(Long endMils) {
        this.endMils = endMils;
    }

    public List<HakukohdeDTO> getHakukohteet() {
        return hakukohteet;
    }

    public void setHakukohteet(List<HakukohdeDTO> hakukohteet) {
        this.hakukohteet = hakukohteet;
    }
}
