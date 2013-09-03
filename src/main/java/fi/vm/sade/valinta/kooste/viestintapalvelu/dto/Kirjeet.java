package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         DTO hyvaksymis- ja jalkiohjauskirjeille
 */
public class Kirjeet {

    private List<Kirje> letters;

    public Kirjeet() {
        this.letters = new ArrayList<Kirje>();
    }

    public Kirjeet(List<Kirje> kirjeet) {
        this.letters = kirjeet;
    }

    public List<Kirje> getLetters() {
        return letters;
    }
}
