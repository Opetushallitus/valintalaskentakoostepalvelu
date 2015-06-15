package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

public class Maakoodi {
    private final String postitoimipaikka;
    private final String maa;

    public Maakoodi(String postitoimipaikka, String maa) {
        this.maa = maa;
        this.postitoimipaikka = postitoimipaikka;
    }

    public String getMaa() {
        return maa;
    }

    public String getPostitoimipaikka() {
        return postitoimipaikka;
    }
}
