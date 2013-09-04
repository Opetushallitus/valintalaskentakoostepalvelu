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
public class Hakijaryhma implements Serializable {

    private Integer prioriteetti;
    private int paikat;
    private String oid;
    private String nimi;
    private List<String> hakijaOid = new ArrayList<String>();

    public List<String> getHakijaOid() {
        return hakijaOid;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getNimi() {
        return nimi;
    }

    public void setNimi(String nimi) {
        this.nimi = nimi;
    }

    public int getPaikat() {
        return paikat;
    }

    public void setPaikat(int paikat) {
        this.paikat = paikat;
    }

    public Integer getPrioriteetti() {
        return prioriteetti;
    }

    public void setPrioriteetti(Integer prioriteetti) {
        this.prioriteetti = prioriteetti;
    }

    @Override
    public String toString() {
        return "Hakijaryhma{" + "prioriteetti=" + prioriteetti + ", paikat=" + paikat + ", oid='" + oid + '\''
                + ", nimi='" + nimi + '\'' + ", hakijaOid=" + hakijaOid + '}';
    }
}
