package fi.vm.sade.valinta.kooste.util;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Jussi Jartamo
 */
public class HenkilotunnuksenTarkistusUtilTest {

    @Test
    public void testaaValidiHenkilotunnus(){
        {
            String jokuvaliditunnus = "200195-949U";
            Assert.assertTrue("Pitäisi olla validi henkilötunnus", HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(jokuvaliditunnus));
        }

        {
            String jokuvaliditunnus = "190195-933N";
            Assert.assertTrue("Pitäisi olla validi henkilötunnus", HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(jokuvaliditunnus));
        }
    }

    @Test
    public void testaaEpavalidiHenkilotunnus(){
        String viallinentunnus = "200195-949B";
        Assert.assertFalse("Pitäisi olla epäkelpo henkilötunnus", HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(viallinentunnus));

        String liianlyhyt = "200195-949";
        Assert.assertFalse("Pitäisi olla epäkelpo henkilötunnus", HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(liianlyhyt));

        String nulli = null;
        Assert.assertFalse("Pitäisi olla epäkelpo henkilötunnus", HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(nulli));

        String kirjaimiasyntymaajassa = "20A195-949B";
        Assert.assertFalse("Pitäisi olla epäkelpo henkilötunnus", HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(kirjaimiasyntymaajassa));

        String kirjaimialoppuosassa = "200195-94QB";
        Assert.assertFalse("Pitäisi olla epäkelpo henkilötunnus", HenkilotunnusTarkistusUtil.tarkistaHenkilotunnus(kirjaimialoppuosassa));
    }
}
