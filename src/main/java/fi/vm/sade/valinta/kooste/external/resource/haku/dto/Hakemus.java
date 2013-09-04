package fi.vm.sade.valinta.kooste.external.resource.haku.dto;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * User: wuoti
 * Date: 3.9.2013
 * Time: 14.42
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hakemus {
    private String type;
    private String applicationSystemId;
    private Answers answers;

    private String oid;
    private String state;
    private String personOid;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getApplicationSystemId() {
        return applicationSystemId;
    }

    public void setApplicationSystemId(String applicationSystemId) {
        this.applicationSystemId = applicationSystemId;
    }

    public Answers getAnswers() {
        return answers;
    }

    public void setAnswers(Answers answers) {
        this.answers = answers;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPersonOid() {
        return personOid;
    }

    public void setPersonOid(String personOid) {
        this.personOid = personOid;
    }
}
