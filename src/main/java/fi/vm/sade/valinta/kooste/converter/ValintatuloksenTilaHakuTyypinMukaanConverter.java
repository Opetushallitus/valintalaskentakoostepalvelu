package fi.vm.sade.valinta.kooste.converter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import static fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.*;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;

import java.util.Map;

/**
 * @author Jussi Jartamo
 */
public class ValintatuloksenTilaHakuTyypinMukaanConverter {

    private static final Map<Hakutyyppi,Map<ValintatuloksenTila, ValintatuloksenTila>> tyyppiTilaMappaus = createTyyppiTilaMappaus();

    private static Map<Hakutyyppi,Map<ValintatuloksenTila, ValintatuloksenTila>> createTyyppiTilaMappaus() {
        // Varmistaa etta toiselle asteelle ei vieda korkeakoulun tyyppeja
        Map<ValintatuloksenTila, ValintatuloksenTila> korkeakouluToToinenAsteMappaus = Maps.newEnumMap(ValintatuloksenTila.class);
        korkeakouluToToinenAsteMappaus.put(EI_VASTAANOTETTU_MAARA_AIKANA,EI_VASTAANOTETTU_MAARA_AIKANA);
        korkeakouluToToinenAsteMappaus.put(PERUNUT,PERUNUT);
        korkeakouluToToinenAsteMappaus.put(PERUUTETTU,PERUNUT);
        korkeakouluToToinenAsteMappaus.put(KESKEN,KESKEN);
        korkeakouluToToinenAsteMappaus.put(VASTAANOTTANUT_SITOVASTI,VASTAANOTTANUT);
        korkeakouluToToinenAsteMappaus.put(VASTAANOTTANUT,VASTAANOTTANUT);
        korkeakouluToToinenAsteMappaus.put(ILMOITETTU,null);
        korkeakouluToToinenAsteMappaus.put(VASTAANOTTANUT_LASNA,null);
        korkeakouluToToinenAsteMappaus.put(VASTAANOTTANUT_POISSAOLEVA,null);
        korkeakouluToToinenAsteMappaus.put(EHDOLLISESTI_VASTAANOTTANUT,null);

        // Varmistaa etta korkeakoululle ei vieda toisen asteen tyyppeja
        Map<ValintatuloksenTila, ValintatuloksenTila> toinenAsteToKorkeakouluMappaus = Maps.newEnumMap(ValintatuloksenTila.class);
        toinenAsteToKorkeakouluMappaus.put(EI_VASTAANOTETTU_MAARA_AIKANA,EI_VASTAANOTETTU_MAARA_AIKANA);
        toinenAsteToKorkeakouluMappaus.put(PERUNUT,PERUNUT);
        toinenAsteToKorkeakouluMappaus.put(PERUUTETTU,PERUUTETTU);
        toinenAsteToKorkeakouluMappaus.put(KESKEN,KESKEN);
        toinenAsteToKorkeakouluMappaus.put(VASTAANOTTANUT_SITOVASTI,VASTAANOTTANUT_SITOVASTI);
        toinenAsteToKorkeakouluMappaus.put(VASTAANOTTANUT,VASTAANOTTANUT_SITOVASTI);
        toinenAsteToKorkeakouluMappaus.put(ILMOITETTU,null);
        toinenAsteToKorkeakouluMappaus.put(VASTAANOTTANUT_LASNA,null);
        toinenAsteToKorkeakouluMappaus.put(VASTAANOTTANUT_POISSAOLEVA,null);
        toinenAsteToKorkeakouluMappaus.put(EHDOLLISESTI_VASTAANOTTANUT,null);

        Map<Hakutyyppi,Map<ValintatuloksenTila, ValintatuloksenTila>> m = Maps.newEnumMap(Hakutyyppi.class);
        m.put(Hakutyyppi.KORKEAKOULU, toinenAsteToKorkeakouluMappaus);
        m.put(Hakutyyppi.TOISEN_ASTEEN_OPPILAITOS, korkeakouluToToinenAsteMappaus);
        return m;
    }

    public static ValintatuloksenTila convertValintatuloksenTilaHakuTyypinMukaan(ValintatuloksenTila tila, Hakutyyppi hakutyyppi) {
        return tyyppiTilaMappaus.get(hakutyyppi).get(tila);
    }
}
