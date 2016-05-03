package fi.vm.sade.valinta.kooste.erillishaku.util;

import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import static fi.vm.sade.valinta.kooste.erillishaku.util.ValidoiTilatUtil.*;

/**
 * @author Jussi Jartamo
 */
public class ValidoiTilatUtilTest {

    @Test
    public void testaaIlmoittautumistietoVoiOllaAinoastaanHyväksytyilläJaVastaanottaneillaHakijoilla() {
        // OK SYOTTEET
        assertThat(validoi(HakemuksenTila.HYVAKSYTTY, ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA, IlmoittautumisTila.LASNA),is(nullValue()));
        assertThat(validoi(HakemuksenTila.HYVAKSYTTY, ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI, IlmoittautumisTila.POISSA),is(nullValue()));
        assertThat(validoi(HakemuksenTila.HYVAKSYTTY, ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT, IlmoittautumisTila.POISSA_KOKO_LUKUVUOSI),is(nullValue()));
        assertThat(validoi(HakemuksenTila.HYVAKSYTTY, ValintatuloksenTila.PERUNUT, null),is(nullValue()));
        assertThat(validoi(HakemuksenTila.HYVAKSYTTY, ValintatuloksenTila.PERUUTETTU, null),is(nullValue()));
        assertThat(validoi(HakemuksenTila.HYVAKSYTTY, ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA, null),is(nullValue()));
        assertThat(validoi(HakemuksenTila.HYVAKSYTTY, ValintatuloksenTila.KESKEN, null),is(nullValue()));

        // NOK SYOTTEET
        assertThat(validoi(HakemuksenTila.VARALLA, null, IlmoittautumisTila.LASNA),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.VARALLA, null, IlmoittautumisTila.POISSA),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.VARALLA, null, IlmoittautumisTila.LASNA_SYKSY),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.VARALLA, null, IlmoittautumisTila.LASNA_KOKO_LUKUVUOSI), is(notNullValue()));
        assertThat(validoi(HakemuksenTila.VARALLA, null, IlmoittautumisTila.POISSA_KOKO_LUKUVUOSI),is(notNullValue()));

        assertThat(validoi(HakemuksenTila.HYLATTY, null, IlmoittautumisTila.LASNA),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.HYLATTY, null, IlmoittautumisTila.POISSA),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.HYLATTY, null, IlmoittautumisTila.LASNA_SYKSY),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.HYLATTY, null, IlmoittautumisTila.LASNA_KOKO_LUKUVUOSI), is(notNullValue()));
        assertThat(validoi(HakemuksenTila.HYLATTY, null, IlmoittautumisTila.POISSA_KOKO_LUKUVUOSI),is(notNullValue()));
    }

    @Test
    public void testaaVastaanottaneenTaiPeruneenHakijanTulisiOllaHyväksyttynä() {
        assertThat(validoi(HakemuksenTila.HYLATTY, ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA, null),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.HYLATTY, ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI, null),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.HYLATTY, ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT, null),is(notNullValue()));

        assertThat(validoi(HakemuksenTila.VARALLA, ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA, null),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.VARALLA, ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI, null),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.VARALLA, ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT, null),is(notNullValue()));

        assertThat(validoi(HakemuksenTila.PERUUTETTU, ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA, null),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.PERUUTETTU, ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI, null),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.PERUUTETTU, ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT, null),is(notNullValue()));

        assertThat(validoi(HakemuksenTila.PERUNUT, ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA, null),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.PERUNUT, ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI, null),is(notNullValue()));
        assertThat(validoi(HakemuksenTila.PERUNUT, ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT, null),is(notNullValue()));
    }
}
