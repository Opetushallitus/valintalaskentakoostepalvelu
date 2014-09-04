package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         DTO hyvaksymis- ja jalkiohjauskirjeille
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Kirjeet<T> {

	private List<T> letters;

	public Kirjeet() {
		this.letters = new ArrayList<T>();
	}

	public Kirjeet(List<T> kirjeet) {
		this.letters = kirjeet;
	}

	public List<T> getLetters() {
		return letters;
	}
}
