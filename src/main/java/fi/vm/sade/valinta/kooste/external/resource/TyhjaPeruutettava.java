package fi.vm.sade.valinta.kooste.external.resource;

public class TyhjaPeruutettava implements Peruutettava {
    private final static TyhjaPeruutettava INSTANSSI = new TyhjaPeruutettava();

    private TyhjaPeruutettava() {
    }

    public boolean onTehty() {
        return true;
    }

    public void peruuta() {

    }

    public boolean equals(Object obj) {
        return INSTANSSI == obj;
    }

    public static Peruutettava tyhjaPeruutettava() {
        return INSTANSSI;
    }
}
