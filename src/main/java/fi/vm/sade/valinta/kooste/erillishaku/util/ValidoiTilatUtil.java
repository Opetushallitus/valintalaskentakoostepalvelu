package fi.vm.sade.valinta.kooste.erillishaku.util;

import com.google.common.collect.Sets;
import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.*;

public class ValidoiTilatUtil {
    private static final Set<HakemuksenTila> HYVAKSYTTYNA =
            Sets.newEnumSet(Arrays.asList(HakemuksenTila.HYVAKSYTTY,HakemuksenTila.VARASIJALTA_HYVAKSYTTY),HakemuksenTila.class);

    private static final Set<HakemuksenTila> HYLATTY_TAI_VARALLA =
            Sets.newEnumSet(Arrays.asList(HakemuksenTila.HYLATTY,HakemuksenTila.VARALLA),HakemuksenTila.class);

    private static final Set<HakemuksenTila> PERUNEENA =
            Sets.newEnumSet(Arrays.asList(HakemuksenTila.PERUNUT,HakemuksenTila.PERUUTETTU,HakemuksenTila.PERUUNTUNUT),HakemuksenTila.class);

    private static final Set<ValintatuloksenTila> VASTAANOTTANEENA =
            Sets.newEnumSet(Arrays.asList(EI_VASTAANOTETTU_MAARA_AIKANA,
                    EHDOLLISESTI_VASTAANOTTANUT,VASTAANOTTANUT_SITOVASTI),ValintatuloksenTila.class);

    private static final Set<ValintatuloksenTila> VASTAANOTTANEENA_TAI_PERUNEENA =
            Sets.newEnumSet(Arrays.asList(EI_VASTAANOTETTU_MAARA_AIKANA,
                    EHDOLLISESTI_VASTAANOTTANUT,VASTAANOTTANUT_SITOVASTI, PERUNUT, PERUUTETTU),ValintatuloksenTila.class);

    private static final Set<ValintatuloksenTila> VASTAANOTTANEENA_TAI_PERUNEENA_EI_MYOHASTYNYT =
            Sets.newEnumSet(Arrays.asList(EHDOLLISESTI_VASTAANOTTANUT,VASTAANOTTANUT_SITOVASTI, PERUNUT, PERUUTETTU),ValintatuloksenTila.class);

    private static final Set<ValintatuloksenTila> KESKEN_TAI_PERUNUT_VASTAANOTTAJA =
            Sets.newHashSet(Arrays.asList(KESKEN, PERUUTETTU, PERUNUT, OTTANUT_VASTAAN_TOISEN_PAIKAN, EI_VASTAANOTETTU_MAARA_AIKANA));
    
    private static final Set<IlmoittautumisTila> EI_ILMOITTAUTUMISTA =
            Sets.newHashSet(Arrays.asList(IlmoittautumisTila.EI_TEHTY));
    /**
     *
     * @return (null if ok) validation error
     */
    public static String validoi(HakemuksenTila hakemuksenTila, ValintatuloksenTila valintatuloksenTila, IlmoittautumisTila ilmoittautumisTila) {
        if (hakemuksenTila != null && (valintatuloksenTila == null && ilmoittautumisTila == null)) {
            return null; // OK
        }
        if (HYVAKSYTTYNA.contains(hakemuksenTila) &&
            (KESKEN_TAI_PERUNUT_VASTAANOTTAJA.contains(valintatuloksenTila) || ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA.equals(valintatuloksenTila)) &&
            ilmoittautumisTila == null) {
                return null; // OK
        }
        if (hakemuksenTila == null || valintatuloksenTila == null || ilmoittautumisTila == null) {
            return virheellinenTilaYhdistelma(new StringBuilder("Tila ei saa olla tyhjä. "), hakemuksenTila, valintatuloksenTila, ilmoittautumisTila).toString();
        }
        if (!EI_ILMOITTAUTUMISTA.contains(ilmoittautumisTila)) {
            if (HYVAKSYTTYNA.contains(hakemuksenTila) && VASTAANOTTANEENA_TAI_PERUNEENA.contains(valintatuloksenTila)) {
                return null; // OK
            } else {
                return virheellinenTilaYhdistelma(new StringBuilder("Ilmoittautumistieto voi olla ainoastaan hyväksytyillä ja vastaanottaneilla hakijoilla. "), hakemuksenTila, valintatuloksenTila, ilmoittautumisTila).toString();
            }
        }

        if (VASTAANOTTANEENA_TAI_PERUNEENA_EI_MYOHASTYNYT.contains(valintatuloksenTila)) {
            if (HYVAKSYTTYNA.contains(hakemuksenTila)) {
                return null; // OK
            } else {
                return virheellinenTilaYhdistelma(new StringBuilder("Vastaanottaneen tai peruneen hakijan tulisi olla hyväksyttynä. "), hakemuksenTila, valintatuloksenTila, ilmoittautumisTila).toString();
            }
        }

        if (HYLATTY_TAI_VARALLA.contains(hakemuksenTila)) {
            if (!VASTAANOTTANEENA.contains(valintatuloksenTila) && EI_ILMOITTAUTUMISTA.contains(ilmoittautumisTila)) {
                return null; // OK
            } else {
                return virheellinenTilaYhdistelma(new StringBuilder("Hylätty tai varalla oleva hakija ei voi olla ilmoittautunut tai vastaanottanut. "), hakemuksenTila, valintatuloksenTila, ilmoittautumisTila).toString();
            }
        }

        if (PERUNEENA.contains(hakemuksenTila)) {
            if (KESKEN_TAI_PERUNUT_VASTAANOTTAJA.contains(valintatuloksenTila)) {
                return null; // OK
            } else {
                return virheellinenTilaYhdistelma(new StringBuilder("Peruneella vastaanottajalla ei voi olla vastaanottotilaa. "), hakemuksenTila, valintatuloksenTila, ilmoittautumisTila).toString();
            }
        }


        /* VTTILA
                EI_VASTAANOTETTU_MAARA_AIKANA, // Hakija ei ole ilmoittanut paikkaa vastaanotetuksi maaraaikana ja on nain ollen hylatty
                PERUNUT,                       // Hakija ei ota paikkaa vastaan
                PERUUTETTU,                    // Hakijan tila on peruutettu
                EHDOLLISESTI_VASTAANOTTANUT,    // Ehdollisesti vastaanottanut
                VASTAANOTTANUT_SITOVASTI,       // Sitovasti vastaanottanut
                KESKEN
                */
        /* ILMOTILAT
                EI_TEHTY,                           // Ei tehty
                LASNA_KOKO_LUKUVUOSI,               // Läsnä (koko lukuvuosi)
                POISSA_KOKO_LUKUVUOSI,              // Poissa (koko lukuvuosi)
                EI_ILMOITTAUTUNUT,                  // Ei ilmoittautunut
                LASNA_SYKSY,                        // Läsnä syksy, poissa kevät
                POISSA_SYKSY,                       // Poissa syksy, läsnä kevät
                LASNA,                              // Läsnä, keväällä alkava koulutus
                POISSA;                              // Poissa, keväällä alkava koulutus
        */
        /* HAKEMUKSEN TILA
                HYLATTY, // hakija ei voi koskaan tulla valituksi kohteeseen
                VARALLA, // Hakija voi tulla kohteeseen valituksi (jossain vaiheessa)
                PERUUNTUNUT, // Hakija on tullut valituksi parempaan paikkaan (korkeampi hakutoive)
                VARASIJALTA_HYVAKSYTTY, //Hakija voi ottaa paikan vastaan (alunperin varasijalla)
                HYVAKSYTTY, //Hakija voi ottaa paikan vastaan
                PERUNUT, //Hakija ei ole vastaanottanut paikkaa. Hakija ei voi tulla enää valituksi matalamman prioriteetin kohteissa
                PERUUTETTU; // Virkailija on perunut paikan. Sama toiminnallisuuks kuil HYLATTY
        */
        return null;
    }

    private static StringBuilder virheellinenTilaYhdistelma(StringBuilder sb, HakemuksenTila hakemuksenTila, ValintatuloksenTila valintatuloksenTila, IlmoittautumisTila ilmoittautumisTila) {
        sb.append(toString(hakemuksenTila)).append(", ").append(toString(valintatuloksenTila)).append(" ja ").append(toString(ilmoittautumisTila)).append(" on virheellinen tilayhdistelmä");
        return sb;
    }

    private static String toString(HakemuksenTila hakemuksenTila) {
        if(hakemuksenTila == null) {
            return "TYHJÄ";
        }
        return hakemuksenTila.toString();
    }

    private static String toString(ValintatuloksenTila valintatuloksenTila) {
        if(valintatuloksenTila == null) {
            return "TYHJÄ";
        }
        return valintatuloksenTila.toString();
    }

    private static String toString(IlmoittautumisTila ilmoittautumisTila) {
        if(ilmoittautumisTila == null) {
            return "TYHJÄ";
        }
        return ilmoittautumisTila.toString();
    }
}
