package fi.vm.sade.valinta.kooste.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** @author Jussi Jartamo */
public class HenkilotunnuksenTarkistusUtilTest {

  @Test
  public void testaaValidiHenkilotunnus() {
    {
      String jokuvaliditunnus = "200195-949U";
      Assertions.assertTrue(
          HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(jokuvaliditunnus),
          "Pitäisi olla validi henkilötunnus");
    }

    {
      String jokuvaliditunnus = "190195-933N";
      Assertions.assertTrue(
          HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(jokuvaliditunnus),
          "Pitäisi olla validi henkilötunnus");
    }

    {
      String jokuvaliditunnusValimerkilla = "190195A933N";
      Assertions.assertTrue(
          HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(jokuvaliditunnusValimerkilla),
          "Pitäisi olla validi henkilötunnus");
    }

    {
      String jokuvaliditunnusUudellaValimerkilla = "190195Y933N";
      Assertions.assertTrue(
          HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(jokuvaliditunnusUudellaValimerkilla),
          "Pitäisi olla validi henkilötunnus");
    }
  }

  @Test
  public void testaaEpavalidiHenkilotunnus() {
    String viallinentunnus = "200195-949B";
    Assertions.assertFalse(
        HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(viallinentunnus),
        "Pitäisi olla epäkelpo henkilötunnus");

    String liianlyhyt = "200195-949";
    Assertions.assertFalse(
        HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(liianlyhyt),
        "Pitäisi olla epäkelpo henkilötunnus");

    String nulli = null;
    Assertions.assertFalse(
        HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(nulli),
        "Pitäisi olla epäkelpo henkilötunnus");

    String kirjaimiasyntymaajassa = "20A195-949B";
    Assertions.assertFalse(
        HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(kirjaimiasyntymaajassa),
        "Pitäisi olla epäkelpo henkilötunnus");

    String kirjaimialoppuosassa = "200195-94QB";
    Assertions.assertFalse(
        HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(kirjaimialoppuosassa),
        "Pitäisi olla epäkelpo henkilötunnus");

    String outoValimerkki = "190195J933N";
    Assertions.assertFalse(
        HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(outoValimerkki),
        "Pitäisi olla epäkelpo henkilötunnus");

    String tyhjaValimerkki = "190294 933N";
    Assertions.assertFalse(
        HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(outoValimerkki),
        "Pitäisi olla epäkelpo henkilötunnus");
  }
}
