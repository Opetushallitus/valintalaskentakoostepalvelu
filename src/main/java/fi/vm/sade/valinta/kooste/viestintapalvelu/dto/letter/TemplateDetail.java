package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         id: 599, name: "otsikko", defaultValue: "HYVÃ„ HAKIJA,", timestamp:
 *         1400245641962, mandatory: false
 */
public class TemplateDetail {
	private long id;
	private String name;
	private String defaultValue;
	private String timestamp;
	private boolean mandatory;

	public void setName(String name) {
		this.name = name;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public boolean isMandatory() {
		return mandatory;
	}
}
