package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Kirje {
    public Kirje(Osoite addressLabel, String languageCode,
                 List<Map<String, String>> tulokset) {
        this.addressLabel = addressLabel;
        this.languageCode = languageCode;
        this.tulokset = tulokset;
        this.koulu = null;
        this.koulutus = null;
    }

    public Kirje(Osoite addressLabel, String languageCode, String koulu,
                 String koulutus, List<Map<String, String>> tulokset) {
        this.addressLabel = addressLabel;
        this.languageCode = languageCode;
        this.tulokset = tulokset;
        this.koulu = koulu;
        this.koulutus = koulutus;
    }

    private Osoite addressLabel;
    private String languageCode;
    private String koulu;
    private String koulutus;
    private List<Map<String, String>> tulokset;

    public Osoite getAddressLabel() {
        return addressLabel;
    }

    public String getKoulu() {
        return koulu;
    }

    public String getKoulutus() {
        return koulutus;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public List<Map<String, String>> getTulokset() {
        return tulokset;
    }

}
