package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

/**
 *         Paluuviesti prosessiId:n välittämiseen käyttöliittymään
 */
public class ProsessiId {
    private final String id;

    private ProsessiId() {
        this("");
    }

    public ProsessiId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
