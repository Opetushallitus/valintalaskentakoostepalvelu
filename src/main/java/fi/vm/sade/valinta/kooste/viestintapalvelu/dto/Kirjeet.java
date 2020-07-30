package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.ArrayList;
import java.util.List;

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
