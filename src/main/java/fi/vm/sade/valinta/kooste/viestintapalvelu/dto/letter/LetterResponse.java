package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

import java.util.Map;

public class LetterResponse {

    public static final String STATUS_ERROR = "error";
    public static final String STATUS_SUCCESS = "success";

    private String batchId;
    private String status;
    private Map<String, String> errors;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }

}
