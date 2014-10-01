package fi.vm.sade.valinta.kooste.external.resource.haku.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * User: wuoti Date: 3.9.2013 Time: 14.42
 * 
 * TODO: Refaktoroi pois haku-paketin alta. Tämä dto ei liity haku-palveluun
 * vaan hakemus-palveluun.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hakemus {
	private String type;
	private String applicationSystemId;
	private Answers answers;
	private Map<String, String> additionalInfo = new HashMap<String, String>();
	private List<Eligibility> preferenceEligibilities;
	private String oid;
	private String state;
	private String personOid;

	public String getType() {
		return type;
	}

	public List<Eligibility> getPreferenceEligibilities() {
		return preferenceEligibilities;
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

	public Map<String, String> getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(Map<String, String> additionalInfo) {
		this.additionalInfo = additionalInfo;
	}
}
