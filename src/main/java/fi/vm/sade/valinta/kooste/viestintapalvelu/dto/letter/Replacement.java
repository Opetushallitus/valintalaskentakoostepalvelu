package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

import java.util.Date;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Replacement {
	private long id;
	private String name = null;
	private String defaultValue = null;
	private Date timestamp;
	private boolean mandatory = false;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Replacement [name=" + name + ", defaultValue=" + defaultValue
				+ ", mandatory=" + mandatory + ", timestamp=" + timestamp
				+ ", id=" + id + "]";
	}
}
