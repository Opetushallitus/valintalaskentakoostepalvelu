package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import java.util.ArrayList;
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
    private String oid;
    private String tarjoajaOid;
    private String hakuOid;
    private List<String> opetuskielet;
    private int valintojenAloituspaikatLkm;
    private String hakukohdeNimiUri;
    private List<ValintakoeDTO> valintakoes = new ArrayList<ValintakoeDTO>();
    private Map<String, String> hakuKausi = new HashMap<String, String>();
    private String hakuVuosi;


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
        return oid;
    }

    public void setHakukohdeOid(String hakukohdeOid) {
        this.oid = hakukohdeOid;
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

    public int getValintojenAloituspaikatLkm() {
        return valintojenAloituspaikatLkm;
    }

    public void setValintojenAloituspaikatLkm(int valintojenAloituspaikatLkm) {
        this.valintojenAloituspaikatLkm = valintojenAloituspaikatLkm;
    }

    public String getHakukohdeNimiUri() {
        return hakukohdeNimiUri;
    }

    public void setHakukohdeNimiUri(String hakukohdeNimiUri) {
        this.hakukohdeNimiUri = hakukohdeNimiUri;
    }

    public List<ValintakoeDTO> getValintakoes() {
        return valintakoes;
    }

    public void setValintakoes(List<ValintakoeDTO> valintakoes) {
        this.valintakoes = valintakoes;
    }


    public String getHakuVuosi() {
        return hakuVuosi;
    }

    public void setHakuVuosi(String hakuVuosi) {
        this.hakuVuosi = hakuVuosi;
    }

    public Map<String, String> getHakuKausi() {
        return hakuKausi;
    }

    public void setHakuKausi(Map<String, String> hakuKausi) {
        this.hakuKausi = hakuKausi;
    }

    public String getHakuOid() {
        return hakuOid;
    }

    public void setHakuOid(String hakuOid) {
        this.hakuOid = hakuOid;
    }
}
