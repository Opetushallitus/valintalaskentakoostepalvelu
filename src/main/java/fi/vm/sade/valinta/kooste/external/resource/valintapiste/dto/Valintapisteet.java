package fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static ApplicationAdditionalDataDTO toAdditionalData(Valintapisteet v) {
        Map<String, String> immutableAdditionalData = v.getPisteet().stream().flatMap(p ->
                Stream.of(
                        Pair.of(p.getTunniste(), p.getArvo()),
                        Pair.of(withOsallistuminenSuffix(p.getTunniste()), p.getOsallistuminen().toString())
                )
        ).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));


        return new ApplicationAdditionalDataDTO(
                v.getHakemusOID(),
                v.getOppijaOID(),
                v.getEtunimet(),
                v.getSukunimi(),
                new HashMap<>(immutableAdditionalData)
        );
    };
    public static String withOsallistuminenSuffix(String tunniste) {
        return new StringBuilder(tunniste).append("-OSALLISTUMINEN").toString();
    }
}
