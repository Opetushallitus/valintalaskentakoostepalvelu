package fi.vm.sade.valinta.kooste.external.resource.koodisto.dto;

import fi.vm.sade.service.valintaperusteet.dto.KoodiDTO;

import java.util.List;

public class Koodi {

    private String koodiArvo;
    private String tila;
    private int versio;
    //private int version;
    private String voimassaAlkuPvm;
    private String voimassaLoppuPvm;
    private List<Metadata> metadata;

    public void setMetadata(List<Metadata> metadata) {
        this.metadata = metadata;
    }

    public int getVersio() {
        return versio;
    }

    public String getKoodiArvo() {
        return koodiArvo;
    }

    public String getTila() {
        return tila;
    }

    public String getVoimassaAlkuPvm() {
        return voimassaAlkuPvm;
    }

    public String getVoimassaLoppuPvm() {
        return voimassaLoppuPvm;
    }

    public List<Metadata> getMetadata() {
        return metadata;
    }

}
