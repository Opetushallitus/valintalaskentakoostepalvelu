package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

public class LetterBatchStatusDto {

	private Integer sent;
	private Integer total;
	private Integer emailsProcessed;
	private String status;

	public Integer getSent() {
		return sent;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setSent(Number sent) {
		this.sent = (sent != null) ? sent.intValue() : null;
	}

	public Integer getTotal() {
		return total;
	}

	public void setTotal(Number total) {
		this.total = (total != null) ? total.intValue() : null;
	}

	public Integer getEmailsProcessed() {
		return emailsProcessed;
	}

	public void setEmailsProcessed(Number emailsProcessed) {
		this.emailsProcessed = emailsProcessed != null ? emailsProcessed
				.intValue() : null;
	}

	public boolean isEmailReviewable() {
		return this.emailsProcessed != null && this.emailsProcessed > 0;
	}
}
