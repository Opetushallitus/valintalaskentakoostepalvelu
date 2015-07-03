package fi.vm.sade.valinta.kooste.util;

import static fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.*;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HARKINNANVARAISESTI_HYVAKSYTTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYLATTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYVAKSYTTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARASIJALTA_HYVAKSYTTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUNUT;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUUNTUNUT;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARALLA;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;

public class HakemusUtil {
    private static final Logger LOG = LoggerFactory.getLogger(HakemusUtil.class);
    private final static Map<String, Map<HakemuksenTila, String>> TILAT = valmistaTilat();
    private final static Map<String, String> VARASIJAT = varasijaTekstinTilat();
    private final static Map<String, Map<IlmoittautumisTila, String>> ILMOITTAUTUMISTILAT = ilmoittautumisTilat();
    private final static Map<String, Map<ValintatuloksenTila, String>> VALINTATULOKSEN_TILAT = valintatulostenTilat();

    private static Map<String, String> varasijaTekstinTilat() {
        Map<String, String> varasijaTekstinTilat = Maps.newHashMap();
        varasijaTekstinTilat.put(KieliUtil.SUOMI, "Varasijan numero on ");
        varasijaTekstinTilat.put(KieliUtil.RUOTSI, "Reservplatsens nummer är ");
        varasijaTekstinTilat.put(KieliUtil.ENGLANTI, "Waiting list number is ");
        return varasijaTekstinTilat;
    }

    private static Map<String, Map<ValintatuloksenTila, String>> valintatulostenTilat() {
        Map<String, Map<ValintatuloksenTila, String>> kielet = new HashMap<String, Map<ValintatuloksenTila, String>>();
        Map<ValintatuloksenTila, String> fi = new HashMap<ValintatuloksenTila, String>();
        fi.put(ILMOITETTU, "Ilmoitettu");
        fi.put(VASTAANOTTANUT, "Vastaanottanut");
        fi.put(VASTAANOTTANUT_LASNA, "Vastaanottanut läsnä");
        fi.put(VASTAANOTTANUT_POISSAOLEVA, "Vastaanottanut poissaolevana");
        fi.put(VASTAANOTTANUT_SITOVASTI, "Vastaanottanut sitovasti");
        fi.put(EI_VASTAANOTETTU_MAARA_AIKANA, "Ei vastaanotettu määräaikana");
        fi.put(KESKEN, "Kesken");
        fi.put(ValintatuloksenTila.PERUNUT, "Perunut");
        fi.put(PERUUTETTU, "Peruutettu");
        fi.put(EHDOLLISESTI_VASTAANOTTANUT, "Ehdollisesti vastaanottanut");

        Map<ValintatuloksenTila, String> sv = new HashMap<ValintatuloksenTila, String>();
        sv.put(ILMOITETTU, "Ilmoitettu");
        sv.put(KESKEN, "Kesken");
        sv.put(VASTAANOTTANUT, "Vastaanottanut");
        sv.put(VASTAANOTTANUT_LASNA, "Vastaanottanut läsnä");
        sv.put(VASTAANOTTANUT_POISSAOLEVA, "Vastaanottanut poissaolevana");
        sv.put(VASTAANOTTANUT_SITOVASTI, "Vastaanottanut sitovasti");
        sv.put(EI_VASTAANOTETTU_MAARA_AIKANA, "Ei vastaanotettu määräaikana");
        sv.put(ValintatuloksenTila.PERUNUT, "Perunut");
        sv.put(PERUUTETTU, "Peruutettu");
        sv.put(EHDOLLISESTI_VASTAANOTTANUT, "Ehdollisesti vastaanottanut");

        Map<ValintatuloksenTila, String> en = new HashMap<ValintatuloksenTila, String>();
        en.put(ILMOITETTU, "Ilmoitettu");
        en.put(KESKEN, "Kesken");
        en.put(VASTAANOTTANUT, "Vastaanottanut");
        en.put(VASTAANOTTANUT_LASNA, "Vastaanottanut läsnä");
        en.put(VASTAANOTTANUT_POISSAOLEVA, "Vastaanottanut poissaolevana");
        en.put(VASTAANOTTANUT_SITOVASTI, "Vastaanottanut sitovasti");
        en.put(EI_VASTAANOTETTU_MAARA_AIKANA, "Ei vastaanotettu määräaikana");
        en.put(ValintatuloksenTila.PERUNUT, "Perunut");
        en.put(PERUUTETTU, "Peruutettu");
        en.put(EHDOLLISESTI_VASTAANOTTANUT, "Ehdollisesti vastaanottanut");

        kielet.put(KieliUtil.SUOMI, Collections.unmodifiableMap(fi));
        kielet.put(KieliUtil.RUOTSI, Collections.unmodifiableMap(sv));
        kielet.put(KieliUtil.ENGLANTI, Collections.unmodifiableMap(en));
        return Collections.unmodifiableMap(kielet);
    }

    private static Map<String, Map<IlmoittautumisTila, String>> ilmoittautumisTilat() {
        Map<String, Map<IlmoittautumisTila, String>> kielet = new HashMap<String, Map<IlmoittautumisTila, String>>();

        Map<IlmoittautumisTila, String> fi = new HashMap<IlmoittautumisTila, String>();
        fi.put(IlmoittautumisTila.EI_TEHTY, "Ei tehty");
        fi.put(IlmoittautumisTila.LASNA_KOKO_LUKUVUOSI, "Läsnä (koko lukuvuosi)");
        fi.put(IlmoittautumisTila.POISSA_KOKO_LUKUVUOSI, "Poissa koko lukuvuosi");
        fi.put(IlmoittautumisTila.EI_ILMOITTAUTUNUT, "Ei ilmoittautunut");
        fi.put(IlmoittautumisTila.LASNA_SYKSY, "Läsnä syksy, poissa kevät");
        fi.put(IlmoittautumisTila.POISSA_SYKSY, "Poissa syksy, läsnä kevät");
        fi.put(IlmoittautumisTila.LASNA, "Läsnä keväällä alkava koulutus");
        fi.put(IlmoittautumisTila.POISSA, "Poissa keväällä alkava koulutus");

        Map<IlmoittautumisTila, String> sv = new HashMap<IlmoittautumisTila, String>();
        sv.put(IlmoittautumisTila.EI_TEHTY, "Ei tehty");
        sv.put(IlmoittautumisTila.LASNA_KOKO_LUKUVUOSI, "Läsnä (koko lukuvuosi)");
        sv.put(IlmoittautumisTila.POISSA_KOKO_LUKUVUOSI, "Poissa koko lukuvuosi");
        sv.put(IlmoittautumisTila.EI_ILMOITTAUTUNUT, "Ei ilmoittautunut");
        sv.put(IlmoittautumisTila.LASNA_SYKSY, "Läsnä syksy, poissa kevät");
        sv.put(IlmoittautumisTila.POISSA_SYKSY, "Poissa syksy, läsnä kevät");
        sv.put(IlmoittautumisTila.LASNA, "Läsnä keväällä alkava koulutus");
        sv.put(IlmoittautumisTila.POISSA, "Poissa keväällä alkava koulutus");

        Map<IlmoittautumisTila, String> en = new HashMap<IlmoittautumisTila, String>();
        en.put(IlmoittautumisTila.EI_TEHTY, "Ei tehty");
        en.put(IlmoittautumisTila.LASNA_KOKO_LUKUVUOSI, "Läsnä (koko lukuvuosi)");
        en.put(IlmoittautumisTila.POISSA_KOKO_LUKUVUOSI, "Poissa koko lukuvuosi");
        en.put(IlmoittautumisTila.EI_ILMOITTAUTUNUT, "Ei ilmoittautunut");
        en.put(IlmoittautumisTila.LASNA_SYKSY, "Läsnä syksy, poissa kevät");
        en.put(IlmoittautumisTila.POISSA_SYKSY, "Poissa syksy, läsnä kevät");
        en.put(IlmoittautumisTila.LASNA, "Läsnä keväällä alkava koulutus");
        en.put(IlmoittautumisTila.POISSA, "Poissa keväällä alkava koulutus");

        kielet.put(KieliUtil.SUOMI, Collections.unmodifiableMap(fi));
        kielet.put(KieliUtil.RUOTSI, Collections.unmodifiableMap(sv));
        kielet.put(KieliUtil.ENGLANTI, Collections.unmodifiableMap(en));
        return Collections.unmodifiableMap(kielet);
    }

    private static Map<String, Map<HakemuksenTila, String>> valmistaTilat() {
        Map<String, Map<HakemuksenTila, String>> kielet = new HashMap<String, Map<HakemuksenTila, String>>();
        Map<HakemuksenTila, String> fi = new HashMap<HakemuksenTila, String>();
        fi.put(HYLATTY, "Hylätty");
        fi.put(VARALLA, "Varalla");
        fi.put(VARASIJALTA_HYVAKSYTTY, "Varasijalta hyväksytty");
        fi.put(PERUUNTUNUT, "Peruuntunut");
        fi.put(HYVAKSYTTY, "Hyväksytty");
        fi.put(HARKINNANVARAISESTI_HYVAKSYTTY, "Harkinnanvaraisesti hyväksytty");
        fi.put(HakemuksenTila.PERUUTETTU, "Peruutettu");
        fi.put(PERUNUT, "Perunut");

        Map<HakemuksenTila, String> sv = new HashMap<HakemuksenTila, String>();
        sv.put(HYLATTY, "Underkänd");
        sv.put(VARALLA, "På reservplats");
        sv.put(VARASIJALTA_HYVAKSYTTY, "Godkänd från reservplats");
        sv.put(PERUUNTUNUT, "Annullerad");
        sv.put(HYVAKSYTTY, "Godkänd");
        sv.put(HARKINNANVARAISESTI_HYVAKSYTTY, "Godkänd enligt prövning");
        sv.put(HakemuksenTila.PERUUTETTU, "Annullerad");
        sv.put(PERUNUT, "Annullerad");

        Map<HakemuksenTila, String> en = new HashMap<HakemuksenTila, String>();
        en.put(HYLATTY, "Rejected");
        en.put(VARALLA, "On a waiting list");
        en.put(PERUUNTUNUT, "Cancelled");
        en.put(HYVAKSYTTY, "Accepted");
        en.put(VARASIJALTA_HYVAKSYTTY, "Accepted from a waiting list");
        en.put(HARKINNANVARAISESTI_HYVAKSYTTY, "Accepted");
        en.put(HakemuksenTila.PERUUTETTU, "Cancelled");
        en.put(PERUNUT, "Cancelled");

        kielet.put(KieliUtil.SUOMI, Collections.unmodifiableMap(fi));
        kielet.put(KieliUtil.RUOTSI, Collections.unmodifiableMap(sv));
        kielet.put(KieliUtil.ENGLANTI, Collections.unmodifiableMap(en));
        return Collections.unmodifiableMap(kielet);
    }

    public static String varasijanNumeroConverter(Integer numero, String preferoitukielikoodi) {
        return VARASIJAT.get(preferoitukielikoodi) + numero;
    }

    public static String tilaConverter(HakemuksenTila tila, String preferoitukielikoodi, boolean harkinnanvarainen) {
        return tilaConverter(tila, preferoitukielikoodi, harkinnanvarainen, false, null);
    }

    public static String tilaConverter(IlmoittautumisTila tila, String preferoitukielikoodi) {
        if (tila == null) {
            return StringUtils.EMPTY;
        }
        try {
            return ILMOITTAUTUMISTILAT.get(preferoitukielikoodi).get(tila);
        } catch (Exception e) {
            LOG.error("Hakemuksen tila utiliteetilla ei ole konversiota ilmoittautumistilalle: {}", tila);
            return tila.toString();
        }
    }

    public static String lupaJulkaisuun(boolean lupa) {
        if (lupa) {
            return "Lupa julkaisuun";
        }
        return StringUtils.EMPTY;
    }

    public static String tilaConverter(ValintatuloksenTila tila, String preferoitukielikoodi) {
        if (tila == null) {
            return StringUtils.EMPTY;
        }
        try {
            return VALINTATULOKSEN_TILAT.get(preferoitukielikoodi).get(tila);
        } catch (Exception e) {
            LOG.error("Hakemuksen tila utiliteetilla ei ole konversiota ilmoittautumistilalle: {}", tila);
            return tila.toString();
        }
    }

    public static String tilaConverter(HakemuksenTila tila, String preferoitukielikoodi, boolean harkinnanvarainen, boolean lisaaVarasijanNumero, Integer varasijanNumero) {
        if (tila == null) {
            return StringUtils.EMPTY;
        }
        HakemuksenTila lopullinenTila = tila;
        if (HYVAKSYTTY.equals(tila) && harkinnanvarainen) {
            lopullinenTila = HARKINNANVARAISESTI_HYVAKSYTTY;
        }
        try {
            if (lisaaVarasijanNumero && VARALLA.equals(lopullinenTila) && varasijanNumero != null) {
                return new StringBuilder().append(TILAT.get(preferoitukielikoodi).get(lopullinenTila)).append(" (")
                        .append(varasijanNumero).append(")").toString();
            } else {
                return TILAT.get(preferoitukielikoodi).get(lopullinenTila);
            }
        } catch (Exception e) {
            LOG.error("Hakemuksen tila utiliteetilla ei ole konversiota hakemustilalle: {}", tila);
            return tila.toString();
        }
    }
}
