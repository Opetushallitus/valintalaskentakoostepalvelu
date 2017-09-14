package fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto;

import fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO;

public class Piste {
    private String tunniste;
    private String arvo;
    private ValintakoeDTO.Osallistuminen osallistuminen;
    private String tallettaja;

    public ValintakoeDTO.Osallistuminen getOsallistuminen() {
        return osallistuminen;
    }

    public String getArvo() {
        return arvo;
    }

    public String getTallettaja() {
        return tallettaja;
    }

    public String getTunniste() {
        return tunniste;
    }
}
