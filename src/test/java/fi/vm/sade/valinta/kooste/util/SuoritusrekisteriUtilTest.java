package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuaikaV1RDTO;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class SuoritusrekisteriUtilTest {

    @Test
    public void testEnsikertalaisuudenRajapvmFound() {
        final Date now = new Date();
        final HakuV1RDTO haku = new HakuV1RDTO();
        final HakuaikaV1RDTO hakuaika = new HakuaikaV1RDTO();
        hakuaika.setLoppuPvm(now);
        haku.getHakuaikas().add(hakuaika);

        assertEquals(new SimpleDateFormat(SuoritusrekisteriUtil.ENSIKERTALAISUUS_RAJAPVM_FORMAT).format(now), SuoritusrekisteriUtil.getEnsikertalaisuudenRajapvm(haku));
    }

    @Test
    public void testEnsikertalaisuudenRajapvmNotFoundForNullHaku() {
        assertEquals(null, SuoritusrekisteriUtil.getEnsikertalaisuudenRajapvm(null));
    }

    @Test
    public void testEnsikertalaisuudenRajapvmNotFoundForNullHakuaika() {
        assertEquals(null, SuoritusrekisteriUtil.getEnsikertalaisuudenRajapvm(new HakuV1RDTO()));
    }

    @Test
    public void testEnsikertalaisuudenRajapvmNotFoundWhenAnyHakuaikaHavingNullLoppupvm() {
        final Date now = new Date();
        final HakuV1RDTO haku = new HakuV1RDTO();
        final HakuaikaV1RDTO hakuaika = new HakuaikaV1RDTO();
        hakuaika.setLoppuPvm(now);
        haku.getHakuaikas().add(hakuaika);
        final HakuaikaV1RDTO hakuaika2 = new HakuaikaV1RDTO();
        haku.getHakuaikas().add(hakuaika2);

        assertEquals(null, SuoritusrekisteriUtil.getEnsikertalaisuudenRajapvm(new HakuV1RDTO()));
    }

    @Test
    public void testEnsikertalaisuudenRajapvmReturnLatestHakuaikaLoppuPvm() {
        final Date now = new Date();
        final HakuV1RDTO haku = new HakuV1RDTO();
        final HakuaikaV1RDTO hakuaika = new HakuaikaV1RDTO();
        hakuaika.setLoppuPvm(now);
        haku.getHakuaikas().add(hakuaika);
        final HakuaikaV1RDTO hakuaika2 = new HakuaikaV1RDTO();
        final Date later = new Date(now.getTime() + 1000);
        hakuaika2.setLoppuPvm(later);
        haku.getHakuaikas().add(hakuaika2);

        assertEquals(new SimpleDateFormat(SuoritusrekisteriUtil.ENSIKERTALAISUUS_RAJAPVM_FORMAT).format(later), SuoritusrekisteriUtil.getEnsikertalaisuudenRajapvm(haku));
    }
}
