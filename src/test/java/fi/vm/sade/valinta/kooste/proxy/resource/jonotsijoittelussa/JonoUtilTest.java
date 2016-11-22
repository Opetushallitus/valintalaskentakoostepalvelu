package fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa;

import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto.Jono;
import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto.JonoPair;
import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.util.JonoUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.*;

public class JonoUtilTest {
    @Test
    public void testPuutteellisetTyhjalle() {
        List<JonoPair> jonoPairs = JonoUtil.pairJonos(emptyList(), emptyList());
        Assert.assertTrue(JonoUtil.puutteellisetHakukohteet(jonoPairs).isEmpty());
    }

    @Test
    public void testMolemmissaSamatJaKaikkiOk() {
        Jono j1 = new Jono("hk1", "j1", Optional.of(true),true);
        List<Jono> laskenta = Arrays.asList(j1);
        List<Jono> perusteet = Arrays.asList(j1);
        List<JonoPair> jonoPairs = JonoUtil.pairJonos(laskenta, perusteet);
        Assert.assertTrue(JonoUtil.puutteellisetHakukohteet(jonoPairs).isEmpty());
    }
}
