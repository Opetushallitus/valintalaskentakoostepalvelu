package fi.vm.sade.valinta.kooste.external.resource.ataru.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AtaruHakemus {

    private String hakemusOid;
    private String personOid;
    private String hakuOid;
    private List<String> hakutoiveet;
    private Map<String,String> keyValues;

    public AtaruHakemus() {}

    public AtaruHakemus(String hakemusOid, String personOid, String hakuOid, List<String> hakutoiveet, Map<String, String> keyValues) {
        this.hakemusOid = hakemusOid;
        this.personOid = personOid;
        this.hakuOid = hakuOid;
        this.hakutoiveet = hakutoiveet;
        this.keyValues = keyValues;
    }

    public String getHakemusOid() {
        return hakemusOid;
    }

    public void setHakemusOid(String hakemusOid) {
        this.hakemusOid = hakemusOid;
    }

    public String getPersonOid() {
        return personOid;
    }

    public void setPersonOid(String personOid) {
        this.personOid = personOid;
    }

    public String getHakuOid() {
        return hakuOid;
    }

    public void setHakuOid(String hakuOid) {
        this.hakuOid = hakuOid;
    }

    public List<String> getHakutoiveet() {
        return hakutoiveet;
    }

    public void setHakutoiveet(List<String> hakutoiveet) {
        this.hakutoiveet = hakutoiveet;
    }

    public Map<String, String> getKeyValues() {
        return keyValues;
    }

    public void setKeyValues(Map<String, String> keyValues) {
        this.keyValues = keyValues;
    }

    public boolean hasHetu() {
        return StringUtils.isNotEmpty(keyValues.get("ssn"));
    }
}
