package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import java.util.Objects;

public class TuontiErrorDTO {

    private final String applicationOID;
    private final String applicantName;

    public TuontiErrorDTO(String applicationOID, String applicantName) {
        this.applicationOID = applicationOID;
        this.applicantName = applicantName;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public String getApplicationOID() {
        return applicationOID;
    }

    @Override
    public String toString() {
        return "TuontiErrorDTO{" +
                "applicationOID='" + applicationOID + '\'' +
                ", applicantName='" + applicantName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TuontiErrorDTO that = (TuontiErrorDTO) o;
        return Objects.equals(applicationOID, that.applicationOID) &&
                Objects.equals(applicantName, that.applicantName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationOID, applicantName);
    }
}