package fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto;

import java.util.List;

public class Valintapisteet {

    private String hakemusOID;
    private String oppijaOID;
    private String etunimet;
    private String sukunimi;
    private List<Piste> pisteet;

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
