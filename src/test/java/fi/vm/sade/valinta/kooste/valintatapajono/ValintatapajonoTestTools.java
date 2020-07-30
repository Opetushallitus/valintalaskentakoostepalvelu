package fi.vm.sade.valinta.kooste.valintatapajono;

import static org.junit.Assert.assertEquals;

import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRivi;

public abstract class ValintatapajonoTestTools {

  public void assertRivi(ValintatapajonoRivi expected, ValintatapajonoRivi rivi) {
    assertEquals(expected.getJonosija(), rivi.getJonosija());
    assertEquals(expected.getNimi(), rivi.getNimi());
    assertEquals(expected.getOid(), rivi.getOid());
    assertEquals(expected.getTila(), rivi.getTila());
    assertEquals(expected.getKuvaus(), rivi.getKuvaus());
  }
}
