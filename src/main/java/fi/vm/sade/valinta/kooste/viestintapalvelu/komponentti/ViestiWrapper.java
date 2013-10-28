package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import com.google.gson.GsonBuilder;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Wrapper olio debuggaukseen. Serialisoi viestin ainoastaan jos debug
 *         tulostetaan.
 */
public class ViestiWrapper {

    private final Object viesti;

    public ViestiWrapper(Object viesti) {
        this.viesti = viesti;
    }

    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(viesti);
    }
}
