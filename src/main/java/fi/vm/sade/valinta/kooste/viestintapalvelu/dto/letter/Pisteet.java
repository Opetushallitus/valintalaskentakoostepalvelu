package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

public class Pisteet {
    private String nimi;
    private String oma;
    private String minimi;

    public Pisteet() {

    }

    public Pisteet(String nimi, String oma, String minimi) {
        this.nimi = nimi;
        this.oma = oma;
        this.minimi = minimi;
    }

    public String getMinimi() {
        return minimi;
    }

    public String getNimi() {
        return nimi;
    }

    public String getOma() {
        return oma;
    }

    @Override
    public String toString() {
        return "Pisteet{" +
                "nimi='" + nimi + '\'' +
                ", oma='" + oma + '\'' +
                ", minimi='" + minimi + '\'' +
                '}';
    }
}
