package fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto;

import java.util.List;

public class Valintapisteet {

    private String hakemusOID;
    private List<Piste> pisteet;

    public List<Piste> getPisteet() {
        return pisteet;
    }

    public String getHakemusOID() {
        return hakemusOID;
    }
}
