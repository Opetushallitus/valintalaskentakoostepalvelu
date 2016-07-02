package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

import java.util.Optional;

public class Sijoitus {
    private String nimi;
    private String oma;
    private String hyvaksytyt;
    private Pisteet pisteet;

    public Sijoitus() {

    }

    public Sijoitus(String nimi, Integer om, Integer hyvaksyty, Pisteet pisteet) {
        this.nimi = nimi;
        if (om == null) {
            this.oma = "-";
        } else {
            this.oma = om.toString();
        }
        if (hyvaksyty == null) {
            this.hyvaksytyt = "-";
        } else {
            this.hyvaksytyt = hyvaksyty.toString();
        }
        this.pisteet = pisteet;
    }

    public Pisteet getPisteet() {
        return pisteet;
    }

    public String getHyvaksytyt() {
        return hyvaksytyt;
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
                ", hyvaksytyt='" + hyvaksytyt + '\'' +
                '}';
    }
}
