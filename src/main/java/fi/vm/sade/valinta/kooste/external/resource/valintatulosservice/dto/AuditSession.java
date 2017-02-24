package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto;

import java.util.List;
import java.util.Optional;

public class AuditSession {
    private String personOid;
    private List<String> roles;
    private String userAgent;
    private String inetAddress;
    private Optional<String> ifUnmodifiedSince;

    public AuditSession() {}

    public AuditSession(String personOid, List<String> roles, String userAgent, String inetAddress) {
        this.personOid = personOid;
        this.roles = roles;
        this.userAgent = userAgent;
        this.inetAddress = inetAddress;
        this.ifUnmodifiedSince = Optional.empty();
    }

    public String getPersonOid() {
        return personOid;
    }

    public void setPersonOid(String personOid) {
        this.personOid = personOid;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getInetAddress() {
        return inetAddress;
    }

    public void setInetAddress(String inetAddress) {
        this.inetAddress = inetAddress;
    }

    public Optional<String> getIfUnmodifiedSince() {
        return ifUnmodifiedSince;
    }

    public void setIfUnmodifiedSince(Optional<String> ifUnmodifiedSince) {
        this.ifUnmodifiedSince = ifUnmodifiedSince;
    }

    @Override
    public String toString() {
        return "AuditSession{" +
                "personOid='" + personOid + '\'' +
                ", roles=" + roles +
                ", userAgent='" + userAgent + '\'' +
                ", inetAddress='" + inetAddress + '\'' +
                ", ifUnmodifiedSince=" + ifUnmodifiedSince +
                '}';
    }
}
