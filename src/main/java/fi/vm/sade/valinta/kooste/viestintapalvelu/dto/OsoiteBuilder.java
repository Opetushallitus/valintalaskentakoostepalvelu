package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

public class OsoiteBuilder {
    private String firstName;
    private String lastName;
    private String addressline;
    private String addressline2;
    private String addressline3;
    private String postalCode;
    private String city;
    private String region;
    private String country;
    private String countryCode;
    private Boolean ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt;
    private String organisaationimi;
    private String numero;
    private String email;
    private String www;

    public OsoiteBuilder setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public OsoiteBuilder setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public OsoiteBuilder setAddressline(String addressline) {
        this.addressline = addressline;
        return this;
    }

    public OsoiteBuilder setAddressline2(String addressline2) {
        this.addressline2 = addressline2;
        return this;
    }

    public OsoiteBuilder setAddressline3(String addressline3) {
        this.addressline3 = addressline3;
        return this;
    }

    public OsoiteBuilder setPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public OsoiteBuilder setCity(String city) {
        this.city = city;
        return this;
    }

    public OsoiteBuilder setRegion(String region) {
        this.region = region;
        return this;
    }

    public OsoiteBuilder setCountry(String country) {
        this.country = country;
        return this;
    }

    public OsoiteBuilder setCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public OsoiteBuilder setUlkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt(Boolean ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt) {
        this.ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt = ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt;
        return this;
    }

    public OsoiteBuilder setOrganisaationimi(String organisaationimi) {
        this.organisaationimi = organisaationimi;
        return this;
    }

    public OsoiteBuilder setNumero(String numero) {
        this.numero = numero;
        return this;
    }

    public OsoiteBuilder setEmail(String email) {
        this.email = email;
        return this;
    }

    public OsoiteBuilder setWww(String www) {
        this.www = www;
        return this;
    }

    public Osoite createOsoite() {
        return new Osoite(firstName, lastName, addressline, addressline2, addressline3, postalCode, city, region, country, countryCode, ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt, organisaationimi, numero, email, www);
    }
}