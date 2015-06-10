package fi.vm.sade.valinta.kooste.external.resource.haku.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
public class ApplicationAdditionalDataDTO implements Serializable {

    private String oid;
    private String personOid;
    private String firstNames;
    private String lastName;
    private Map<String, String> additionalData = new HashMap<String, String>();

    public ApplicationAdditionalDataDTO() {
    }

    @JsonCreator
    public ApplicationAdditionalDataDTO(
            @JsonProperty(value = "oid") String oid,
            @JsonProperty(value = "personOid") String personOid,
            @JsonProperty(value = "firstNames") String firstNames,
            @JsonProperty(value = "lastName") String lastName,
            @JsonProperty(value = "additionalData") Map<String, String> additionalData) {
        this.oid = oid;
        this.personOid = personOid;
        this.firstNames = firstNames;
        this.lastName = lastName;
        this.additionalData = additionalData;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getPersonOid() {
        return personOid;
    }

    public void setPersonOid(String personOid) {
        this.personOid = personOid;
    }

    public String getFirstNames() {
        return firstNames;
    }

    public void setFirstNames(String firstNames) {
        this.firstNames = firstNames;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Map<String, String> getAdditionalData() {
        if (additionalData == null) {
            return new HashMap<>();
        }
        return additionalData;
    }

    public void setAdditionalData(Map<String, String> additionalData) {
        this.additionalData = additionalData;
    }

    /**
     *
     */
    private static final long serialVersionUID = 3450726545263839586L;
}
