package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import java.util.Collection;
import java.util.Map;

public interface KirjeProsessi {
  void vaiheValmistui();

  boolean isValmis();

  void valmistui(String dokumenttiId);

  void keskeyta();

  void keskeyta(Poikkeus syy);

  void keskeyta(Collection<Poikkeus> syyt);

  void keskeyta(String syy);

  void keskeyta(String syy, Map<String, String> virheet);

  boolean isKeskeytetty();
}
