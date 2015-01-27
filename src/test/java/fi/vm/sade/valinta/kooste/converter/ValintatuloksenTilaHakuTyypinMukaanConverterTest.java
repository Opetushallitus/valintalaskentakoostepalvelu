package fi.vm.sade.valinta.kooste.converter;

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import static fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.*;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;

import static fi.vm.sade.valinta.kooste.converter.ValintatuloksenTilaHakuTyypinMukaanConverter.convertValintatuloksenTilaHakuTyypinMukaan;
import static fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi.*;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.function.Function;

import static org.hamcrest.CoreMatchers.*;

import static fi.vm.sade.valinta.kooste.converter.ValintatuloksenTilaHakuTyypinMukaanConverter.*;

/**
 * @author Jussi Jartamo
 */
public class ValintatuloksenTilaHakuTyypinMukaanConverterTest {

    @Test
    public void testaaValintatuloksenTilaKonversioToisenAsteenHaunTilojenTuonnille() {
        Function<ValintatuloksenTila,ValintatuloksenTila> toinenAsteConverter =
                v0 -> convertValintatuloksenTilaHakuTyypinMukaan(v0,TOISEN_ASTEEN_OPPILAITOS);

        assertThat(toinenAsteConverter.apply(ILMOITETTU),equalTo(ILMOITETTU));
        assertThat(toinenAsteConverter.apply(VASTAANOTTANUT),is(VASTAANOTTANUT));
        assertThat(toinenAsteConverter.apply(VASTAANOTTANUT_LASNA),equalTo(VASTAANOTTANUT));
        assertThat(toinenAsteConverter.apply(VASTAANOTTANUT_POISSAOLEVA),equalTo(VASTAANOTTANUT));
        assertThat(toinenAsteConverter.apply(EI_VASTAANOTETTU_MAARA_AIKANA),is(EI_VASTAANOTETTU_MAARA_AIKANA));
        assertThat(toinenAsteConverter.apply(PERUNUT),is(PERUNUT));
        assertThat(toinenAsteConverter.apply(PERUUTETTU),is(PERUNUT));
        assertThat(toinenAsteConverter.apply(EHDOLLISESTI_VASTAANOTTANUT),equalTo(VASTAANOTTANUT));
        assertThat(toinenAsteConverter.apply(VASTAANOTTANUT_SITOVASTI),is(VASTAANOTTANUT));
        assertThat(toinenAsteConverter.apply(KESKEN),is(KESKEN));
    }

    @Test
    public void testaaValintatuloksenTilaKonversioKorkeakouluHaunTilojenTuonnille() {
        Function<ValintatuloksenTila,ValintatuloksenTila> korkeakouluConverter =
                v0 -> convertValintatuloksenTilaHakuTyypinMukaan(v0,KORKEAKOULU);

        assertThat(korkeakouluConverter.apply(ILMOITETTU),equalTo(ILMOITETTU));
        assertThat(korkeakouluConverter.apply(VASTAANOTTANUT),is(VASTAANOTTANUT_SITOVASTI));
        assertThat(korkeakouluConverter.apply(VASTAANOTTANUT_LASNA),equalTo(VASTAANOTTANUT));
        assertThat(korkeakouluConverter.apply(VASTAANOTTANUT_POISSAOLEVA),equalTo(VASTAANOTTANUT));
        assertThat(korkeakouluConverter.apply(EI_VASTAANOTETTU_MAARA_AIKANA),is(EI_VASTAANOTETTU_MAARA_AIKANA));
        assertThat(korkeakouluConverter.apply(PERUNUT),is(PERUNUT));
        assertThat(korkeakouluConverter.apply(PERUUTETTU),is(PERUUTETTU));
        assertThat(korkeakouluConverter.apply(EHDOLLISESTI_VASTAANOTTANUT),equalTo(VASTAANOTTANUT));
        assertThat(korkeakouluConverter.apply(VASTAANOTTANUT_SITOVASTI),is(VASTAANOTTANUT_SITOVASTI));
        assertThat(korkeakouluConverter.apply(KESKEN),is(KESKEN));
    }
}
