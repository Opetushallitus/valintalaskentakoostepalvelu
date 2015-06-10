package fi.vm.sade.valinta.kooste.external.resource.haku.dto;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Answers {
    private Map<String, String> henkilotiedot = new HashMap<String, String>();
    private Map<String, String> lisatiedot = new HashMap<String, String>();
    private Map<String, String> hakutoiveet = new HashMap<String, String>();
    private Map<String, String> koulutustausta = new HashMap<String, String>();
    private Map<String, String> osaaminen = new HashMap<String, String>();

    public Map<String, String> getHenkilotiedot() {
        return henkilotiedot;
    }

    public void setHenkilotiedot(Map<String, String> henkilotiedot) {
        this.henkilotiedot = henkilotiedot;
    }

    public Map<String, String> getLisatiedot() {
        return lisatiedot;
    }

    public void setLisatiedot(Map<String, String> lisatiedot) {
        this.lisatiedot = lisatiedot;
    }

    public Map<String, String> getHakutoiveet() {
        return hakutoiveet;
    }

    public void setHakutoiveet(Map<String, String> hakutoiveet) {
        this.hakutoiveet = hakutoiveet;
    }

    public Map<String, String> getKoulutustausta() {
        return koulutustausta;
    }

    public void setKoulutustausta(Map<String, String> koulutustausta) {
        this.koulutustausta = koulutustausta;
    }

    public Map<String, String> getOsaaminen() {
        return osaaminen;
    }

    public void setOsaaminen(Map<String, String> osaaminen) {
        this.osaaminen = osaaminen;
    }
}
