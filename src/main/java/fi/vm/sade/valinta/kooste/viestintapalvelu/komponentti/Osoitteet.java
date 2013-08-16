package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Koska Viestintapalvelulla ei ole API:a
 * 
 */
public class Osoitteet {

    private List<Osoite> addressLabels;

    public Osoitteet(List<Osoite> addressLabels) {
        this.addressLabels = addressLabels;
    }

    public List<Osoite> getAddressLabels() {
        return addressLabels;
    }
}
