package fi.vm.sade.valinta.kooste.erillishaku.util;

import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import junit.framework.Assert;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import static fi.vm.sade.valinta.kooste.erillishaku.util.ValidoiTilatUtil.*;

/**
 * @author Jussi Jartamo
 */
public class ValidoiTilatUtilTest {
    @Test
    public void testaaKäytöstäpoistettujenTilojenHavaitseminen() {
        // Ilmoitettu on poistettu käytöstä
        assertThat(validoi(HakemuksenTila.HYVAKSYTTY, ValintatuloksenTila.ILMOITETTU, IlmoittautumisTila.LASNA_SYKSY),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.HYLATTY, ValintatuloksenTila.ILMOITETTU, IlmoittautumisTila.EI_ILMOITTAUTUNUT),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.HYLATTY, ValintatuloksenTila.ILMOITETTU, IlmoittautumisTila.POISSA),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.PERUNUT, ValintatuloksenTila.ILMOITETTU, IlmoittautumisTila.EI_ILMOITTAUTUNUT),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.HYLATTY, ValintatuloksenTila.ILMOITETTU, IlmoittautumisTila.EI_TEHTY),is(notNullValue()));
    }
    @Test
    public void testaaHylätylläTaiVarallaOlevallaHakijallaOnVastaanottoonViittaaviaTietoja() {
        assertThat(validoi(HakemuksenTila.HYLATTY, ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT, IlmoittautumisTila.EI_ILMOITTAUTUNUT),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.HYLATTY, ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA, IlmoittautumisTila.EI_ILMOITTAUTUNUT),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.HYLATTY, ValintatuloksenTila.KESKEN, IlmoittautumisTila.EI_ILMOITTAUTUNUT),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.HYLATTY, ValintatuloksenTila.PERUNUT, IlmoittautumisTila.EI_ILMOITTAUTUNUT),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.HYLATTY, ValintatuloksenTila.PERUUTETTU, IlmoittautumisTila.EI_ILMOITTAUTUNUT),is(notNullValue()));
    }
    @Test
    public void testaaHyväksyttyäVastaavatTilat() {
        assertNull(validoi(HakemuksenTila.HYVAKSYTTY, ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA, IlmoittautumisTila.EI_ILMOITTAUTUNUT));
        assertThat(validoi(HakemuksenTila.HYVAKSYTTY, ValintatuloksenTila.PERUNUT, IlmoittautumisTila.EI_ILMOITTAUTUNUT), is(nullValue()));
    }

    @Test
    public void testaaEttäJulkaistullaVoiOlla() {
        assertNull(validoi(HakemuksenTila.HYVAKSYTTY, ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA, IlmoittautumisTila.EI_ILMOITTAUTUNUT));
        assertThat(validoi(HakemuksenTila.HYVAKSYTTY, ValintatuloksenTila.PERUNUT, IlmoittautumisTila.EI_ILMOITTAUTUNUT), is(nullValue()));
    }
}
