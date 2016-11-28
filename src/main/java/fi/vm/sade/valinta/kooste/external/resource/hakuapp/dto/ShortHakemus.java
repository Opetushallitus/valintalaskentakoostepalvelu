package fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;

public class ShortHakemus {
    private final String oid;
    private final String state;
    private final long received;
    private final String firstNames;
    private final String lastName;
    private final String ssn;
    private final String personOid;

    @JsonCreator
    public ShortHakemus(String oid,
                        String state,
                        long received,
                        String firstNames,
                        String lastName,
                        String ssn,
                        String personOid) {
        this.oid = oid;
        this.state = state;
        this.received = received;
        this.firstNames = firstNames;
        this.lastName = lastName;
        this.ssn = ssn;
        this.personOid = personOid;
    }

    public String getOid() {
        return oid;
    }

    public String getState() {
        return state;
    }

    public LocalDateTime getReceived() {
        return Instant.ofEpochMilli(received).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public String getFirstNames() {
        return firstNames;
    }

    public String getLastName() {
        return lastName;
    }

    public String getSsn() {
        return ssn;
    }

    public String getPersonOid() {
        return personOid;
    }
}
