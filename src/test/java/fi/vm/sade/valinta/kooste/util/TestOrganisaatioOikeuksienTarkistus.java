package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.valinta.kooste.OrganisaatioOikeuksienTarkistus;
import fi.vm.sade.valinta.kooste.exception.OrganisaatioException;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Created by jussija on 09/01/15.
 */
public class TestOrganisaatioOikeuksienTarkistus {

    @Test
    public void testaaOrganisaatioOikeuksienTarkistusOPHKayttajalle() {
        Assert.assertTrue(OrganisaatioOikeuksienTarkistus.tarkistaKayttooikeudet("1.2.246.562.10.00000000001",
                "1.2.246.562.10.00000000001/1.2.246.562.10.53814745062/1.2.246.562.10.39218317368"));
    }

    public void testaaOrganisaatioOikeuksienTarkistusOikeutetulleKayttajalle() {
        Assert.assertTrue(OrganisaatioOikeuksienTarkistus.tarkistaKayttooikeudet("1.2.246.562.10.39218317368",
                "1.2.246.562.10.00000000001/1.2.246.562.10.53814745062/1.2.246.562.10.39218317368"));
    }
    public void testaaOrganisaatioOikeuksienTarkistusHakukohteeseenKuulumattomalleKayttajalle() {
        Assert.assertFalse(OrganisaatioOikeuksienTarkistus.tarkistaKayttooikeudet("1.2.246.562.10.55555555555",
                "1.2.246.562.10.00000000001/1.2.246.562.10.53814745062/1.2.246.562.10.39218317368"));
    }

    public void testaaOrganisaatioOikeuksienKayttooikeuksettomalleKayttajalle() {
        Assert.assertFalse(OrganisaatioOikeuksienTarkistus.tarkistaKayttooikeudet(null,
                "1.2.246.562.10.00000000001/1.2.246.562.10.53814745062/1.2.246.562.10.39218317368"));
    }
}
