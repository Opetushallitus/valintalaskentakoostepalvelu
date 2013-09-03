package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;


/**
 * 
 * @author Jussi Jartamo
 * 
 *         Koska Viestintapalvelulla ei ole API:a
 * 
 */
public class Osoite {

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

    public Osoite(String firstName, String lastName, String addressline, String addressline2, String addressline3,
            String postalCode, String city, String region, String country, String countryCode) {
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

}
