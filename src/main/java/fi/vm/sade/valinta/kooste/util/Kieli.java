package fi.vm.sade.valinta.kooste.util;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;

/** Yhteinen utiliteetti tarjonnasta tulevan kielikoodien transformointiin ja preferointiin. */
public class Kieli {

  private final String kieli;

  public Kieli(String kielikoodi) {
    this.kieli = KieliUtil.normalisoiKielikoodi(kielikoodi);
  }

  public Kieli(Collection<String> kielikoodit) {
    Set<String> kielet = Sets.newHashSet();
    for (String kielikoodi : kielikoodit) {
      kielet.add(KieliUtil.normalisoiKielikoodi(kielikoodi));
    }
    this.kieli = KieliUtil.preferoi(kielet);
  }

  public String getKieli() {
    return this.kieli;
  }
}
