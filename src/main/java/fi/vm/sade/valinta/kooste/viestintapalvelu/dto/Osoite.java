package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Osoite implements Serializable {
    private String firstName; // "Etunimi",
    private String lastName; // ":"Sukunimi",
    private String addressline; // ":"Osoiterivi1",
    private String addressline2; // ":"Osoiterivi2",
    private String addressline3; // :"Osoiterivi3",
    private String postalCode; // ":"00500",
    private String city; // ":"Helsinki",
    private String region;// ":"Kallio",
    private String country; // ":"Suomi",
    private String countryCode; // ":"FI"
    private String organisaationimi;
    private String email;
    private String numero;
    @JsonIgnore
    private Boolean ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt;

    public Osoite(
            String firstName,
            String lastName,
            String addressline,
            String addressline2,
            String addressline3,
            String postalCode,
            String city,
            String region,
            String country,
            String countryCode,
            Boolean ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.addressline = addressline;
        this.addressline2 = addressline2;
        this.addressline3 = addressline3;
        this.postalCode = postalCode;
        this.city = city;
        this.region = region;
        this.country = country;
        this.countryCode = countryCode;
        this.ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt = ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt;
        this.organisaationimi = null;
        this.email = null;
        this.numero = null;
    }

    public Osoite(
            String firstName,
            String lastName,
            String addressline,
            String addressline2,
            String addressline3,
            String postalCode,
            String city,
            String region,
            String country,
            String countryCode,
            String organisaationimi,
            String numero,
            String email,
            Boolean ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt) {
        this.numero = numero;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.addressline = addressline;
        this.addressline2 = addressline2;
        this.addressline3 = addressline3;
        this.postalCode = postalCode;
        this.city = city;
        this.region = region;
        this.country = country;
        this.countryCode = countryCode;
        this.ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt = ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt;
        this.organisaationimi = organisaationimi;
    }

    public String getOrganisaationimi() {
        return organisaationimi;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    @JsonIgnore
    public boolean isUlkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt() {
        return ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt;
    }

    public String getAddressline() {
        return addressline;
    }

    public String getAddressline2() {
        return addressline2;
    }

    public String getAddressline3() {
        return addressline3;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getRegion() {
        return region;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(addressline);
        if (addressline2 != null) {
            b.append(" ").append(addressline2);
        }
        if (addressline3 != null) {
            b.append(" ").append(addressline3);
        }
        b.append(", ").append(postalCode).append(" ").append(city);
        return b.toString();
    }

    private static final long serialVersionUID = -1734664942239974134L;
}
