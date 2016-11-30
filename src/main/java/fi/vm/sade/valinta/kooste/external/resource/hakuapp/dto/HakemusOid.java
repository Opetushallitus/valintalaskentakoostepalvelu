package fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;

public class HakemusOid {
    private final String oid;

    @JsonCreator
    public HakemusOid(String oid) {
        this.oid = oid;
    }

    public String getOid() {
        return oid;
    }
}
