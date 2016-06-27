package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

public class Pisteet {
    private String oma;
    private String minimi;
    private String ensikertMinimi;

    public Pisteet() {

    }

    public Pisteet(String oma, String minimi, String ensikertMinimi) {
        this.oma = oma;
        this.minimi = minimi;
        this.ensikertMinimi = ensikertMinimi;
    }

    public String getEnsikertMinimi() {
        return ensikertMinimi;
    }

    public String getMinimi() {
        return minimi;
    }

    public String getOma() {
        return oma;
    }

    @Override
    public String toString() {
        return "Pisteet{" +
                "oma='" + oma + '\'' +
                ", minimi='" + minimi + '\'' +
                ", ensikertalaisen minimipisteet='" + ensikertMinimi + '\'' +
                '}';
    }
}
