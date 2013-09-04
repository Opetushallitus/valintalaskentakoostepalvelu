package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Koska sijoittelulla ei ole omaa API:a!
 */
public class SijoitteluAjo implements Serializable {

    private String id;
    private Long sijoitteluajoId;
    private String hakuOid;
    private Long startMils;
    private Long endMils;
    private List<HakukohdeItem> hakukohteet = new ArrayList<HakukohdeItem>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getSijoitteluajoId() {
        return sijoitteluajoId;
    }

    public void setSijoitteluajoId(Long sijoitteluajoId) {
        this.sijoitteluajoId = sijoitteluajoId;
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

    public List<HakukohdeItem> getHakukohteet() {
        return hakukohteet;
    }

    public void setHakukohteet(List<HakukohdeItem> hakukohteet) {
        this.hakukohteet = hakukohteet;
    }

    public String getHakuOid() {
        return hakuOid;
    }

    public void setHakuOid(String hakuOid) {
        this.hakuOid = hakuOid;
    }
}