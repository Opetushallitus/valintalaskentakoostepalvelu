package fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto;

public class Recipient {
    private String email;
    private String securelink;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSecurelink() {
        return securelink;
    }

    public void setSecurelink(String securelink) {
        this.securelink = securelink;
    }

    @Override
    public String toString() {
        return "Recipient{" +
                "email='" + email + '\'' +
                ", securelink='" + securelink + '\'' +
                '}';
    }
}
