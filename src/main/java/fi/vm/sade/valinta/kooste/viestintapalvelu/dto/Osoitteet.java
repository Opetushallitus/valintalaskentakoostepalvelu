package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Koska Viestintapalvelulla ei ole API:a
 * 
 */

public class Osoitteet implements Serializable {

    private List<Osoite> addressLabels;

    public Osoitteet(List<Osoite> addressLabels) {
        this.addressLabels = addressLabels;
    }

    public List<Osoite> getAddressLabels() {
        return addressLabels;
    }

    private static final long serialVersionUID = -7777955308444290462L;

}
