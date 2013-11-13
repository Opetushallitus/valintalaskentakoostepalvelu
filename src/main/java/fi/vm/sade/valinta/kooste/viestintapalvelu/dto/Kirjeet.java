package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         DTO hyvaksymis- ja jalkiohjauskirjeille
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
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
