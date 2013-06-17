package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: kkammone
 * Date: 17.6.2013
 * Time: 10:42
 * To change this template use File | Settings | File Templates.
 */
public class HakukohdeDTO {

    private Map<String, String> tarjoajaNimi = new HashMap<String, String>();
    private Map<String, String> hakukohdeNimi = new HashMap<String, String>();
    private String hakukohdeOid;
    private String tarjoajaOid;
    private List<String > opetuskielet;
    private String valintojenAloituspaikatLkm;


    public Map<String, String> getTarjoajaNimi() {
        return tarjoajaNimi;
    }

    public void setTarjoajaNimi(Map<String, String> tarjoajaNimi) {
        this.tarjoajaNimi = tarjoajaNimi;
    }

    public Map<String, String> getHakukohdeNimi() {
        return hakukohdeNimi;
    }

    public void setHakukohdeNimi(Map<String, String> hakukohdeNimi) {
        this.hakukohdeNimi = hakukohdeNimi;
    }

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public void setHakukohdeOid(String hakukohdeOid) {
        this.hakukohdeOid = hakukohdeOid;
    }

    public String getTarjoajaOid() {
        return tarjoajaOid;
    }

    public void setTarjoajaOid(String tarjoajaOid) {
        this.tarjoajaOid = tarjoajaOid;
    }

    public List<String> getOpetuskielet() {
        return opetuskielet;
    }

    public void setOpetuskielet(List<String> opetuskielet) {
        this.opetuskielet = opetuskielet;
    }

    public String getValintojenAloituspaikatLkm() {
        return valintojenAloituspaikatLkm;
    }

    public void setValintojenAloituspaikatLkm(String valintojenAloituspaikatLkm) {
        this.valintojenAloituspaikatLkm = valintojenAloituspaikatLkm;
    }
}
