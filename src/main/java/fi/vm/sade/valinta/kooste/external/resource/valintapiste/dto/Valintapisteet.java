package fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto;

import java.util.List;

public class Valintapisteet {

    private String hakemusOID;
    private String oppijaOID;
    private String etunimet;
    private String sukunimi;
    private List<Piste> pisteet;

    public Valintapisteet() {
    }
    public Valintapisteet(String hakemusOID, String oppijaOID, String etunimet, String sukunimi, List<Piste> pisteet) {
        this.hakemusOID = hakemusOID;
        this.oppijaOID = oppijaOID;
        this.etunimet = etunimet;
        this.sukunimi = sukunimi;
        this.pisteet = pisteet;
    }

    public List<Piste> getPisteet() {
        return pisteet;
    }

    public String getHakemusOID() {
        return hakemusOID;
    }

    public String getEtunimet() {
        return etunimet;
    }

    public String getOppijaOID() {
        return oppijaOID;
    }

    public String getSukunimi() {
        return sukunimi;
    }
}
