package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

import java.util.Optional;

public class Sijoitus {
    private String nimi;
    private String oma;
    private String varasija;
    private Pisteet pisteet;

    public Sijoitus() {

    }

    public Sijoitus(String nimi, Integer om, String varasija, Pisteet pisteet) {
        this.nimi = nimi;
        if (om == null) {
            this.oma = "-";
        } else {
            this.oma = om.toString();
        }
        this.varasija = varasija;
        this.pisteet = pisteet;
    }

    public Pisteet getPisteet() {
        return pisteet;
    }

    public String getVarasija() {
        return varasija;
    }

    public String getNimi() {
        return nimi;
    }

    public String getOma() {
        return oma;
    }

    @Override
    public String toString() {
        return "Sijoitus{" +
                "nimi='" + nimi + '\'' +
                ", oma='" + oma + '\'' +
                ", varasija='" + varasija + '\'' +
                '}';
    }
}
