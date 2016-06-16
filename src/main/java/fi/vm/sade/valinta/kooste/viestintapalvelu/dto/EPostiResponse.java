package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

public class EPostiResponse {

    private Long batchId;
    private Integer numberOfRecipients;

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Integer getNumberOfRecipients() {
        return numberOfRecipients;
    }

    public void setNumberOfRecipients(Integer numberOfRecipients) {
        this.numberOfRecipients = numberOfRecipients;
    }
}
