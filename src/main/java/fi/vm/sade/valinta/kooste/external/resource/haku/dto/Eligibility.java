package fi.vm.sade.valinta.kooste.external.resource.haku.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Eligibility {
	private String aoId;
	private String status;
	private String source;

	public Eligibility() {
		this.aoId = null;
		this.status = null;
		this.source = null;
	}

	public Eligibility(String aoId, String status, String source) {
		this.aoId = aoId;
		this.status = status;
		this.source = source;
	}

	public String getAoId() {
		return aoId;
	}

	public String getSource() {
		return source;
	}

	public String getStatus() {
		return status;
	}

	public String getParsedEligibilityStatus() {
		return status.equals("AUTOMATICALLY_CHECKED_ELIGIBLE") ? "ELIGIBLE" : status;
	}
}
