package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class TuontiErrorDTO {
    private final String applicationOID;
    private final String applicantName;
    private final String errorMessage;

    public TuontiErrorDTO(String applicationOID, String applicantName, String errorMessage) {
        this.applicationOID = applicationOID;
        this.applicantName = applicantName;
        this.errorMessage = errorMessage;

    }

    public String getApplicantName() {
        return applicantName;
    }

    public String getApplicationOID() {
        return applicationOID;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
