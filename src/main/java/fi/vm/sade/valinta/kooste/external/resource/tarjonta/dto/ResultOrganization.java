package fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto;

import java.util.List;

public class ResultOrganization {
    private String oid;
    private List<ResultHakukohde> tulokset;

    public List<ResultHakukohde> getTulokset() {
        return tulokset;
    }

    public String getOid() {
        return oid;
    }
}
